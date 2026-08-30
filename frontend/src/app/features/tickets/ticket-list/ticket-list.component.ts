import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TicketService } from '../../../core/services/ticket.service';
import { Categoria, Stato, TicketResponse } from '../../../shared/models/ticket.models';
import {
  CATEGORIA_LABEL,
  dataRelativa,
  STATO_BADGE_CLASS,
  STATO_COLOR,
  STATO_LABEL,
} from '../../../shared/utils/ticket-display.util';

type Ordinamento = 'data_desc' | 'data_asc' | 'priorita';

interface SegmentoDonut {
  stato: Stato;
  label: string;
  colore: string;
  conteggio: number;
  percentuale: number;
  dashArray: string;
  dashOffset: number;
}

const DONUT_RADIUS = 40;
const DONUT_CIRCONFERENZA = 2 * Math.PI * DONUT_RADIUS;

interface BarraCategoria {
  categoria: Categoria;
  label: string;
  conteggio: number;
  percentuale: number;
}

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ticket-list.component.html',
})
export class TicketListComponent implements OnInit {
  readonly STATO_LABEL = STATO_LABEL;
  readonly STATO_BADGE_CLASS = STATO_BADGE_CLASS;
  readonly STATO_COLOR = STATO_COLOR;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly dataRelativa = dataRelativa;

  readonly stati = Object.values(Stato);
  readonly categorie = Object.values(Categoria);

  caricamento = true;
  errore: string | null = null;
  tickets: TicketResponse[] = [];

  statoFiltro = '';
  categoriaFiltro = '';
  ricerca = '';
  ordinamento: Ordinamento = 'data_desc';

  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
    private router: Router,
  ) {}

  get titoloPagina(): string {
    return this.authService.getRuolo() === 'TECNICO' ? 'Le mie segnalazioni assegnate' : 'Le mie segnalazioni';
  }

  ngOnInit(): void {
    const ruolo = this.authService.getRuolo();
    const tickets$ = ruolo === 'TECNICO' ? this.ticketService.getTicketsAssegnati() : this.ticketService.getMyTickets();

    tickets$.subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare le segnalazioni.';
      },
    });
  }

  // ─── Statistiche ────────────────────────────────────────────────────────

  private questoMese(t: TicketResponse): boolean {
    const ora = new Date();
    const data = new Date(t.createdAt);
    return data.getFullYear() === ora.getFullYear() && data.getMonth() === ora.getMonth();
  }

  get totale(): number {
    return this.tickets.length;
  }

  get apertoAssegnato(): TicketResponse[] {
    return this.tickets.filter((t) => t.stato === Stato.APERTA || t.stato === Stato.ASSEGNATA);
  }

  get inLavorazione(): TicketResponse[] {
    return this.tickets.filter((t) => t.stato === Stato.IN_LAVORAZIONE);
  }

  get completate(): TicketResponse[] {
    return this.tickets.filter((t) => t.stato === Stato.COMPLETATA);
  }

  get trendTotale(): number {
    return this.tickets.filter((t) => this.questoMese(t)).length;
  }

  get trendApertoAssegnato(): number {
    return this.apertoAssegnato.filter((t) => this.questoMese(t)).length;
  }

  get trendInLavorazione(): number {
    return this.inLavorazione.filter((t) => this.questoMese(t)).length;
  }

  get trendCompletate(): number {
    return this.completate.filter((t) => this.questoMese(t)).length;
  }

  // ─── Grafici ────────────────────────────────────────────────────────────

  readonly donutRaggio = DONUT_RADIUS;
  readonly donutCirconferenza = DONUT_CIRCONFERENZA;

  get donutSegmenti(): SegmentoDonut[] {
    const totale = this.tickets.length || 1;
    let cumulatoLunghezza = 0;
    return this.stati
      .map((stato) => {
        const conteggio = this.tickets.filter((t) => t.stato === stato).length;
        const percentuale = (conteggio / totale) * 100;
        const lunghezza = (percentuale / 100) * DONUT_CIRCONFERENZA;
        const dashArray = `${lunghezza} ${DONUT_CIRCONFERENZA}`;
        const dashOffset = -cumulatoLunghezza;
        cumulatoLunghezza += lunghezza;
        return {
          stato,
          label: STATO_LABEL[stato],
          colore: STATO_COLOR[stato],
          conteggio,
          percentuale,
          dashArray,
          dashOffset,
        };
      })
      .filter((s) => s.conteggio > 0);
  }

  get barreCategorie(): BarraCategoria[] {
    const massimo = Math.max(1, ...this.categorie.map((c) => this.tickets.filter((t) => t.categoria === c).length));
    return this.categorie.map((categoria) => {
      const conteggio = this.tickets.filter((t) => t.categoria === categoria).length;
      return {
        categoria,
        label: CATEGORIA_LABEL[categoria],
        conteggio,
        percentuale: (conteggio / massimo) * 100,
      };
    });
  }

  // ─── Filtri e lista ─────────────────────────────────────────────────────

  get ticketFiltrati(): TicketResponse[] {
    let risultato = this.tickets;

    if (this.statoFiltro) {
      risultato = risultato.filter((t) => t.stato === this.statoFiltro);
    }
    if (this.categoriaFiltro) {
      risultato = risultato.filter((t) => t.categoria === this.categoriaFiltro);
    }
    if (this.ricerca.trim()) {
      const q = this.ricerca.trim().toLowerCase();
      risultato = risultato.filter((t) => t.titolo.toLowerCase().includes(q));
    }

    const ordinato = [...risultato];
    switch (this.ordinamento) {
      case 'data_asc':
        ordinato.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        break;
      case 'priorita':
        ordinato.sort((a, b) => (a.priorita === b.priorita ? 0 : a.priorita === 'ALTA' ? -1 : 1));
        break;
      default:
        ordinato.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }
    return ordinato;
  }

  apriDettaglio(ticket: TicketResponse): void {
    this.router.navigate(['/tickets', ticket.id]);
  }
}
