import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { forkJoin } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { TicketService } from '../../../core/services/ticket.service';
import { AssegnaModalComponent } from '../../../shared/components/assegna-modal/assegna-modal.component';
import { TecnicoAdminResponse } from '../../../shared/models/admin.models';
import { Stato, TicketResponse, ZonaResponse } from '../../../shared/models/ticket.models';
import {
  CATEGORIA_LABEL,
  dataRelativa,
  STATO_BADGE_CLASS,
  STATO_COLOR,
  STATO_LABEL,
} from '../../../shared/utils/ticket-display.util';
import { MAPPA_UNICAL_HEIGHT, MAPPA_UNICAL_WIDTH, ZONE_MAP_DEFS, ZonaMapDef } from '../../map/zone-map.data';

/** Stati che contano come "attivi" nella mappa di calore. */
const STATI_ATTIVI: Stato[] = [Stato.APERTA, Stato.ASSEGNATA, Stato.IN_LAVORAZIONE];

/** Soglie della scala di intensità (ticket attivi per zona). */
const LIVELLI_INTENSITA = [
  { label: '0', min: 0, colore: '#E5E7EB' },
  { label: '1-2', min: 1, colore: '#FDE68A' },
  { label: '3-4', min: 3, colore: '#FB923C' },
  { label: '5+', min: 5, colore: '#EF4444' },
];

const COLORE_IN_ATTESA = '#DC2626';

/** Aggregato dei ticket di una zona della planimetria. */
export interface ZonaStat {
  def: ZonaMapDef;
  attivi: number;
  inAttesa: number;
  totale: number;
  tickets: TicketResponse[];
}

/**
 * Mappa panoramica del campus per l'admin: mappa di calore delle zone in base ai ticket
 * attivi, evidenza dei ticket IN_ATTESA e pannello laterale con il dettaglio per zona
 * e l'assegnazione manuale. Non permette di creare segnalazioni.
 */
