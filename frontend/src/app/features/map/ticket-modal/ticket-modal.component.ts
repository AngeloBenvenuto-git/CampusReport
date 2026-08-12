import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TicketService } from '../../../core/services/ticket.service';
import { Categoria, TicketRequest, TicketResponse, ZonaResponse } from '../../../shared/models/ticket.models';

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
export class TicketModalComponent implements OnChanges {
  @Input() zona: ZonaResponse | null = null;
  @Input() zoneOptions: ZonaResponse[] = [];
  @Output() chiudi = new EventEmitter<void>();
  @Output() ticketCreato = new EventEmitter<TicketResponse>();

  readonly piani = PIANI;
  readonly categorie = CATEGORIE;

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

  ngOnChanges(): void {
    this.form.patchValue({ zonaId: this.zona?.id ?? null });
  }

  selezionaCategoria(categoria: Categoria): void {
    this.form.patchValue({ categoria });
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
