import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/reportes/reportes'),
  },
];

export default routes;
