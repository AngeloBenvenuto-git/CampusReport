import { Categoria } from '../models/ticket.models';

export const SPECIALIZZAZIONE_BADGE_CLASS: Record<Categoria, string> = {
  [Categoria.ELETTRICO]: 'bg-yellow-50 text-yellow-700',
  [Categoria.WIFI]: 'bg-blue-50 text-blue-700',
  [Categoria.IDRAULICO]: 'bg-cyan-50 text-cyan-700',
  [Categoria.ATTREZZATURA]: 'bg-purple-50 text-purple-700',
  [Categoria.ALTRO]: 'bg-gray-100 text-gray-600',
};

export function caricoColore(caricoAttuale: number, caricoMassimo: number): string {
  const percentuale = caricoMassimo > 0 ? (caricoAttuale / caricoMassimo) * 100 : 0;
  if (percentuale > 80) return '#EF4444';
  if (percentuale >= 50) return '#F59E0B';
  return '#10B981';
}
