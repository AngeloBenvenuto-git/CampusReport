import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { NavbarComponent } from './shared/navbar/navbar.component';

const ROTTE_SENZA_NAVBAR = ['/login', '/register'];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  template: `
    <app-navbar *ngIf="mostraNavbar$ | async"></app-navbar>
    <router-outlet></router-outlet>
  `,
})
export class AppComponent {
  mostraNavbar$ = this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    map((event) => this.calcolaMostraNavbar(event.urlAfterRedirects)),
    startWith(this.calcolaMostraNavbar(this.router.url)),
  );

  constructor(private router: Router) {}

  private calcolaMostraNavbar(url: string): boolean {
    return url !== '/' && !ROTTE_SENZA_NAVBAR.some((rotta) => url.startsWith(rotta));
  }
}
