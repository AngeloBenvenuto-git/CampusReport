import { Categoria } from './ticket.models';

export interface TecnicoAdminResponse {
  id: string;
  nome: string;
  cognome: string;
  email: string;
  specializzazioni: Categoria[];
  zona: string;
  caricoMassimo: number;
  caricoAttuale: number;
  attivo: boolean;
}

export interface SettimanaData {
  settimana: string;
  count: number;
}

export interface TecnicoPerformance {
  nome: string;
  completate: number;
  inLavorazione: number;
}

export interface AdminStatisticheResponse {
  totaleTicketAttivi: number;
  ticketInAttesa: number;
  tecniciAttivi: number;
  tempoMedioRisoluzioneOre: number;
  ticketPerStato: Record<string, number>;
  ticketPerCategoria: Record<string, number>;
  /** Numero di segnalazioni per livello di urgenza (chiavi "1"-"5"). */
  distribuzioneUrgenza: Record<string, number>;
  ticketPerSettimana: SettimanaData[];
  performanceTecnici: TecnicoPerformance[];
}

export interface ConfigPesiResponse {
  alpha: number;
  beta: number;
  gamma: number;
  delta: number;
}

export type ConfigPesiRequest = ConfigPesiResponse;

export interface CreaTecnicoRequest {
  nome: string;
  cognome: string;
  email: string;
  specializzazioni: Categoria[];
  zona: string;
  caricoMassimo: number;
}

export interface ModificaTecnicoRequest {
  specializzazioni: Categoria[];
  zona: string;
  caricoMassimo: number;
}

export interface AdminModificaTicketRequest {
  titolo: string;
  descrizione: string;
  categoria: Categoria;
  zonaId: string;
  cubo: string | null;
  piano: string | null;
  urgenza: number | null;
}

export interface AdminTicketFiltri {
  stato?: string;
  categoria?: string;
  tecnicoId?: string;
  ricerca?: string;
}
