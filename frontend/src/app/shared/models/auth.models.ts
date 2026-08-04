export enum Ruolo {
  STUDENTE = 'STUDENTE',
  DOCENTE = 'DOCENTE',
  TECNICO = 'TECNICO',
  ADMIN = 'ADMIN',
}

export interface RegisterRequest {
  nome: string;
  cognome: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  id: string;
  email: string;
  ruolo: Ruolo;
  nome: string;
  cognome: string;
}
