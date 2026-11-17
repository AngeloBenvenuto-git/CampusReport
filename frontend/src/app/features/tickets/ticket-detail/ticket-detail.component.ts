import { CommonModule, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ChatComponent } from '../../../shared/components/chat/chat.component';
import { TicketService } from '../../../core/services/ticket.service';
import { Categoria, ModificaTicketRequest, Stato, TicketResponse } from '../../../shared/models/ticket.models';
import { CATEGORIA_LABEL, STATO_BADGE_CLASS, URGENZA_BADGE_CLASS, URGENZA_BADGE_LABEL, STATO_COLOR, STATO_LABEL } from '../../../shared/utils/ticket-display.util';
import { ZONE_MAP_DEFS } from '../../map/zone-map.data';

interface CategoriaOption {
  value: Categoria;
  label: string;
}

const PIANI = ['Piano Terra', '1° Piano', '2° Piano', '3° Piano', '4° Piano', 'Esterno/Area comune'];

const STATI_MODIFICABILI: Stato[] = [Stato.APERTA, Stato.ASSEGNATA];

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, ChatComponent],
  templateUrl: './ticket-detail.component.html',
})
export class TicketDetailComponent implements OnInit {
  readonly STATO_LABEL = STATO_LABEL;
  readonly STATO_BADGE_CLASS = STATO_BADGE_CLASS;
  readonly URGENZA_BADGE_CLASS = URGENZA_BADGE_CLASS;
  readonly URGENZA_BADGE_LABEL = URGENZA_BADGE_LABEL;
  readonly STATO_COLOR = STATO_COLOR;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly Stato = Stato;

  readonly piani = PIANI;
  readonly categorie: CategoriaOption[] = Object.values(Categoria).map((value) => ({
    value,
    label: CATEGORIA_LABEL[value],
  }));

  caricamento = true;
  errore: string | null = null;
  ticket: TicketResponse | null = null;

  modalModificaAperto = false;
  loadingModifica = false;
  erroreModifica: string | null = null;

  formModifica = this.fb.group({
    titolo: this.fb.control<string>('', [Validators.required, Validators.maxLength(200)]),
    descrizione: this.fb.control<string>('', [Validators.required, Validators.maxLength(2000)]),
    categoria: this.fb.control<Categoria | null>(null, Validators.required),
    cubo: this.fb.control<string>(''),
    piano: this.fb.control<string>(''),
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ticketService: TicketService,
    private authService: AuthService,
    private fb: FormBuilder,
    private location: Location,
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

  get puoModificare(): boolean {
    if (!this.ticket) return false;
    const utenteCorrente = this.authService.getCurrentUser();
    return STATI_MODIFICABILI.includes(this.ticket.stato) && this.ticket.segnalante.id === utenteCorrente?.id;
  }

  get cubiDisponibili(): string[] {
    if (!this.ticket) return [];
    const zonaDef = ZONE_MAP_DEFS.find((z) => z.nome === this.ticket?.zona.nome);
    return zonaDef?.cubi ?? [];
  }

  /** True se l'utente corrente è l'admin: non partecipa alla chat del ticket. */
  get isAdmin(): boolean {
    return this.authService.getRuolo() === 'ADMIN';
  }

  /** Breadcrumb verso l'elenco di provenienza in base al ruolo. */
  get breadcrumb(): { label: string; link: string } {
    switch (this.authService.getRuolo()) {
      case 'ADMIN':
        return { label: 'Tutti i ticket', link: '/admin/tickets' };
      case 'TECNICO':
        return { label: 'Dashboard', link: '/dashboard' };
      default:
        return { label: 'Le mie segnalazioni', link: '/tickets' };
    }
  }

  /**
   * Torna alla pagina di provenienza. Se la pagina è stata aperta direttamente
   * (prima navigazione dell'app, navigationId === 1) usa la home del ruolo:
   * ADMIN → /admin/tickets, TECNICO → /dashboard, altri → /tickets.
   */
  torna(): void {
    const navigationId = (this.location.getState() as { navigationId?: number } | null)?.navigationId ?? 1;
    if (navigationId > 1) {
      this.location.back();
      return;
    }
    this.router.navigate([this.breadcrumb.link]);
  }

  apriModifica(): void {
    if (!this.ticket) return;
    this.erroreModifica = null;
    this.formModifica.reset({
      titolo: this.ticket.titolo,
      descrizione: this.ticket.descrizione,
      categoria: this.ticket.categoria,
      cubo: this.ticket.cubo ?? '',
      piano: this.ticket.piano ?? '',
    });
    this.modalModificaAperto = true;
  }

  chiudiModifica(): void {
    this.modalModificaAperto = false;
  }

  onSalvaModifica(): void {
    if (this.formModifica.invalid || !this.ticket) {
      this.formModifica.markAllAsTouched();
      return;
    }

    this.loadingModifica = true;
    this.erroreModifica = null;

    const valori = this.formModifica.getRawValue();
    const request: ModificaTicketRequest = {
      titolo: valori.titolo as string,
      descrizione: valori.descrizione as string,
      categoria: valori.categoria as Categoria,
      cubo: valori.cubo || undefined,
      piano: valori.piano || undefined,
    };

    this.ticketService.modificaTicket(this.ticket.id, request).subscribe({
      next: (updated) => {
        this.ticket = updated;
        this.modalModificaAperto = false;
        this.loadingModifica = false;
      },
      error: (err: HttpErrorResponse) => {
        this.erroreModifica =
          err.status === 403 ? 'Non puoi più modificare questa segnalazione' : 'Errore durante il salvataggio. Riprova.';
        this.loadingModifica = false;
      },
    });
  }
}
