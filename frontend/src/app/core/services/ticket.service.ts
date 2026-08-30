import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TicketRequest, TicketResponse, ZonaResponse } from '../../shared/models/ticket.models';

/**
 * Espone le operazioni REST relative a zone e ticket di segnalazione.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly apiUrl = environment.apiUrl;

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
}
