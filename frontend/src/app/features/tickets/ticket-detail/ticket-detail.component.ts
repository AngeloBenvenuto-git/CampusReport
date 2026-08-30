import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketResponse } from '../../../shared/models/ticket.models';
import { CATEGORIA_LABEL, STATO_BADGE_CLASS, STATO_COLOR, STATO_LABEL } from '../../../shared/utils/ticket-display.util';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './ticket-detail.component.html',
})
export class TicketDetailComponent implements OnInit {
  readonly STATO_LABEL = STATO_LABEL;
  readonly STATO_BADGE_CLASS = STATO_BADGE_CLASS;
  readonly STATO_COLOR = STATO_COLOR;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;

  caricamento = true;
  errore: string | null = null;
  ticket: TicketResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ticketService: TicketService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/tickets']);
      return;
    }

    this.ticketService.getTicket(id).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.caricamento = false;
      },
      error: (err: HttpErrorResponse) => {
        this.caricamento = false;
        this.errore =
          err.status === 403
            ? 'Non sei autorizzato a visualizzare questa segnalazione.'
            : 'Impossibile caricare la segnalazione richiesta.';
      },
    });
  }

  get inizialiTecnico(): string {
    const tecnico = this.ticket?.tecnico;
    if (!tecnico) return '';
    return `${tecnico.nome.charAt(0)}${tecnico.cognome.charAt(0)}`.toUpperCase();
  }

  torna(): void {
    this.router.navigate(['/tickets']);
  }
}
