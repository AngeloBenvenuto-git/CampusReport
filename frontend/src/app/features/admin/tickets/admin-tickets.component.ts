import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { TecnicoAdminResponse } from '../../../shared/models/admin.models';
import { Categoria, Stato, TicketResponse } from '../../../shared/models/ticket.models';
import { CATEGORIA_LABEL, dataRelativa, STATO_COLOR, STATO_LABEL } from '../../../shared/utils/ticket-display.util';

@Component({
  selector: 'app-admin-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-tickets.component.html',
})
export class AdminTicketsComponent implements OnInit {
  readonly stati = Object.values(Stato);
  readonly categorie = Object.values(Categoria);
  readonly STATO_LABEL = STATO_LABEL;
  readonly STATO_COLOR = STATO_COLOR;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly dataRelativa = dataRelativa;

  caricamento = true;
  errore: string | null = null;

  tickets: TicketResponse[] = [];
  tecnici: TecnicoAdminResponse[] = [];

  // ─── Filtri ─────────────────────────────────────────────────────────────

  statoFiltro = '';
  categoriaFiltro = '';
  tecnicoFiltro = '';
  ricercaFiltro = '';

  esportando = false;

  // ─── Modal assegnazione manuale ─────────────────────────────────────────

  modalAssegnaAperto = false;
  ticketDaAssegnare: TicketResponse | null = null;
  tecnicoSelezionatoId = '';
  assegnando = false;
  erroreAssegna: string | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.caricaDati();
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
        link.download = 'tickets.csv';
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

  tecniciDisponibiliPerAssegnazione(ticket: TicketResponse): TecnicoAdminResponse[] {
    return this.tecnici.filter((t) => t.attivo && t.specializzazioni.includes(ticket.categoria));
  }

  apriAssegna(ticket: TicketResponse): void {
    this.ticketDaAssegnare = ticket;
    this.tecnicoSelezionatoId = '';
    this.erroreAssegna = null;
    this.modalAssegnaAperto = true;
  }

  chiudiAssegna(): void {
    this.modalAssegnaAperto = false;
    this.ticketDaAssegnare = null;
  }

  confermaAssegna(): void {
    if (!this.ticketDaAssegnare || !this.tecnicoSelezionatoId) return;

    const ticketId = this.ticketDaAssegnare.id;
    this.assegnando = true;
    this.erroreAssegna = null;

    this.adminService.assegnaManualmente(ticketId, this.tecnicoSelezionatoId).subscribe({
      next: (aggiornato) => {
        const idx = this.tickets.findIndex((t) => t.id === aggiornato.id);
        if (idx !== -1) this.tickets[idx] = aggiornato;
        this.assegnando = false;
        this.modalAssegnaAperto = false;
        this.ticketDaAssegnare = null;
      },
      error: () => {
        this.assegnando = false;
        this.erroreAssegna = 'Impossibile assegnare il ticket.';
      },
    });
  }
}
