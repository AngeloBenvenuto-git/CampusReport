import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { TicketService } from '../../../core/services/ticket.service';
import { AssegnaModalComponent } from '../../../shared/components/assegna-modal/assegna-modal.component';
import { TecnicoAdminResponse } from '../../../shared/models/admin.models';
import { Categoria, Stato, TicketResponse, ZonaResponse } from '../../../shared/models/ticket.models';
import {
  CATEGORIA_LABEL,
  dataRelativa,
  STATO_COLOR,
  STATO_LABEL,
  URGENZA_DESCRIZIONE,
} from '../../../shared/utils/ticket-display.util';
import { ZONE_MAP_DEFS } from '../../map/zone-map.data';

const PIANI = ['Piano Terra', '1° Piano', '2° Piano', '3° Piano', '4° Piano', 'Esterno/Area comune'];

@Component({
  selector: 'app-admin-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AssegnaModalComponent],
  templateUrl: './admin-tickets.component.html',
})
export class AdminTicketsComponent implements OnInit {
  readonly stati = Object.values(Stato);
  readonly categorie = Object.values(Categoria);
  readonly STATO_LABEL = STATO_LABEL;
  readonly STATO_COLOR = STATO_COLOR;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly dataRelativa = dataRelativa;
  readonly URGENZA_DESCRIZIONE = URGENZA_DESCRIZIONE;
  readonly livelliUrgenza = [1, 2, 3, 4, 5];
  readonly piani = PIANI;

  caricamento = true;
  errore: string | null = null;

  tickets: TicketResponse[] = [];
  tecnici: TecnicoAdminResponse[] = [];
  zone: ZonaResponse[] = [];
  messaggioSuccesso: string | null = null;

  // ─── Filtri ─────────────────────────────────────────────────────────────

  statoFiltro = '';
  categoriaFiltro = '';
  tecnicoFiltro = '';
  ricercaFiltro = '';

  esportando = false;

  // ─── Modal assegnazione manuale ─────────────────────────────────────────

  ticketDaAssegnare: TicketResponse | null = null;

  // ─── Modal modifica ticket ──────────────────────────────────────────────

  ticketInModifica: TicketResponse | null = null;
  salvandoModifica = false;
  erroreModifica: string | null = null;
  urgenzaModifica: number | null = null;

  formModifica = this.fb.group({
    titolo: ['', [Validators.required, Validators.maxLength(200)]],
    descrizione: ['', [Validators.required, Validators.maxLength(2000)]],
    categoria: this.fb.control<Categoria | null>(null, Validators.required),
    zonaId: ['', Validators.required],
    cubo: [''],
    piano: [''],
  });

  // ─── Modal eliminazione ticket ──────────────────────────────────────────

  ticketDaEliminare: TicketResponse | null = null;
  eliminando = false;
  erroreElimina: string | null = null;

  constructor(
    private adminService: AdminService,
    private ticketService: TicketService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.caricaDati();
    this.ticketService.getZone().subscribe({
      next: (zone) => (this.zone = zone),
      error: () => (this.errore = 'Impossibile caricare le zone.'),
    });
  }

  private caricaDati(): void {
    this.caricamento = true;
    this.errore = null;

    forkJoin({
      tickets: this.adminService.getAllTickets(this.filtriCorrenti()),
      tecnici: this.adminService.getTecnici(),
    }).subscribe({
      next: ({ tickets, tecnici }) => {
        this.tickets = tickets;
        this.tecnici = tecnici;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare i ticket.';
      },
    });
  }

  private filtriCorrenti() {
    return {
      stato: this.statoFiltro || undefined,
      categoria: this.categoriaFiltro || undefined,
      tecnicoId: this.tecnicoFiltro || undefined,
      ricerca: this.ricercaFiltro.trim() || undefined,
    };
  }

