import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { ConfigPesiResponse } from '../../../shared/models/admin.models';

@Component({
  selector: 'app-admin-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-config.component.html',
})
export class AdminConfigComponent implements OnInit {
  caricamento = true;
  errore: string | null = null;
  messaggioSuccesso: string | null = null;

  alpha = 0.5;
  beta = 0.5;

  salvando = false;
  configAttuale: ConfigPesiResponse | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getPesi().subscribe({
      next: (pesi) => {
        this.alpha = pesi.alpha;
        this.beta = pesi.beta;
        this.configAttuale = pesi;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare la configurazione.';
      },
    });
  }

  get somma(): number {
    return Math.round((this.alpha + this.beta) * 100) / 100;
  }

  get configurazioneValida(): boolean {
    return this.somma === 1;
  }

  onAlphaChange(valore: number): void {
    this.alpha = valore;
    this.beta = Math.round((1 - valore) * 100) / 100;
  }

  salvaConfigurazione(): void {
    if (!this.configurazioneValida) return;

    this.salvando = true;
    this.errore = null;
    this.messaggioSuccesso = null;

    this.adminService.aggiornaPesi(this.alpha, this.beta).subscribe({
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
