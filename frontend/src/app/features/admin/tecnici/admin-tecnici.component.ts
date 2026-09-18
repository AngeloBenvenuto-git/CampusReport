import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { TecnicoAdminResponse } from '../../../shared/models/admin.models';
import { Categoria } from '../../../shared/models/ticket.models';
import { CATEGORIA_LABEL } from '../../../shared/utils/ticket-display.util';
import { caricoColore, SPECIALIZZAZIONE_BADGE_CLASS } from '../../../shared/utils/admin-display.util';

@Component({
  selector: 'app-admin-tecnici',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-tecnici.component.html',
})
export class AdminTecniciComponent implements OnInit {
  readonly categorie = Object.values(Categoria);
  readonly CATEGORIA_LABEL = CATEGORIA_LABEL;
  readonly SPECIALIZZAZIONE_BADGE_CLASS = SPECIALIZZAZIONE_BADGE_CLASS;
  readonly caricoColore = caricoColore;

  caricamento = true;
  errore: string | null = null;
  messaggioSuccesso: string | null = null;

  tecnici: TecnicoAdminResponse[] = [];
  loadingStato: Record<string, boolean> = {};

  // ─── Modal nuovo tecnico ────────────────────────────────────────────────

  modalNuovoAperto = false;
  inviandoNuovo = false;
  erroreNuovo: string | null = null;
  specializzazioniNuovo: Categoria[] = [];

  formNuovo = this.fb.group({
    nome: ['', Validators.required],
    cognome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    zona: ['', Validators.required],
    caricoMassimo: [10, [Validators.required, Validators.min(1)]],
  });

  // ─── Modal modifica tecnico ─────────────────────────────────────────────

  modalModificaAperto = false;
  inviandoModifica = false;
  erroreModifica: string | null = null;
  tecnicoInModifica: TecnicoAdminResponse | null = null;
  specializzazioniModifica: Categoria[] = [];

