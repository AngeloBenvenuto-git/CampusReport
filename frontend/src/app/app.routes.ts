import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LandingComponent } from './features/landing/landing.component';
import { MapComponent } from './features/map/map.component';
import { TicketDetailComponent } from './features/tickets/ticket-detail/ticket-detail.component';
import { TicketListComponent } from './features/tickets/ticket-list/ticket-list.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'map',
    component: MapComponent,
    canActivate: [authGuard, roleGuard],
    data: { ruoli: ['STUDENTE', 'DOCENTE', 'TECNICO', 'ADMIN'] },
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { ruoli: ['TECNICO'] },
  },
  {
    path: 'tickets',
    component: TicketListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'tickets/:id',
    component: TicketDetailComponent,
    canActivate: [authGuard],
  },
  { path: '**', redirectTo: '/login' },
];
