import { Categoria, Stato } from '../models/ticket.models';

export const STATO_COLOR: Record<Stato, string> = {
  [Stato.APERTA]: '#EF4444',
  [Stato.ASSEGNATA]: '#F59E0B',
  [Stato.IN_LAVORAZIONE]: '#3B82F6',
  [Stato.COMPLETATA]: '#10B981',
  [Stato.IN_ATTESA]: '#9CA3AF',
  [Stato.RIFIUTATA]: '#6B7280',
};

export const STATO_BADGE_CLASS: Record<Stato, string> = {
  [Stato.APERTA]: 'bg-red-50 text-red-600',
  [Stato.ASSEGNATA]: 'bg-yellow-50 text-yellow-600',
  [Stato.IN_LAVORAZIONE]: 'bg-blue-50 text-blue-600',
  [Stato.COMPLETATA]: 'bg-green-50 text-green-600',
  [Stato.IN_ATTESA]: 'bg-gray-100 text-gray-500',
  [Stato.RIFIUTATA]: 'bg-gray-100 text-gray-400',
};

export const STATO_LABEL: Record<Stato, string> = {
  [Stato.APERTA]: 'Aperta',
  [Stato.ASSEGNATA]: 'Assegnata',
  [Stato.IN_LAVORAZIONE]: 'In lavorazione',
  [Stato.COMPLETATA]: 'Completata',
  [Stato.IN_ATTESA]: 'In attesa',
  [Stato.RIFIUTATA]: 'Rifiutata',
};

export const CATEGORIA_LABEL: Record<Categoria, string> = {
  [Categoria.ELETTRICO]: 'Elettrico',
  [Categoria.WIFI]: 'WiFi',
  [Categoria.IDRAULICO]: 'Idraulico',
  [Categoria.ATTREZZATURA]: 'Attrezzatura',
  [Categoria.ALTRO]: 'Altro',
};

export const CATEGORIA_INIZIALE: Record<Categoria, string> = {
  [Categoria.ELETTRICO]: 'E',
  [Categoria.WIFI]: 'W',
  [Categoria.IDRAULICO]: 'I',
  [Categoria.ATTREZZATURA]: 'A',
  [Categoria.ALTRO]: '?',
};

/** Etichetta del badge di urgenza (livelli 1-5). */
export const URGENZA_BADGE_LABEL: Record<number, string> = {
  5: '⚡ Critica',
  4: '↑ Alta',
  3: '→ Media',
  2: '↓ Bassa',
  1: 'Minima',
};

/** Classi Tailwind del badge di urgenza (livelli 1-5). */
export const URGENZA_BADGE_CLASS: Record<number, string> = {
  5: 'bg-red-100 text-red-700',
  4: 'bg-orange-100 text-orange-700',
  3: 'bg-yellow-100 text-yellow-700',
  2: 'bg-sky-100 text-sky-700',
  1: 'bg-gray-100 text-gray-500',
};

/** Colore esadecimale per ogni livello di urgenza (usato nei grafici). */
export const URGENZA_COLOR: Record<number, string> = {
  5: '#EF4444',
  4: '#F97316',
  3: '#EAB308',
  2: '#38BDF8',
  1: '#9CA3AF',
};

/** Descrizione estesa di ogni livello di urgenza (form di creazione). */
export const URGENZA_DESCRIZIONE: Record<number, string> = {
  1: 'Minima — può aspettare',
  2: 'Bassa — non urgente',
  3: 'Media — da risolvere presto',
  4: 'Alta — problema grave',
  5: 'Critica — emergenza',
};

export function dataRelativa(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return 'ora';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin} min fa`;
  const diffOre = Math.floor(diffMin / 60);
  if (diffOre < 24) return `${diffOre} ${diffOre === 1 ? 'ora' : 'ore'} fa`;
  const diffGiorni = Math.floor(diffOre / 24);
  if (diffGiorni < 30) return `${diffGiorni} ${diffGiorni === 1 ? 'giorno' : 'giorni'} fa`;
  return new Date(iso).toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric' });
}