@Component({
  selector: 'app-admin-mappa',
  standalone: true,
  imports: [CommonModule, AssegnaModalComponent],
  templateUrl: './admin-mappa.component.html',
})
export class AdminMappaComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly STATO_COLOR = STATO_COLOR;
  readonly STATO_BADGE_CLASS = STATO_BADGE_CLASS;
  readonly STATO_LABEL = STATO_LABEL;
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly LIVELLI_INTENSITA = LIVELLI_INTENSITA;
  readonly dataRelativa = dataRelativa;

  @ViewChild('mappa', { static: true }) mappaRef!: ElementRef<HTMLDivElement>;

  caricamento = true;
  errore: string | null = null;

  tickets: TicketResponse[] = [];
  tecnici: TecnicoAdminResponse[] = [];
  zoneBackend: ZonaResponse[] = [];
  statZone: ZonaStat[] = [];

  zonaSelezionata: ZonaStat | null = null;
  ticketDaAssegnare: TicketResponse | null = null;

  private map: L.Map | null = null;
  private zoneLayerGroup: L.LayerGroup | null = null;
  private markerLayerGroup: L.LayerGroup | null = null;
  private resizeObserver: ResizeObserver | null = null;

  constructor(
    private adminService: AdminService,
    private ticketService: TicketService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.caricaDati();
  }

  ngAfterViewInit(): void {
    this.inizializzaMappa();
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }

  // ─── Dati ───────────────────────────────────────────────────────────────

  private caricaDati(): void {
    this.caricamento = true;
    this.errore = null;

    forkJoin({
      tickets: this.adminService.getAllTickets(),
      zone: this.ticketService.getZone(),
      tecnici: this.adminService.getTecnici(),
    }).subscribe({
      next: ({ tickets, zone, tecnici }) => {
        this.tickets = tickets;
        this.zoneBackend = zone;
        this.tecnici = tecnici;
        this.caricamento = false;
        this.aggiornaStatistiche();
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare i dati della mappa. Riprova più tardi.';
      },
    });
  }

  /**
   * Ricalcola gli aggregati per zona (abbinando ticket.zona.nome al nome in ZONE_MAP_DEFS),
   * mantiene la selezione corrente e ridisegna la mappa.
   */
  private aggiornaStatistiche(): void {
    this.statZone = ZONE_MAP_DEFS.map((def) => {
      const ticketZona = this.tickets
        .filter((t) => t.zona?.nome === def.nome)
        .sort((a, b) => {
          const attesaA = a.stato === Stato.IN_ATTESA ? 0 : 1;
          const attesaB = b.stato === Stato.IN_ATTESA ? 0 : 1;
          if (attesaA !== attesaB) return attesaA - attesaB;
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
        });
      return {
        def,
        attivi: ticketZona.filter((t) => STATI_ATTIVI.includes(t.stato)).length,
        inAttesa: ticketZona.filter((t) => t.stato === Stato.IN_ATTESA).length,
        totale: ticketZona.length,
        tickets: ticketZona,
      };
    });

    if (this.zonaSelezionata) {
      const nome = this.zonaSelezionata.def.nome;
      this.zonaSelezionata = this.statZone.find((z) => z.def.nome === nome) ?? null;
    }
    this.disegnaMappa();
  }

  // ─── Statistiche pannello ───────────────────────────────────────────────

  get totaleAttivi(): number {
    return this.statZone.reduce((acc, z) => acc + z.attivi, 0);
  }

  get totaleInAttesa(): number {
    return this.statZone.reduce((acc, z) => acc + z.inAttesa, 0);
  }

  get zoneConAttivi(): number {
    return this.statZone.filter((z) => z.attivi > 0).length;
  }

  /** Zone ordinate per ticket attivi decrescenti (a parità, per ticket in attesa). */
  get zoneOrdinate(): ZonaStat[] {
    return [...this.statZone].sort((a, b) => b.attivi - a.attivi || b.inAttesa - a.inAttesa);
  }

  /** Descrizione della zona selezionata presa dal backend, se presente. */
  get descrizioneZonaSelezionata(): string | null {
    const nome = this.zonaSelezionata?.def.nome;
    return this.zoneBackend.find((z) => z.nome === nome)?.descrizione ?? null;
  }

  /**
   * Colore di riempimento della zona in base al numero di ticket attivi.
   */
  coloreIntensita(attivi: number): string {
    let colore = LIVELLI_INTENSITA[0].colore;
    for (const livello of LIVELLI_INTENSITA) {
      if (attivi >= livello.min) colore = livello.colore;
    }
    return colore;
  }

  nomeTecnico(ticket: TicketResponse): string | null {
    return ticket.tecnico ? `${ticket.tecnico.nome} ${ticket.tecnico.cognome}` : null;
  }

  // ─── Selezione zona ─────────────────────────────────────────────────────

  /**
   * Seleziona una zona: il pannello mostra il dettaglio e la mappa la evidenzia.
   */
  selezionaZona(zona: ZonaStat): void {
    this.zonaSelezionata = zona;
    this.disegnaZone();
  }

  /**
   * Torna alla panoramica di tutte le zone.
   */
  deselezionaZona(): void {
    this.zonaSelezionata = null;
    this.disegnaZone();
  }

  // ─── Azioni ticket ──────────────────────────────────────────────────────

  apriDettaglio(ticket: TicketResponse): void {
    this.router.navigate(['/tickets', ticket.id]);
  }

  apriAssegna(ticket: TicketResponse): void {
    this.ticketDaAssegnare = ticket;
  }

  chiudiAssegna(): void {
    this.ticketDaAssegnare = null;
  }

  /**
   * Aggiorna il ticket assegnato nei dati locali e ridisegna la mappa.
   */
  onTicketAssegnato(aggiornato: TicketResponse): void {
    this.tickets = this.tickets.map((t) => (t.id === aggiornato.id ? aggiornato : t));
    this.ticketDaAssegnare = null;
    this.aggiornaStatistiche();
    // Il carico del tecnico è cambiato: ricarica l'elenco per le assegnazioni successive.
    this.adminService.getTecnici().subscribe({ next: (tecnici) => (this.tecnici = tecnici) });
  }

  // ─── Mappa ──────────────────────────────────────────────────────────────

  private inizializzaMappa(): void {
    const bounds: L.LatLngBoundsExpression = [
      [0, 0],
      [MAPPA_UNICAL_HEIGHT, MAPPA_UNICAL_WIDTH],
    ];
    const mapEl = this.mappaRef.nativeElement;

    this.map = L.map(mapEl, {
      crs: L.CRS.Simple,
      minZoom: -3,
      maxZoom: 2,
      zoomControl: true,
      zoomSnap: 0.1,
      zoomDelta: 0.5,
    });

    L.imageOverlay('assets/mappa-unical.png', bounds).addTo(this.map);

    this.zoneLayerGroup = L.layerGroup().addTo(this.map);
    this.markerLayerGroup = L.layerGroup().addTo(this.map);

    this.disegnaMappa();

    // Aspetta che il DOM sia stabile prima di fitBounds
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
        this.map.fitBounds(bounds, {
          padding: [0, 0],
          animate: false,
          maxZoom: this.map.getBoundsZoom(bounds, true),
        });
      }
    }, 100);

    this.resizeObserver = new ResizeObserver(() => {
      this.map?.invalidateSize();
    });
    this.resizeObserver.observe(mapEl);
  }

  private disegnaMappa(): void {
    this.disegnaZone();
    this.disegnaMarker();
  }

  private disegnaZone(): void {
    if (!this.map || !this.zoneLayerGroup) return;
    this.zoneLayerGroup.clearLayers();

    this.statZone.forEach((zona) => {
      const colore = this.coloreIntensita(zona.attivi);
      const selezionata = this.zonaSelezionata?.def.nome === zona.def.nome;
      const conAttesa = zona.inAttesa > 0;

      const stileBase: L.PathOptions = {
        color: conAttesa ? COLORE_IN_ATTESA : colore,
        fillColor: colore,
        fillOpacity: 0.45,
        weight: selezionata ? 4 : conAttesa ? 3 : 1.5,
        opacity: selezionata || conAttesa ? 1 : 0.8,
      };

      const rect = L.rectangle(zona.def.bounds, stileBase).addTo(this.zoneLayerGroup!);

      rect.bindTooltip(`${zona.def.nome} — ${zona.attivi} attivi, ${zona.inAttesa} in attesa`, {
        direction: 'top',
        sticky: true,
        className: 'leaflet-tooltip-custom',
      });

      rect.on('mouseover', () => rect.setStyle({ fillOpacity: 0.65 }));
      rect.on('mouseout', () => rect.setStyle({ fillOpacity: stileBase.fillOpacity }));
      rect.on('click', () => this.selezionaZona(zona));
    });
  }

  private disegnaMarker(): void {
    if (!this.map || !this.markerLayerGroup) return;
    this.markerLayerGroup.clearLayers();

    this.statZone.forEach((zona) => {
      const colore = this.coloreIntensita(zona.attivi);

      // I marker non sono interattivi: hover e click passano al rettangolo della zona.
      if (zona.attivi > 0) {
        const icon = L.divIcon({
          html: `<div style="
            background: white;
            width: 30px; height: 30px;
            border-radius: 50%;
            border: 3px solid ${colore};
            box-shadow: 0 2px 8px rgba(0,0,0,0.25);
            display: flex; align-items: center; justify-content: center;
            font-size: 12px; color: #1F2937; font-weight: bold;
          ">${zona.attivi}</div>`,
          className: '',
          iconSize: [30, 30],
          iconAnchor: [15, 15],
        });
        L.marker(zona.def.centro, { icon, interactive: false }).addTo(this.markerLayerGroup!);
      }

      if (zona.inAttesa > 0) {
        // Spostato in alto a destra rispetto al centro per non sovrapporsi al marker degli attivi
        const ancora: L.PointExpression = zona.attivi > 0 ? [-8, 30] : [12, 12];
        const icon = L.divIcon({
          html: `<div style="
            background: ${COLORE_IN_ATTESA};
            width: 24px; height: 24px;
            border-radius: 50%;
            border: 2px solid white;
            box-shadow: 0 2px 8px rgba(0,0,0,0.3);
            display: flex; align-items: center; justify-content: center;
            font-size: 11px; color: white; font-weight: bold;
          ">${zona.inAttesa}</div>`,
          className: '',
          iconSize: [24, 24],
          iconAnchor: ancora,
        });
        L.marker(zona.def.centro, { icon, interactive: false }).addTo(this.markerLayerGroup!);
      }
    });
  }
}