  applicaFiltri(): void {
    this.caricamento = true;
    this.adminService.getAllTickets(this.filtriCorrenti()).subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare i ticket.';
      },
    });
  }

  resetFiltri(): void {
    this.statoFiltro = '';
    this.categoriaFiltro = '';
    this.tecnicoFiltro = '';
    this.ricercaFiltro = '';
    this.applicaFiltri();
  }

  nomeTecnico(ticket: TicketResponse): string | null {
    return ticket.tecnico ? `${ticket.tecnico.nome} ${ticket.tecnico.cognome}` : null;
  }

  // ─── Export CSV ─────────────────────────────────────────────────────────

  esportaCsv(): void {
    this.esportando = true;
    this.adminService.exportCsv().subscribe({
      next: (blob) => {
        this.esportando = false;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'tickets.xlsx';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.esportando = false;
        this.errore = 'Impossibile esportare il CSV.';
      },
    });
  }

  // ─── Modal assegnazione manuale ─────────────────────────────────────────

  apriAssegna(ticket: TicketResponse): void {
    this.ticketDaAssegnare = ticket;
  }

  chiudiAssegna(): void {
    this.ticketDaAssegnare = null;
  }

  /**
   * Sostituisce nella lista locale il ticket appena assegnato dal modal.
   */
  onTicketAssegnato(aggiornato: TicketResponse): void {
    const idx = this.tickets.findIndex((t) => t.id === aggiornato.id);
    if (idx !== -1) this.tickets[idx] = aggiornato;
    this.ticketDaAssegnare = null;
  }

  /**
   * Restituisce i cubi della zona selezionata nel form di modifica, presi da ZONE_MAP_DEFS.
   * Se la zona non ha cubi definiti il template mostra un campo di testo libero.
   */
  get cubiDisponibili(): string[] {
    const zonaId = this.formModifica.controls.zonaId.value;
    const nomeZona = this.zone.find((z) => z.id === zonaId)?.nome;
    return ZONE_MAP_DEFS.find((z) => z.nome === nomeZona)?.cubi ?? [];
  }

  // ─── Modal modifica ticket ──────────────────────────────────────────────

  /**
   * Apre il modal di modifica precompilato con i valori attuali del ticket.
   */
  apriModifica(ticket: TicketResponse): void {
    this.ticketInModifica = ticket;
    this.erroreModifica = null;
    this.urgenzaModifica = ticket.urgenza;
    this.formModifica.reset({
      titolo: ticket.titolo,
      descrizione: ticket.descrizione,
      categoria: ticket.categoria,
      zonaId: ticket.zona.id,
      cubo: ticket.cubo ?? '',
      piano: ticket.piano ?? '',
    });
  }

  /**
   * Chiude il modal di modifica senza salvare.
   */
  chiudiModifica(): void {
    if (this.salvandoModifica) return;
    this.ticketInModifica = null;
  }

  /**
   * Al cambio di zona azzera il cubo, che apparteneva alla zona precedente.
   */
  onZonaModificaChange(): void {
    this.formModifica.controls.cubo.setValue('');
  }

  /**
   * Imposta il livello di urgenza selezionato con i pallini.
   */
  setUrgenzaModifica(livello: number): void {
    this.urgenzaModifica = livello;
  }

  /**
   * Invia le modifiche al backend e aggiorna il ticket nella lista locale.
   */
  salvaModifica(): void {
    if (!this.ticketInModifica || this.formModifica.invalid) {
      this.formModifica.markAllAsTouched();
      return;
    }

    const valori = this.formModifica.getRawValue();
    this.salvandoModifica = true;
    this.erroreModifica = null;

    this.adminService
      .modificaTicket(this.ticketInModifica.id, {
        titolo: valori.titolo!,
        descrizione: valori.descrizione!,
        categoria: valori.categoria!,
        zonaId: valori.zonaId!,
        cubo: valori.cubo || null,
        piano: valori.piano || null,
        urgenza: this.urgenzaModifica,
      })
      .subscribe({
        next: (aggiornato) => {
          const idx = this.tickets.findIndex((t) => t.id === aggiornato.id);
          if (idx !== -1) this.tickets[idx] = aggiornato;
          this.salvandoModifica = false;
          this.ticketInModifica = null;
          this.mostraSuccesso('Segnalazione modificata');
        },
        error: () => {
          this.salvandoModifica = false;
          this.erroreModifica = 'Impossibile salvare le modifiche. Riprova.';
        },
      });
  }

  // ─── Modal eliminazione ticket ──────────────────────────────────────────

  /**
   * Apre il modal di conferma per l'eliminazione definitiva del ticket.
   */
  apriElimina(ticket: TicketResponse): void {
    this.ticketDaEliminare = ticket;
    this.erroreElimina = null;
  }

  /**
   * Chiude il modal di conferma eliminazione.
   */
  chiudiElimina(): void {
    if (this.eliminando) return;
    this.ticketDaEliminare = null;
  }

  /**
   * Elimina il ticket selezionato e lo rimuove dalla lista locale.
   */
  confermaElimina(): void {
    if (!this.ticketDaEliminare) return;
    const id = this.ticketDaEliminare.id;
    this.eliminando = true;
    this.erroreElimina = null;

    this.adminService.eliminaTicket(id).subscribe({
      next: () => {
        this.tickets = this.tickets.filter((t) => t.id !== id);
        this.eliminando = false;
        this.ticketDaEliminare = null;
        this.mostraSuccesso('Segnalazione eliminata');
      },
      error: () => {
        this.eliminando = false;
        this.erroreElimina = 'Impossibile eliminare la segnalazione. Riprova.';
      },
    });
  }

  private mostraSuccesso(messaggio: string): void {
    this.messaggioSuccesso = messaggio;
    setTimeout(() => (this.messaggioSuccesso = null), 4000);
  }
}
