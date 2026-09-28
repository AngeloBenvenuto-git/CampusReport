import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

enum StatoAttivazione {
  CARICAMENTO,
  TOKEN_NON_VALIDO,
  FORM,
  COMPLETATO,
}

type ForzaPassword = 'debole' | 'media' | 'forte';

const REDIRECT_DELAY_MS = 3000;

/**
 * Pagina pubblica di attivazione dell'account tecnico, raggiunta dal link nell'email di invito.
 * Verifica il token, mostra il form per impostare la password e, ad attivazione riuscita,
 * avvia la sessione e reindirizza alla dashboard.
 */
@Component({
  selector: 'app-attiva-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './attiva-account.component.html',
  styles: [
    `
      .progress-redirect {
        width: 0;
        animation-name: riempi;
        animation-timing-function: linear;
        animation-fill-mode: forwards;
      }
      @keyframes riempi {
        to {
          width: 100%;
        }
      }
    `,
  ],
})
export class AttivaAccountComponent implements OnInit, OnDestroy {
  readonly StatoAttivazione = StatoAttivazione;
  readonly redirectDelayMs = REDIRECT_DELAY_MS;

  stato = StatoAttivazione.CARICAMENTO;

  token = '';
  datiUtente: { nome: string; cognome: string; email: string } | null = null;
  motivoErrore = '';

  form = this.fb.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confermaPassword: ['', Validators.required],
    },
    { validators: AttivaAccountComponent.passwordMatchValidator },
  );

  loading = false;
  erroreAttivazione: string | null = null;
  showPassword = false;
  showConferma = false;

  private redirectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.motivoErrore = 'Nessun token specificato';
      this.stato = StatoAttivazione.TOKEN_NON_VALIDO;
      return;
    }

    this.authService.verificaToken(this.token).subscribe({
      next: (result) => {
        if (result.valido) {
          this.datiUtente = { nome: result.nome!, cognome: result.cognome!, email: result.email! };
          this.stato = StatoAttivazione.FORM;
        } else {
          this.motivoErrore = result.motivo ?? 'Token non valido';
          this.stato = StatoAttivazione.TOKEN_NON_VALIDO;
        }
      },
      error: () => {
        this.motivoErrore = 'Errore durante la verifica del link';
        this.stato = StatoAttivazione.TOKEN_NON_VALIDO;
      },
    });
  }

  ngOnDestroy(): void {
    if (this.redirectTimer) clearTimeout(this.redirectTimer);
  }

  /**
   * Validatore di gruppo: segnala `passwordMismatch` se password e conferma differiscono.
   */
  static passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const conferma = group.get('confermaPassword')?.value;
    return password === conferma ? null : { passwordMismatch: true };
  }

  /** True se la conferma è stata toccata e non coincide con la password. */
  get passwordNonCoincidono(): boolean {
    const conferma = this.form.get('confermaPassword');
    return this.form.hasError('passwordMismatch') && (conferma?.touched === true || conferma?.dirty === true);
  }

  /** Forza della password: debole (< 8 caratteri), media (>= 8), forte (>= 8 con maiuscola e numero). */
  get forzaPassword(): ForzaPassword | null {
    const password = this.form.get('password')?.value ?? '';
    if (!password) return null;
    if (password.length < 8) return 'debole';
    return /[A-Z]/.test(password) && /\d/.test(password) ? 'forte' : 'media';
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.erroreAttivazione = null;
    const { password, confermaPassword } = this.form.getRawValue();

    this.authService
      .attivaAccount({ token: this.token, password: password!, confermaPassword: confermaPassword! })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.stato = StatoAttivazione.COMPLETATO;
          this.redirectTimer = setTimeout(() => this.router.navigate(['/dashboard']), REDIRECT_DELAY_MS);
        },
        error: (err: HttpErrorResponse) => {
          this.erroreAttivazione = err.error?.message ?? "Errore durante l'attivazione. Riprova.";
        },
      });
  }
}
