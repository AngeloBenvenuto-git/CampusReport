import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { ConfigPesiResponse } from '../../../shared/models/admin.models';

type NomePeso = 'alpha' | 'beta' | 'gamma' | 'delta';

interface PesoDef {
  chiave: NomePeso;
  simbolo: string;
  nome: string;
  descrizione: string;
}

const PESI: PesoDef[] = [
  { chiave: 'alpha', simbolo: 'α', nome: 'Alpha', descrizione: 'Peso specializzazione tecnico' },
  { chiave: 'beta', simbolo: 'β', nome: 'Beta', descrizione: 'Peso carico di lavoro' },
  { chiave: 'gamma', simbolo: 'γ', nome: 'Gamma', descrizione: 'Peso urgenza segnalazione' },
  { chiave: 'delta', simbolo: 'δ', nome: 'Delta', descrizione: 'Peso vicinanza zona tecnico' },
];

@Component({
  selector: 'app-admin-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-config.component.html',
})
export class AdminConfigComponent implements OnInit {
  readonly pesiDef = PESI;

  caricamento = true;
  errore: string | null = null;
  messaggioSuccesso: string | null = null;

  pesi: ConfigPesiResponse = { alpha: 0.5, beta: 0.3, gamma: 0.1, delta: 0.1 };

  salvando = false;
  configAttuale: ConfigPesiResponse | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getPesi().subscribe({
      next: (pesi) => {
        this.pesi = { ...pesi };
        this.configAttuale = pesi;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare la configurazione.';
      },
    });
  }

  /** Somma α + β + γ + δ arrotondata a due decimali. */
  get somma(): number {
    const { alpha, beta, gamma, delta } = this.pesi;
    return Math.round((alpha + beta + gamma + delta) * 100) / 100;
  }

  get configurazioneValida(): boolean {
    return this.somma === 1 && PESI.every((p) => this.pesi[p.chiave] >= 0 && this.pesi[p.chiave] <= 1);
  }

  /** Aggiorna un singolo peso normalizzando l'input numerico a due decimali. */
  onPesoChange(chiave: NomePeso, valore: number | string): void {
    const numero = Number(valore);
    this.pesi = { ...this.pesi, [chiave]: Number.isFinite(numero) ? Math.round(numero * 100) / 100 : 0 };
  }

  salvaConfigurazione(): void {
    if (!this.configurazioneValida) return;

    this.salvando = true;
    this.errore = null;
    this.messaggioSuccesso = null;

    this.adminService.aggiornaPesi(this.pesi).subscribe({
      next: (pesi) => {
        this.configAttuale = pesi;
        this.salvando = false;
        this.messaggioSuccesso = 'Configurazione aggiornata con successo';
        setTimeout(() => (this.messaggioSuccesso = null), 4000);
      },
      error: () => {
        this.salvando = false;
        this.errore = 'Impossibile salvare la configurazione.';
      },
    });
  }
}
