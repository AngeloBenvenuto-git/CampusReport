import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../core/services/ticket.service';
import { ChatResponse } from '../../models/ticket.models';

/** Intervallo di polling dei messaggi non letti (ms). */
const POLLING_INTERVAL_MS = 10000;

/**
 * Chat tra segnalante e tecnico assegnato per un singolo ticket.
 *
 * <p>Carica la conversazione all'avvio e controlla ogni 10 secondi se ci sono
 * nuovi messaggi non letti, ricaricando la chat solo in quel caso. Usata sia
 * nel dettaglio segnalazione (studente/docente) sia nella dashboard tecnico.
 */
@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
})
export class ChatComponent implements OnInit, OnDestroy {
  /** Identificativo del ticket di cui mostrare la chat. */
  @Input({ required: true }) ticketId!: string;

  /** Titolo mostrato nell'header della card. */
  @Input() titolo = 'Chat con il tecnico';

  /** Testo secondario dello stato vuoto. */
  @Input() suggerimentoVuoto = 'Inizia la conversazione con il tecnico.';

  /** Emesso dopo ogni caricamento della chat: i messaggi ricevuti sono ora letti. */
  @Output() chatLetta = new EventEmitter<void>();

  @ViewChild('chatMessages') private chatContainer?: ElementRef<HTMLDivElement>;

  chat: ChatResponse | null = null;
  nuovoMessaggio = '';
  loadingMessaggio = false;
  errore: string | null = null;

  private pollingInterval: ReturnType<typeof setInterval> | undefined;

  constructor(private ticketService: TicketService) {}

  ngOnInit(): void {
    this.caricaChat();
    this.avviaPolling();
  }

  ngOnDestroy(): void {
    clearInterval(this.pollingInterval);
  }

  /** Carica (o ricarica) la conversazione e scorre in fondo. */
  caricaChat(): void {
    this.ticketService.getChat(this.ticketId).subscribe({
      next: (chat) => {
        this.chat = chat;
        this.errore = null;
        this.chatLetta.emit();
        this.scrollToBottom();
      },
      error: () => {
        this.errore = 'Impossibile caricare la chat.';
      },
    });
  }

  /** Avvia il controllo periodico dei messaggi non letti. */
  avviaPolling(): void {
    clearInterval(this.pollingInterval);
    this.pollingInterval = setInterval(() => {
      this.ticketService.getNonLetti(this.ticketId).subscribe((result) => {
        if (result.nonLetti > 0) {
          this.caricaChat();
        }
      });
    }, POLLING_INTERVAL_MS);
  }

  /** Invia il messaggio digitato; in caso di errore il testo viene ripristinato. */
  inviaMessaggio(): void {
    const testo = this.nuovoMessaggio.trim();
    if (!testo || this.loadingMessaggio) return;

    this.loadingMessaggio = true;
    this.errore = null;
    this.nuovoMessaggio = '';

    this.ticketService.inviaMessaggio(this.ticketId, { testo }).subscribe({
      next: (msg) => {
        if (this.chat) {
          this.chat.messaggi.push(msg);
        }
        this.loadingMessaggio = false;
        this.scrollToBottom();
      },
      error: () => {
        this.nuovoMessaggio = testo;
        this.loadingMessaggio = false;
        this.errore = 'Invio non riuscito. Riprova.';
      },
    });
  }

  /** Scorre l'area messaggi fino all'ultimo messaggio, dopo il render. */
  scrollToBottom(): void {
    setTimeout(() => {
      const container = this.chatContainer?.nativeElement;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 50);
  }
}
