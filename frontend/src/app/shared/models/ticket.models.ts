import { Ruolo } from './auth.models';

export enum Categoria {
  ELETTRICO = 'ELETTRICO',
  WIFI = 'WIFI',
  IDRAULICO = 'IDRAULICO',
  ATTREZZATURA = 'ATTREZZATURA',
  ALTRO = 'ALTRO',
}

export enum Stato {
  APERTA = 'APERTA',
  ASSEGNATA = 'ASSEGNATA',
  IN_LAVORAZIONE = 'IN_LAVORAZIONE',
  COMPLETATA = 'COMPLETATA',
  IN_ATTESA = 'IN_ATTESA',
  RIFIUTATA = 'RIFIUTATA',
}

export enum Priorita {
  NORMALE = 'NORMALE',
  ALTA = 'ALTA',
}

export interface ZonaResponse {
  id: string;
  nome: string;
  descrizione: string;
  geojson: string;
  colore: string;
}

export interface UserResponse {
  id: string;
  nome: string;
  cognome: string;
  email: string;
  ruolo: Ruolo;
}

export interface CambioStatoResponse {
  id: string;
  statoPrecedente: Stato;
  statoNuovo: Stato;
  utente: UserResponse;
  nota: string | null;
  timestamp: string;
}

export interface TicketResponse {
  id: string;
  titolo: string;
  descrizione: string;
  categoria: Categoria;
  stato: Stato;
  priorita: Priorita;
  zona: ZonaResponse;
  segnalante: UserResponse;
  tecnico: UserResponse | null;
  categoriaConfidenza: number | null;
  createdAt: string;
  updatedAt: string;
  storico: CambioStatoResponse[];
}

export interface TicketRequest {
  zonaId: string;
  titolo: string;
  descrizione: string;
  categoria: Categoria;
}
