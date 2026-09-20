import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ZonaMapDef } from '../zone-map.data';

/**
 * Modal intermedio: mostra i cubi della zona cliccata sulla mappa,
 * permette di selezionarne uno prima di aprire il form di segnalazione.
 */
@Component({
  selector: 'app-cubo-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cubo-selector.component.html',
})
export class CuboSelectorComponent {
  @Input() zona: ZonaMapDef | null = null;
  @Output() cuboSelezionato = new EventEmitter<string>();
  @Output() annulla = new EventEmitter<void>();

  onSeleziona(cubo: string): void {
    this.cuboSelezionato.emit(cubo);
  }

  onSalta(): void {
    this.cuboSelezionato.emit('');
  }

  onClose(): void {
    this.annulla.emit();
  }
}
