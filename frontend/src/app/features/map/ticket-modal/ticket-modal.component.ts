import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, Subscription, debounceTime, distinctUntilChanged, filter, map, merge, switchMap } from 'rxjs';
import { TicketService, URGENZA_DEFAULT } from '../../../core/services/ticket.service';
import { Categoria, TicketRequest, TicketResponse, ZonaResponse } from '../../../shared/models/ticket.models';
import { URGENZA_DESCRIZIONE } from '../../../shared/utils/ticket-display.util';
import { ZONE_MAP_DEFS } from '../zone-map.data';

/** Pausa di digitazione dopo la quale si richiede la stima dell'urgenza. */
const STIMA_URGENZA_DEBOUNCE_MS = 1000;
/** Lunghezza minima della descrizione accettata dal microservizio NLP. */
const STIMA_URGENZA_MIN_CARATTERI = 3;

interface CategoriaOption {
  value: Categoria;
  label: string;
  icona: string;
}

const PIANI = ['Piano Terra', '1° Piano', '2° Piano', '3° Piano', '4° Piano', 'Esterno/Area comune'];

const CATEGORIE: CategoriaOption[] = [
  {
    value: Categoria.ELETTRICO,
    label: 'Elettrico',
    icona: 'M13 2 3 14h7l-1 8 10-12h-7l1-8Z',
  },
  {
    value: Categoria.WIFI,
    label: 'WiFi',
    icona: 'M5 12.5a11 11 0 0 1 14 0M8.5 16a6.5 6.5 0 0 1 7 0M12 19.5h.01',
  },
  {
    value: Categoria.IDRAULICO,
    label: 'Idraulico',
    icona: 'M12 2s6 7 6 11.5a6 6 0 1 1-12 0C6 9 12 2 12 2Z',
  },
  {
    value: Categoria.ATTREZZATURA,
    label: 'Attrezzatura',
    icona: 'm14.7 6.3 3 3-8.4 8.4a2.1 2.1 0 0 1-3-3l8.4-8.4Zm2.6-2.6 2 2M4 20l2.5-1',
  },
  {
    value: Categoria.ALTRO,
    label: 'Altro',
    icona: 'M9.1 9a3 3 0 0 1 5.8 1c0 2-3 2-3 4M12 17h.01M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z',
  },
];

/**
 * Modal di creazione segnalazione, aperto dalla mappa (con zona preimpostata) o dalla sidebar ("+ Nuova").
 */
@Component({
  selector: 'app-ticket-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ticket-modal.component.html',
})
export class TicketModalComponent implements OnInit, OnChanges, OnDestroy {
  @Input() zona: ZonaResponse | null = null;
  @Input() zoneOptions: ZonaResponse[] = [];
  @Input() cuboPreimpostato = '';
  @Output() chiudi = new EventEmitter<void>();
  @Output() ticketCreato = new EventEmitter<TicketResponse>();

  readonly piani = PIANI;
  readonly categorie = CATEGORIE;
  readonly livelliUrgenza = [1, 2, 3, 4, 5];
  readonly URGENZA_DESCRIZIONE = URGENZA_DESCRIZIONE;

  /** Urgenza mostrata nel form (stimata dal sistema o scelta dall'utente). */
  urgenzaSelezionata = URGENZA_DEFAULT;
  /** Ultima stima ricevuta dal microservizio NLP, null finché non arriva. */
  stimaUrgenza: { urgenza: number; confidenza: number } | null = null;
  /** True se l'utente ha cliccato un livello: le stime successive non sovrascrivono la scelta. */
  urgenzaModificataManualmente = false;
  stimaInCorso = false;

  private readonly descrizioneBlur$ = new Subject<void>();
  private stimaSubscription?: Subscription;

  inviando = false;
  errore: string | null = null;

