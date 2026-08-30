import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { TicketService } from '../../core/services/ticket.service';
import { Priorita, Stato, TicketResponse } from '../../shared/models/ticket.models';
import { dataRelativa, STATO_BADGE_CLASS, STATO_LABEL } from '../../shared/utils/ticket-display.util';

const CARICO_MASSIMO = 10;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  readonly oggi = new Date();
  readonly caricoMassimo = CARICO_MASSIMO;
  readonly dataRelativa = dataRelativa;

  caricamento = true;
  errore: string | null = null;

  tickets: TicketResponse[] = [];
  loading: Record<string, boolean> = {};

  modalRifiutoAperto = false;
  ticketDaRifiutare: TicketResponse | null = null;
  motivazioneRifiuto = '';
  erroreRifiuto: string | null = null;
  inviandoRifiuto = false;

  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.ticketService.getTicketsAssegnati().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare le segnalazioni assegnate.';
      },
    });
  }

  // ─── Utente ─────────────────────────────────────────────────────────────

  get nomeUtente(): string {
    return this.authService.getCurrentUser()?.nome ?? '';
  }

  // ─── Liste derivate ─────────────────────────────────────────────────────

  get ticketAttivi(): TicketResponse[] {
    return this.tickets
      .filter((t) => t.stato === Stato.ASSEGNATA || t.stato === Stato.IN_LAVORAZIONE)
      .sort((a, b) => {
        if (a.priorita === Priorita.ALTA && b.priorita !== Priorita.ALTA) return -1;
        if (b.priorita === Priorita.ALTA && a.priorita !== Priorita.ALTA) return 1;
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      });
  }

  get ticketCompletati(): TicketResponse[] {
    return this.tickets
      .filter((t) => t.stato === Stato.COMPLETATA)
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      .slice(0, 10);
  }

  // ─── Statistiche ────────────────────────────────────────────────────────

  get assegnateOggi(): number {
    const oggi = new Date().toDateString();
    return this.tickets.filter((t) => new Date(t.createdAt).toDateString() === oggi).length;
  }

  get inLavorazione(): number {
    return this.tickets.filter((t) => t.stato === Stato.IN_LAVORAZIONE).length;
  }

  get completateQuestoMese(): number {
    const ora = new Date();
    return this.tickets.filter((t) => {
      const d = new Date(t.updatedAt);
      return t.stato === Stato.COMPLETATA && d.getMonth() === ora.getMonth() && d.getFullYear() === ora.getFullYear();
    }).length;
  }

  get totaleAttive(): number {
    return this.ticketAttivi.length;
  }

  get percentualeCarico(): number {
    return Math.min(100, (this.totaleAttive / CARICO_MASSIMO) * 100);
  }

  // ─── Display helpers ────────────────────────────────────────────────────

  statoLabel(stato: Stato): string {
    return STATO_LABEL[stato];
  }

  statoBadgeClass(stato: Stato): string {
    return STATO_BADGE_CLASS[stato];
  }

  // ─── Azioni ─────────────────────────────────────────────────────────────

  prendiInCarico(ticket: TicketResponse): void {
    this.loading[ticket.id] = true;
    this.ticketService.aggiornaStato(ticket.id, { statoNuovo: 'IN_LAVORAZIONE', nota: null }).subscribe({
      next: (updated) => this.aggiornaTicketLocale(updated),
      error: () => {
        this.loading[ticket.id] = false;
        this.errore = 'Impossibile prendere in carico la segnalazione. Riprova più tardi.';
      },
    });
  }

  segnaCompletata(ticket: TicketResponse): void {
    this.loading[ticket.id] = true;
    this.ticketService.aggiornaStato(ticket.id, { statoNuovo: 'COMPLETATA', nota: null }).subscribe({
      next: (updated) => this.aggiornaTicketLocale(updated),
      error: () => {
        this.loading[ticket.id] = false;
        this.errore = 'Impossibile completare la segnalazione. Riprova più tardi.';
      },
    });
  }

  private aggiornaTicketLocale(updated: TicketResponse): void {
    const idx = this.tickets.findIndex((t) => t.id === updated.id);
    if (idx !== -1) this.tickets[idx] = updated;
    this.loading[updated.id] = false;
  }

  // ─── Modal rifiuto ──────────────────────────────────────────────────────

  apriRifiuto(ticket: TicketResponse): void {
    this.ticketDaRifiutare = ticket;
    this.motivazioneRifiuto = '';
    this.erroreRifiuto = null;
    this.modalRifiutoAperto = true;
  }

  chiudiRifiuto(): void {
    this.modalRifiutoAperto = false;
    this.ticketDaRifiutare = null;
  }

  confermaRifiuto(): void {
    if (!this.ticketDaRifiutare || this.motivazioneRifiuto.trim().length < 10) return;

    const ticketId = this.ticketDaRifiutare.id;
    this.inviandoRifiuto = true;
    this.erroreRifiuto = null;

    this.ticketService.rifiutaTicket(ticketId, { motivazione: this.motivazioneRifiuto.trim() }).subscribe({
      next: () => {
        this.tickets = this.tickets.filter((t) => t.id !== ticketId);
        this.inviandoRifiuto = false;
        this.modalRifiutoAperto = false;
        this.ticketDaRifiutare = null;
      },
      error: (err: HttpErrorResponse) => {
        this.inviandoRifiuto = false;
        this.erroreRifiuto =
          err.status === 400
            ? 'Motivazione non valida.'
            : 'Impossibile rifiutare la segnalazione. Riprova più tardi.';
      },
    });
  }
}
