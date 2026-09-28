import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ModificaTicketRequest,
  StimaUrgenzaResponse,
  TicketRequest,
  TicketResponse,
  ZonaResponse,
} from '../../shared/models/ticket.models';

/** Urgenza media usata quando il microservizio NLP non è raggiungibile. */
export const URGENZA_DEFAULT = 3;

/**
 * Espone le operazioni REST relative a zone e ticket di segnalazione.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly apiUrl = environment.apiUrl;
  private readonly nlpUrl = environment.nlpUrl;

  constructor(private http: HttpClient) {}

  getZone(): Observable<ZonaResponse[]> {
    return this.http.get<ZonaResponse[]>(`${this.apiUrl}/api/map/zone`);
  }

  getMyTickets(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/api/tickets/miei`);
  }

  getTicketsAssegnati(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/api/tickets/assegnati`);
  }

  getTicket(id: string): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.apiUrl}/api/tickets/${id}`);
  }

  createTicket(request: TicketRequest): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.apiUrl}/api/tickets`, request);
  }

  /**
   * Stima l'urgenza (1-5) della descrizione chiamando direttamente il microservizio NLP
   * (non passa dal backend). In caso di errore restituisce urgenza 3 con confidenza 0.
   */
  stimaUrgenza(testo: string): Observable<StimaUrgenzaResponse> {
    return this.http
      .post<StimaUrgenzaResponse>(`${this.nlpUrl}/classify-urgenza`, { testo })
      .pipe(catchError(() => of({ urgenza: URGENZA_DEFAULT, confidenza: 0 })));
  }

  modificaTicket(id: string, request: ModificaTicketRequest): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.apiUrl}/api/tickets/${id}`, request);
  }

  aggiornaStato(id: string, request: { statoNuovo: string; nota: string | null }): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/api/tickets/${id}/stato`, request);
  }

  rifiutaTicket(
    id: string,
    request: { motivazione: string; tipoRifiuto: 'RIASSEGNA' | 'ELIMINA' },
  ): Observable<TicketResponse | null> {
    // Con tipoRifiuto = ELIMINA il backend risponde 204 No Content: HttpClient
    // restituisce `null` come valore emesso, senza passare per l'error handler.
    return this.http.post<TicketResponse | null>(`${this.apiUrl}/api/tickets/${id}/rifiuta`, request);
  }
}
