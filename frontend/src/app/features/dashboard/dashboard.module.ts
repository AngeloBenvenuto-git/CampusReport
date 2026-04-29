import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';

const routes: Routes = [
  { path: '', loadComponent: () => import('./components/dashboard/dashboard.component').then(c => c.DashboardComponent) },
];

@NgModule({
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class DashboardModule {}
