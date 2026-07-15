import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/mantenimiento/mantenimiento'),
  },
];

export default routes;
