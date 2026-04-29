import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

const routes: Routes = [
  { path: '', loadComponent: () => import('./components/ticket-list/ticket-list.component').then(c => c.TicketListComponent) },
  { path: ':id', loadComponent: () => import('./components/ticket-detail/ticket-detail.component').then(c => c.TicketDetailComponent) },
  { path: 'new', loadComponent: () => import('./components/ticket-form/ticket-form.component').then(c => c.TicketFormComponent) },
];

@NgModule({
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class TicketsModule {}
