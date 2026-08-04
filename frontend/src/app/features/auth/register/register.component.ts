import { NgStyle } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgStyle],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  registerForm = this.fb.group({
    nome: ['', [Validators.required]],
    cognome: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  showPassword = false;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {}

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.loading = true;

    const { nome, cognome, email, password } = this.registerForm.getRawValue();

    this.authService
      .register({ nome: nome!, cognome: cognome!, email: email!, password: password! })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => this.router.navigate(['/map']),
        error: (err: HttpErrorResponse) => {
          this.errorMessage =
            err.status === 409
              ? 'Esiste già un account con questa email.'
              : err.status === 400
                ? "Verifica i dati inseriti: l'email deve appartenere a un dominio Unical valido."
                : 'Si è verificato un errore. Riprova più tardi.';
        },
      });
  }
}
