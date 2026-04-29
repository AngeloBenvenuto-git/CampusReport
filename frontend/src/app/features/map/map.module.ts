import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LeafletModule } from '@asymmetrik/ngx-leaflet';
import { SharedModule } from '../../shared/shared.module';

const routes: Routes = [
  { path: '', loadComponent: () => import('./components/map-view/map-view.component').then(c => c.MapViewComponent) },
];

@NgModule({
  imports: [SharedModule, LeafletModule, RouterModule.forChild(routes)],
})
export class MapModule {}