  form = this.fb.group({
    zonaId: this.fb.control<string | null>(null, Validators.required),
    cubo: this.fb.control<string>(''),
    piano: this.fb.control<string>(''),
    titolo: this.fb.control<string>('', [Validators.required, Validators.maxLength(200)]),
    descrizione: this.fb.control<string>('', [Validators.required, Validators.maxLength(2000)]),
    categoria: this.fb.control<Categoria | null>(null, Validators.required),
  });

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
  ) {}

  /**
   * Avvia la stima automatica dell'urgenza: dopo 1s di pausa nella digitazione
   * o al blur della descrizione, chiama il microservizio NLP.
   */
  ngOnInit(): void {
    const descrizione = this.form.controls.descrizione;
    const dopoPausa$ = descrizione.valueChanges.pipe(debounceTime(STIMA_URGENZA_DEBOUNCE_MS));
    const alBlur$ = this.descrizioneBlur$.pipe(map(() => descrizione.value));

    this.stimaSubscription = merge(dopoPausa$, alBlur$)
      .pipe(
        map((testo) => (testo ?? '').trim()),
        filter((testo) => testo.length >= STIMA_URGENZA_MIN_CARATTERI),
        distinctUntilChanged(),
        switchMap((testo) => {
          this.stimaInCorso = true;
          return this.ticketService.stimaUrgenza(testo);
        }),
      )
      .subscribe((stima) => {
        this.stimaInCorso = false;
        this.stimaUrgenza = stima;
        if (!this.urgenzaModificataManualmente) {
          this.urgenzaSelezionata = stima.urgenza;
        }
      });
  }

  ngOnDestroy(): void {
    this.stimaSubscription?.unsubscribe();
  }

  ngOnChanges(): void {
    this.form.patchValue({ zonaId: this.zona?.id ?? null });
    if (this.cuboPreimpostato) {
      this.form.patchValue({ cubo: this.cuboPreimpostato });
    }
  }

  get cubiDisponibili(): string[] {
    if (!this.zona) return [];
    const zonaDef = ZONE_MAP_DEFS.find((z) => z.nome === this.zona?.nome);
    return zonaDef?.cubi ?? [];
  }

  selezionaCategoria(categoria: Categoria): void {
    this.form.patchValue({ categoria });
  }

  /**
   * Imposta manualmente l'urgenza: da questo momento la stima NLP non la sovrascrive più.
   */
  setUrgenza(livello: number): void {
    this.urgenzaSelezionata = livello;
    this.urgenzaModificataManualmente = true;
  }

  /**
   * Urgenza da includere nella richiesta: quella scelta dall'utente o stimata dall'NLP.
   * Se la stima non è ancora arrivata (o è il fallback per NLP non raggiungibile)
   * restituisce undefined, così il backend esegue la propria stima al salvataggio.
   */
  private urgenzaDaInviare(): number | undefined {
    const stimaValida = this.stimaUrgenza !== null && this.stimaUrgenza.confidenza > 0;
    return this.urgenzaModificataManualmente || stimaValida ? this.urgenzaSelezionata : undefined;
  }

  onDescrizioneBlur(): void {
    this.descrizioneBlur$.next();
  }

  onClose(): void {
    this.chiudi.emit();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valori = this.form.getRawValue();
    const request: TicketRequest = {
      zonaId: valori.zonaId as string,
      cubo: valori.cubo || undefined,
      piano: valori.piano || undefined,
      titolo: valori.titolo as string,
      descrizione: valori.descrizione as string,
      categoria: valori.categoria as Categoria,
      urgenza: this.urgenzaDaInviare(),
    };

    this.inviando = true;
    this.errore = null;
    this.ticketService.createTicket(request).subscribe({
      next: (ticket) => {
        this.inviando = false;
        this.ticketCreato.emit(ticket);
      },
      error: () => {
        this.inviando = false;
        this.errore = 'Impossibile inviare la segnalazione. Riprova più tardi.';
      },
    });
  }
}
