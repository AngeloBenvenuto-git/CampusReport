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
