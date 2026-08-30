import { NgStyle } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgStyle],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
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
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.loading = true;

    const { email, password } = this.loginForm.getRawValue();

    this.authService
      .login({ email: email!, password: password! })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          console.log('[LoginComponent] Login riuscito, risposta backend:', response);
          console.log('[LoginComponent] Token salvato in localStorage:', localStorage.getItem('campusreport_token'));
          this.router.navigate(['/map']).then((navigated) => {
            console.log('[LoginComponent] router.navigate(/map) esito:', navigated);
          });
        },
        error: (err: HttpErrorResponse) => {
          console.error('[LoginComponent] Errore login:', err);
          this.errorMessage =
            err.status === 401 || err.status === 403
              ? 'Email o password non corretti.'
              : 'Si è verificato un errore. Riprova più tardi.';
        },
      });
  }
}
