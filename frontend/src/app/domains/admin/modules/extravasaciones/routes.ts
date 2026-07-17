import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'alertas',
  },
  {
    path: 'alertas',
    loadComponent: () => import('./features/alertas/alertas'),
  },
];

export default routes;
