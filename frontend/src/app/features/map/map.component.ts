import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { AuthService } from '../../core/services/auth.service';
import { TicketService } from '../../core/services/ticket.service';
import { Stato, TicketResponse, ZonaResponse } from '../../shared/models/ticket.models';
import { CATEGORIA_INIZIALE, dataRelativa, STATO_BADGE_CLASS, STATO_COLOR, STATO_LABEL } from '../../shared/utils/ticket-display.util';
import { MAPPA_UNICAL_HEIGHT, MAPPA_UNICAL_WIDTH, ZONE_MAP_DEFS, ZonaMapDef } from './zone-map.data';
import { TicketModalComponent } from './ticket-modal/ticket-modal.component';

type Tab = 'tutte' | 'attive' | 'completate';

const STATI_ATTIVI: Stato[] = [Stato.APERTA, Stato.ASSEGNATA, Stato.IN_LAVORAZIONE, Stato.IN_ATTESA];

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, TicketModalComponent],
  templateUrl: './map.component.html',
})
export class MapComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly STATO_COLOR = STATO_COLOR;
  readonly STATO_BADGE_CLASS = STATO_BADGE_CLASS;
  readonly STATO_LABEL = STATO_LABEL;
  readonly dataRelativa = dataRelativa;

  caricamento = true;
  errore: string | null = null;

  zoneBackend: ZonaResponse[] = [];
  tickets: TicketResponse[] = [];

  tabAttiva: Tab = 'tutte';

  modalAperto = false;
  zonaSelezionata: ZonaResponse | null = null;

  private map: L.Map | null = null;
  private zoneLayerGroup: L.LayerGroup | null = null;
  private markerLayerGroup: L.LayerGroup | null = null;
  private resizeObserver: ResizeObserver | null = null;

  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.caricaDati();
  }

  ngAfterViewInit(): void {
    this.inizializzaMappa();
  }

  private caricaDati(): void {
    this.caricamento = true;
    this.errore = null;

    this.ticketService.getZone().subscribe({
      next: (zone) => {
        this.zoneBackend = zone;
        this.disegnaZone();
        this.disegnaMarker();
      },
      error: () => {
        this.errore = 'Impossibile caricare le zone del campus.';
      },
    });

    const ruolo = this.authService.getRuolo();
    const tickets$ =
      ruolo === 'TECNICO' ? this.ticketService.getTicketsAssegnati() : this.ticketService.getMyTickets();

    tickets$.subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.caricamento = false;
        this.disegnaMarker();
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare le segnalazioni.';
      },
    });
  }

  private inizializzaMappa(): void {
    const imageWidth = MAPPA_UNICAL_WIDTH;
    const imageHeight = MAPPA_UNICAL_HEIGHT;
    const bounds: L.LatLngBoundsExpression = [
      [0, 0],
      [imageHeight, imageWidth],
    ];

    this.map = L.map('map', {
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

    this.disegnaZone();
    this.disegnaMarker();

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

    const mapEl = document.getElementById('map');
    if (mapEl) {
      this.resizeObserver = new ResizeObserver(() => {
        this.map?.invalidateSize();
      });
      this.resizeObserver.observe(mapEl);
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.map?.remove();
  }

  private disegnaZone(): void {
    if (!this.map || !this.zoneLayerGroup || this.zoneBackend.length === 0) return;
    this.zoneLayerGroup.clearLayers();

    ZONE_MAP_DEFS.forEach((def) => {
      const backendZona = this.trovaZonaBackend(def);

      const rect = L.rectangle(def.bounds, {
        color: def.colore,
        fillColor: def.colore,
        fillOpacity: 0.15,
        weight: 1.5,
        opacity: 0.4,
      }).addTo(this.zoneLayerGroup!);

      rect.on('mouseover', () => {
        rect.setStyle({ fillOpacity: 0.3, opacity: 0.8 });
        rect
          .bindTooltip(def.nome, {
            permanent: false,
            direction: 'top',
            className: 'leaflet-tooltip-custom',
          })
          .openTooltip();
      });

      rect.on('mouseout', () => {
        rect.setStyle({ fillOpacity: 0.15, opacity: 0.4 });
      });

      if (backendZona) {
        rect.on('click', () => this.apriModal(backendZona));
      }
    });
  }

  private disegnaMarker(): void {
    if (!this.map || !this.markerLayerGroup) return;
    this.markerLayerGroup.clearLayers();
    if (this.tickets.length === 0) return;

    this.tickets.forEach((ticket) => {
      const def = ZONE_MAP_DEFS.find((z) => z.nome === ticket.zona?.nome);
      if (!def) return;

      const colore = STATO_COLOR[ticket.stato];
      const iniziale = CATEGORIA_INIZIALE[ticket.categoria];

      const icon = L.divIcon({
        html: `<div style="
          background: ${colore};
          width: 28px; height: 28px;
          border-radius: 50%;
          border: 3px solid white;
          box-shadow: 0 2px 8px rgba(0,0,0,0.3);
          display: flex; align-items: center;
          justify-content: center;
          font-size: 11px; color: white; font-weight: bold;
        ">${iniziale}</div>`,
        className: '',
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      });

      L.marker(def.centro, { icon })
        .addTo(this.markerLayerGroup!)
        .bindPopup(`
          <b>${ticket.titolo}</b><br>
          ${ticket.zona?.nome ?? ''}<br>
          <span style="color:${colore}">${STATO_LABEL[ticket.stato]}</span>
        `);
    });
  }

  private trovaZonaBackend(def: ZonaMapDef): ZonaResponse | null {
    return this.zoneBackend.find((z) => z.nome === def.nome) ?? null;
  }

  // ─── Sidebar ────────────────────────────────────────────────────────────

  get ticketFiltrati(): TicketResponse[] {
    switch (this.tabAttiva) {
      case 'attive':
        return this.tickets.filter((t) => STATI_ATTIVI.includes(t.stato));
      case 'completate':
        return this.tickets.filter((t) => t.stato === Stato.COMPLETATA || t.stato === Stato.RIFIUTATA);
      default:
        return this.tickets;
    }
  }

  get conteggioAperte(): number {
    return this.tickets.filter((t) => t.stato === Stato.APERTA || t.stato === Stato.ASSEGNATA).length;
  }

  get conteggioInLavorazione(): number {
    return this.tickets.filter((t) => t.stato === Stato.IN_LAVORAZIONE).length;
  }

  get conteggioCompletate(): number {
    return this.tickets.filter((t) => t.stato === Stato.COMPLETATA).length;
  }

  selezionaTab(tab: Tab): void {
    this.tabAttiva = tab;
  }

  apriDettaglio(ticket: TicketResponse): void {
    this.router.navigate(['/tickets', ticket.id]);
  }

  // ─── Modal ──────────────────────────────────────────────────────────────

  apriModal(zona: ZonaResponse | null): void {
    this.zonaSelezionata = zona;
    this.modalAperto = true;
  }

  chiudiModal(): void {
    this.modalAperto = false;
    this.zonaSelezionata = null;
  }

  onTicketCreato(ticket: TicketResponse): void {
    this.tickets = [ticket, ...this.tickets];
    this.disegnaMarker();
    this.chiudiModal();
  }
}
