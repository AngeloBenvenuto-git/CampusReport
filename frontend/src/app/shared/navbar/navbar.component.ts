import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html',
})
export class NavbarComponent {
  menuAperto = false;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get utente() {
    return this.authService.getCurrentUser();
  }

  get iniziali(): string {
    const utente = this.utente;
    if (!utente) {
      return '';
    }
    return `${utente.nome.charAt(0)}${utente.cognome.charAt(0)}`.toUpperCase();
  }

  toggleMenu(): void {
    this.menuAperto = !this.menuAperto;
  }

  chiudiMenu(): void {
    this.menuAperto = false;
  }

  logout(): void {
    this.chiudiMenu();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
