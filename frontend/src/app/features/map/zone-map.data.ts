import * as L from 'leaflet';

export interface ZonaMapDef {
  nome: string;
  bounds: L.LatLngBoundsExpression;
  centro: L.LatLngExpression;
  cubi: string[];
  colore: string;
}

function toBounds(x1: number, y1: number, x2: number, y2: number): L.LatLngBoundsExpression {
  return [
    [y1, x1],
    [y2, x2],
  ];
}

function toCentro(x1: number, y1: number, x2: number, y2: number): L.LatLngExpression {
  return [(y1 + y2) / 2, (x1 + x2) / 2];
}

function zona(nome: string, x1: number, y1: number, x2: number, y2: number, cubi: string[], colore: string): ZonaMapDef {
  return {
    nome,
    bounds: toBounds(x1, y1, x2, y2),
    centro: toCentro(x1, y1, x2, y2),
    cubi,
    colore,
  };
}

export const ZONE_MAP_DEFS: ZonaMapDef[] = [
  zona(
    'Polo Umanistico',
    801,
    816,
    1029,
    670,
    ['0A', '0B', '0C', '1A', '1B', '1C', '1D', '2B', '2C', '3A', '3B', '3C', '3D', '4A', '4B', '4C', '4D', '5B', '5C', '6B', '6C'],
    '#3B82F6',
  ),
  zona(
    'Polo Scientifico Ovest',
    1034,
    816,
    1403,
    670,
    [
      '7-11B', '12B', '12C', '13C', '14B', '14C', '14D', '15B', '15C', '15D', '16C', '17B', '17C', '17D',
      '18C', '18D', '20A', '20B',
    ],
    '#8B5CF6',
  ),
  zona(
    'Biblioteca e Rettorato',
    1408,
    630,
    1649,
    861,
    ['22B', '23B', '23C', '24B', '25B', '25C', 'Rettorato'],
    '#F59E0B',
  ),
  zona(
    'Polo Scientifico Est',
    1655,
    637,
    1938,
    852,
    [
      '26B', '26C', '27B', '27C', '28A', '28B', '28C', '29B', '29C', '30A', '30B', '30C', '30D', '31A',
      '31B', '31C', '31D', '32B', '32C', '33B', '33C',
    ],
    '#10B981',
  ),
  zona(
    'Polo Ingegneria',
    1943,
    637,
    2330,
    894,
    [
      '37B', '38B', '38C', '38D', '38E', '39B', '39C', '40B', '41A', '41B', '41C', '41D', '41Z', '42A',
      '42B', '42C', '42D', '42Z', '44A', '44B', '44C', '44D', '44Z', '45A', '45B', '45C', '45D', '46B', '46C',
    ],
    '#EF4444',
  ),
  zona('Polifunzionale', 650, 454, 1103, 82, ['Polifunzionale', 'SSSAP', 'Anfiteatro', 'Stabulario'], '#EC4899'),
  zona('TAU Cinema Campus', 2336, 647, 2513, 835, ['TAU', 'Cinema Campus'], '#6366F1'),
  zona(
    'PTU e Centro Residenziale',
    907,
    1065,
    1117,
    886,
    ['PTU', 'Centro Residenziale', 'Servizi Didattici', 'Ufficio Disabilità'],
    '#14B8A6',
  ),
  zona('Aula Caldora', 791, 969, 846, 870, ['Aula U. Caldora'], '#F97316'),
  zona('Centro Sportivo', 422, 1247, 621, 854, ['Centro Sportivo', 'CAG', 'Palestra'], '#84CC16'),
  zona(
    'Polo di Innovazione',
    1832,
    580,
    1957,
    467,
    ['Polo Innovazione', 'Polo Tecnologico', 'Progetto Star'],
    '#06B6D4',
  ),
  zona(
    'Centro Congressi e Aula Magna',
    1058,
    875,
    1117,
    827,
    ['Aula Magna', 'Sala A', 'Sala B', 'Sala C'],
    '#D97706',
  ),
];

export const MAPPA_UNICAL_WIDTH = 3000;
export const MAPPA_UNICAL_HEIGHT = 1510;
