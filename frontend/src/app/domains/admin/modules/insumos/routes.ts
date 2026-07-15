import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'lotes',
  },
  {
    path: 'lotes',
    loadComponent: () => import('./features/lotes/lotes'),
  },
  {
    path: 'pedidos',
    loadComponent: () => import('./features/pedidos/pedidos'),
  },
];

export default routes;
