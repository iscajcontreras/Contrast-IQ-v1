import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/integracion-clinica/integracion-clinica'),
  },
];

export default routes;
