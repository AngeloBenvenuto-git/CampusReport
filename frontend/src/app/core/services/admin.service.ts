import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminStatisticheResponse,
  AdminTicketFiltri,
  ConfigPesiResponse,
  CreaTecnicoRequest,
  ModificaTecnicoRequest,
  TecnicoAdminResponse,
} from '../../shared/models/admin.models';
import { TicketResponse } from '../../shared/models/ticket.models';

/**
 * Espone le operazioni REST del pannello di amministrazione: statistiche,
 * gestione tecnici, gestione ticket e configurazione dell'algoritmo di assegnazione.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/api/admin`;

  constructor(private http: HttpClient) {}

  // ─── Statistiche ────────────────────────────────────────────────────────

  getStatistiche(): Observable<AdminStatisticheResponse> {
    return this.http.get<AdminStatisticheResponse>(`${this.apiUrl}/statistiche`);
  }

  // ─── Ticket ─────────────────────────────────────────────────────────────

  getAllTickets(filtri: AdminTicketFiltri = {}): Observable<TicketResponse[]> {
    let params = new HttpParams().set('size', '1000');
    if (filtri.stato) params = params.set('stato', filtri.stato);
    if (filtri.categoria) params = params.set('categoria', filtri.categoria);
    if (filtri.tecnicoId) params = params.set('tecnicoId', filtri.tecnicoId);
    if (filtri.ricerca) params = params.set('ricerca', filtri.ricerca);
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/tickets`, { params });
  }

  assegnaManualmente(ticketId: string, tecnicoId: string): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/tickets/${ticketId}/assegna`, { tecnicoId });
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/csv`, { responseType: 'blob' });
  }

  // ─── Tecnici ────────────────────────────────────────────────────────────

  getTecnici(): Observable<TecnicoAdminResponse[]> {
    return this.http.get<TecnicoAdminResponse[]>(`${this.apiUrl}/tecnici`);
  }

  creaTecnico(request: CreaTecnicoRequest): Observable<TecnicoAdminResponse> {
    return this.http.post<TecnicoAdminResponse>(`${this.apiUrl}/tecnici`, request);
  }

  modificaTecnico(id: string, request: ModificaTecnicoRequest): Observable<TecnicoAdminResponse> {
    return this.http.put<TecnicoAdminResponse>(`${this.apiUrl}/tecnici/${id}`, request);
  }

  cambioStatoTecnico(id: string, attivo: boolean): Observable<TecnicoAdminResponse> {
    return this.http.patch<TecnicoAdminResponse>(`${this.apiUrl}/tecnici/${id}/stato`, { attivo });
  }

  // ─── Configurazione algoritmo ───────────────────────────────────────────

  getPesi(): Observable<ConfigPesiResponse> {
    return this.http.get<ConfigPesiResponse>(`${this.apiUrl}/config/pesi`);
  }

  aggiornaPesi(alpha: number, beta: number): Observable<ConfigPesiResponse> {
    return this.http.post<ConfigPesiResponse>(`${this.apiUrl}/config/pesi`, { alpha, beta });
  }
}
