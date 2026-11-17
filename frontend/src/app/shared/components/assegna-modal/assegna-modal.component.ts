import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { TecnicoAdminResponse } from '../../models/admin.models';
import { TicketResponse } from '../../models/ticket.models';
import { CATEGORIA_LABEL } from '../../utils/ticket-display.util';

/**
 * Modal di assegnazione manuale di un ticket a un tecnico, usato dal pannello admin.
 * Propone solo i tecnici attivi con la specializzazione della categoria del ticket.
 */
@Component({
  selector: 'app-assegna-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assegna-modal.component.html',
})
export class AssegnaModalComponent {
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;

  /** Ticket da assegnare. */
  @Input({ required: true }) ticket!: TicketResponse;

  /** Elenco completo dei tecnici; il filtro per attivo/specializzazione è applicato qui. */
  @Input({ required: true }) tecnici: TecnicoAdminResponse[] = [];

  /** Emesso con il ticket aggiornato dopo un'assegnazione riuscita. */
  @Output() assegnato = new EventEmitter<TicketResponse>();

  /** Emesso quando l'utente chiude il modal senza assegnare. */
  @Output() chiuso = new EventEmitter<void>();

  tecnicoSelezionatoId = '';
  assegnando = false;
  erroreAssegna: string | null = null;

  constructor(private adminService: AdminService) {}

  /**
   * Tecnici attivi la cui specializzazione include la categoria del ticket.
   */
  get tecniciDisponibili(): TecnicoAdminResponse[] {
    return this.tecnici.filter((t) => t.attivo && t.specializzazioni.includes(this.ticket.categoria));
  }

  /**
   * Chiude il modal (ignorato mentre l'assegnazione è in corso).
   */
  chiudi(): void {
    if (this.assegnando) return;
    this.chiuso.emit();
  }

  /**
   * Assegna il ticket al tecnico selezionato ed emette il ticket aggiornato.
   */
  conferma(): void {
    if (!this.tecnicoSelezionatoId) return;

    this.assegnando = true;
    this.erroreAssegna = null;

    this.adminService.assegnaManualmente(this.ticket.id, this.tecnicoSelezionatoId).subscribe({
      next: (aggiornato) => {
        this.assegnando = false;
        this.assegnato.emit(aggiornato);
      },
      error: () => {
        this.assegnando = false;
        this.erroreAssegna = 'Impossibile assegnare il ticket.';
      },
    });
  }
}
