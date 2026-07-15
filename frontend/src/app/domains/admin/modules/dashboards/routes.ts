import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'contrast-injector',
  },
  {
    path: 'contrast-injector',
    loadComponent: () => import('./features/contrast-injector/contrast-injector'),
  },
];

export default routes;
