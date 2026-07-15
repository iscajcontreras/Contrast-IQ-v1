import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/dashboard-paciente/dashboard-paciente'),
  },
  {
    path: 'inyeccion/:id',
    loadComponent: () => import('./features/inyeccion-detalle/inyeccion-detalle'),
  },
];

export default routes;
