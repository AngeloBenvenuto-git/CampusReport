import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { ZONE_MAP_DEFS, ZonaMapDef } from '../zone-map.data';

/**
 * Modal intermedio aperto dal bottone "+ Nuova": permette di scegliere
 * la zona del campus prima di passare al selettore cubo.
 */
@Component({
  selector: 'app-zona-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './zona-selector.component.html',
})
export class ZonaSelectorComponent {
  @Output() zonaSelezionata = new EventEmitter<ZonaMapDef>();
  @Output() annulla = new EventEmitter<void>();

  readonly zone = ZONE_MAP_DEFS;

  onSeleziona(zona: ZonaMapDef): void {
    this.zonaSelezionata.emit(zona);
  }

  onClose(): void {
    this.annulla.emit();
  }
}
