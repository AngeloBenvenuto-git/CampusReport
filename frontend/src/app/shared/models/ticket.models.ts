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

export enum TipoRifiuto {
  RIASSEGNA = 'RIASSEGNA',
  ELIMINA = 'ELIMINA',
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
  cubo: string;
  piano: string;
  zona: ZonaResponse;
  segnalante: UserResponse;
  tecnico: UserResponse | null;
  categoriaConfidenza: number | null;
  /** Urgenza 1-5; null per i ticket creati prima dell'introduzione del campo. */
  urgenza: number | null;
  createdAt: string;
  updatedAt: string;
  storico: CambioStatoResponse[];
}

export interface TicketRequest {
  zonaId: string;
  cubo?: string;
  piano?: string;
  titolo: string;
  descrizione: string;
  categoria: Categoria;
  /** Urgenza 1-5; se assente il backend usa la stima del microservizio NLP. */
  urgenza?: number;
}

/** Risposta del microservizio NLP per la stima dell'urgenza. */
export interface StimaUrgenzaResponse {
  urgenza: number;
  confidenza: number;
}

export interface ModificaTicketRequest {
  titolo: string;
  descrizione: string;
  categoria: Categoria;
  cubo?: string;
  piano?: string;
}