  formModifica = this.fb.group({
    zona: ['', Validators.required],
    caricoMassimo: [10, [Validators.required, Validators.min(1)]],
  });

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
  ) {}

  ngOnInit(): void {
    this.caricaTecnici();
  }

  private caricaTecnici(): void {
    this.caricamento = true;
    this.adminService.getTecnici().subscribe({
      next: (tecnici) => {
        this.tecnici = tecnici;
        this.caricamento = false;
      },
      error: () => {
        this.caricamento = false;
        this.errore = 'Impossibile caricare i tecnici.';
      },
    });
  }

  iniziali(tecnico: TecnicoAdminResponse): string {
    return `${tecnico.nome.charAt(0)}${tecnico.cognome.charAt(0)}`.toUpperCase();
  }

  percentualeCarico(tecnico: TecnicoAdminResponse): number {
    return tecnico.caricoMassimo > 0 ? Math.min(100, (tecnico.caricoAttuale / tecnico.caricoMassimo) * 100) : 0;
  }

  // ─── Attiva / disattiva ─────────────────────────────────────────────────

  cambiaStato(tecnico: TecnicoAdminResponse): void {
    this.loadingStato[tecnico.id] = true;
    this.adminService.cambioStatoTecnico(tecnico.id, !tecnico.attivo).subscribe({
      next: (aggiornato) => {
        this.aggiornaTecnicoLocale(aggiornato);
        this.loadingStato[tecnico.id] = false;
      },
      error: () => {
        this.loadingStato[tecnico.id] = false;
        this.errore = 'Impossibile aggiornare lo stato del tecnico.';
      },
    });
  }

  private aggiornaTecnicoLocale(aggiornato: TecnicoAdminResponse): void {
    const idx = this.tecnici.findIndex((t) => t.id === aggiornato.id);
    if (idx !== -1) this.tecnici[idx] = aggiornato;
  }

  // ─── Modal nuovo tecnico ────────────────────────────────────────────────

  apriModalNuovo(): void {
    this.formNuovo.reset({ caricoMassimo: 10 });
    this.specializzazioniNuovo = [];
    this.erroreNuovo = null;
    this.modalNuovoAperto = true;
  }

  chiudiModalNuovo(): void {
    this.modalNuovoAperto = false;
  }

  toggleSpecializzazioneNuovo(categoria: Categoria): void {
    const idx = this.specializzazioniNuovo.indexOf(categoria);
    if (idx === -1) {
      this.specializzazioniNuovo.push(categoria);
    } else {
      this.specializzazioniNuovo.splice(idx, 1);
    }
  }

  isSpecializzazioneSelezionataNuovo(categoria: Categoria): boolean {
    return this.specializzazioniNuovo.includes(categoria);
  }

  confermaNuovo(): void {
    if (this.formNuovo.invalid || this.specializzazioniNuovo.length === 0) {
      this.formNuovo.markAllAsTouched();
      if (this.specializzazioniNuovo.length === 0) {
        this.erroreNuovo = 'Seleziona almeno una specializzazione.';
      }
      return;
    }

    const valori = this.formNuovo.getRawValue();
    this.inviandoNuovo = true;
    this.erroreNuovo = null;

    this.adminService
      .creaTecnico({
        nome: valori.nome!,
        cognome: valori.cognome!,
        email: valori.email!,
        zona: valori.zona!,
        caricoMassimo: valori.caricoMassimo!,
        specializzazioni: this.specializzazioniNuovo,
      })
      .subscribe({
        next: (nuovo) => {
          this.tecnici = [...this.tecnici, nuovo];
          this.inviandoNuovo = false;
          this.modalNuovoAperto = false;
          this.mostraSuccesso('Tecnico creato, email inviata');
        },
        error: (err: HttpErrorResponse) => {
          this.inviandoNuovo = false;
          this.erroreNuovo = err.status === 409 ? 'Esiste già un account con questa email.' : 'Impossibile creare il tecnico.';
        },
      });
  }

  // ─── Modal modifica tecnico ─────────────────────────────────────────────

  apriModalModifica(tecnico: TecnicoAdminResponse): void {
    this.tecnicoInModifica = tecnico;
    this.specializzazioniModifica = [...tecnico.specializzazioni];
    this.formModifica.reset({ zona: tecnico.zona, caricoMassimo: tecnico.caricoMassimo });
    this.erroreModifica = null;
    this.modalModificaAperto = true;
  }

  chiudiModalModifica(): void {
    this.modalModificaAperto = false;
    this.tecnicoInModifica = null;
  }

  toggleSpecializzazioneModifica(categoria: Categoria): void {
    const idx = this.specializzazioniModifica.indexOf(categoria);
    if (idx === -1) {
      this.specializzazioniModifica.push(categoria);
    } else {
      this.specializzazioniModifica.splice(idx, 1);
    }
  }

  isSpecializzazioneSelezionataModifica(categoria: Categoria): boolean {
    return this.specializzazioniModifica.includes(categoria);
  }

  confermaModifica(): void {
    if (!this.tecnicoInModifica || this.formModifica.invalid || this.specializzazioniModifica.length === 0) {
      this.formModifica.markAllAsTouched();
      if (this.specializzazioniModifica.length === 0) {
        this.erroreModifica = 'Seleziona almeno una specializzazione.';
      }
      return;
    }

    const valori = this.formModifica.getRawValue();
    const id = this.tecnicoInModifica.id;
    this.inviandoModifica = true;
    this.erroreModifica = null;

    this.adminService
      .modificaTecnico(id, {
        zona: valori.zona!,
        caricoMassimo: valori.caricoMassimo!,
        specializzazioni: this.specializzazioniModifica,
      })
      .subscribe({
        next: (aggiornato) => {
          this.aggiornaTecnicoLocale(aggiornato);
          this.inviandoModifica = false;
          this.modalModificaAperto = false;
          this.tecnicoInModifica = null;
          this.mostraSuccesso('Modifiche salvate');
        },
        error: () => {
          this.inviandoModifica = false;
          this.erroreModifica = 'Impossibile salvare le modifiche.';
        },
      });
  }

  private mostraSuccesso(messaggio: string): void {
    this.messaggioSuccesso = messaggio;
    setTimeout(() => (this.messaggioSuccesso = null), 4000);
  }
}
