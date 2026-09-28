import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AttivaAccountRequest,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  VerificaTokenResponse,
} from '../../shared/models/auth.models';

const TOKEN_KEY = 'campusreport_token';
const USER_KEY = 'campusreport_user';

/**
 * Gestisce autenticazione, sessione JWT e persistenza dell'utente corrente in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.salvaSessione(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, request)
      .pipe(tap((response) => this.salvaSessione(response)));
  }

  /**
   * Verifica se un token di attivazione è ancora utilizzabile, prima di mostrare il form.
   */
  verificaToken(token: string): Observable<VerificaTokenResponse> {
    return this.http.get<VerificaTokenResponse>(`${this.apiUrl}/verifica-token`, { params: { token } });
  }

  /**
   * Attiva l'account di un tecnico impostando la password e avvia la sessione con il JWT restituito.
   */
  attivaAccount(request: AttivaAccountRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/attiva`, request)
      .pipe(tap((response) => this.salvaSessione(response)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getCurrentUser(): AuthResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  }

  getRuolo(): string | null {
    return this.getCurrentUser()?.ruolo ?? null;
  }

  private salvaSessione(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(response));
  }
}
