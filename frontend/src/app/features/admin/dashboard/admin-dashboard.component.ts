import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStatisticheResponse, TecnicoPerformance } from '../../../shared/models/admin.models';
import { Stato } from '../../../shared/models/ticket.models';
import { STATO_COLOR, STATO_LABEL } from '../../../shared/utils/ticket-display.util';

interface SegmentoDonut {
  stato: Stato;
  label: string;
  colore: string;
  conteggio: number;
  dashArray: string;
  dashOffset: number;
}

interface PuntoGrafico {
  x: number;
  y: number;
  label: string;
  count: number;
}

const DONUT_RADIUS = 40;
const DONUT_CIRCONFERENZA = 2 * Math.PI * DONUT_RADIUS;

const CHART_WIDTH = 460;
const CHART_HEIGHT = 180;
const CHART_PADDING_X = 20;
const CHART_PADDING_TOP = 15;
const CHART_PADDING_BOTTOM = 30;

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  readonly oggi = new Date();
  readonly stati = Object.values(Stato);
  readonly STATO_LABEL = STATO_LABEL;

  readonly donutRaggio = DONUT_RADIUS;
  readonly chartWidth = CHART_WIDTH;
  readonly chartHeight = CHART_HEIGHT;

  caricamento = true;
  errore: string | null = null;
  stats: AdminStatisticheResponse | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getStatistiche().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare le statistiche.';
      },
    });
  }

  // ─── Grafico donut: distribuzione per stato ────────────────────────────

  get donutSegmenti(): SegmentoDonut[] {
    if (!this.stats) return [];
    const perStato = this.stats.ticketPerStato;
    const totale = this.stati.reduce((acc, s) => acc + (perStato[s] ?? 0), 0) || 1;

    let cumulato = 0;
    return this.stati
      .map((stato) => {
        const conteggio = perStato[stato] ?? 0;
        const lunghezza = (conteggio / totale) * DONUT_CIRCONFERENZA;
        const dashArray = `${lunghezza} ${DONUT_CIRCONFERENZA}`;
        const dashOffset = -cumulato;
        cumulato += lunghezza;
        return {
          stato,
          label: STATO_LABEL[stato],
          colore: STATO_COLOR[stato],
          conteggio,
          dashArray,
          dashOffset,
        };
      })
      .filter((s) => s.conteggio > 0);
  }

  get donutTotale(): number {
    return this.donutSegmenti.reduce((acc, s) => acc + s.conteggio, 0);
  }

  // ─── Grafico andamento settimanale ──────────────────────────────────────

  private get puntiSettimana(): PuntoGrafico[] {
    const dati = this.stats?.ticketPerSettimana ?? [];
    if (dati.length === 0) return [];

    const massimo = Math.max(1, ...dati.map((d) => d.count));
    const larghezzaUtile = CHART_WIDTH - CHART_PADDING_X * 2;
    const altezzaUtile = CHART_HEIGHT - CHART_PADDING_TOP - CHART_PADDING_BOTTOM;
    const step = dati.length > 1 ? larghezzaUtile / (dati.length - 1) : 0;

    return dati.map((d, i) => ({
      x: CHART_PADDING_X + step * i,
      y: CHART_PADDING_TOP + altezzaUtile * (1 - d.count / massimo),
      label: d.settimana,
      count: d.count,
    }));
  }

  get weeklyMassimo(): number {
    const dati = this.stats?.ticketPerSettimana ?? [];
    return Math.max(1, ...dati.map((d) => d.count));
  }

  get weeklyPoints(): PuntoGrafico[] {
    return this.puntiSettimana;
  }

  get weeklyPolyline(): string {
    return this.puntiSettimana.map((p) => `${p.x},${p.y}`).join(' ');
  }

  get weeklyAreaPath(): string {
    const punti = this.puntiSettimana;
    if (punti.length === 0) return '';
    const baseline = CHART_HEIGHT - CHART_PADDING_BOTTOM;
    const primo = punti[0];
    const ultimo = punti[punti.length - 1];
    const linea = punti.map((p) => `L ${p.x} ${p.y}`).join(' ');
    return `M ${primo.x} ${baseline} ${linea} L ${ultimo.x} ${baseline} Z`;
  }

  get weeklyBaseline(): number {
    return CHART_HEIGHT - CHART_PADDING_BOTTOM;
  }

  // ─── Performance tecnici ────────────────────────────────────────────────

  get performanceOrdinata(): TecnicoPerformance[] {
    return [...(this.stats?.performanceTecnici ?? [])].sort((a, b) => b.completate - a.completate);
  }

  get performanceMassimo(): number {
    return Math.max(1, ...this.performanceOrdinata.map((p) => p.completate));
  }

  percentualeBarraPerformance(completate: number): number {
    return (completate / this.performanceMassimo) * 100;
  }
}
