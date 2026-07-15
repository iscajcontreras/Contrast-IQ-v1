import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/permiso.guard';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'usuarios',
  },
  {
    path: 'usuarios',
    canActivate: [permisoGuard('ADMINISTRACION', 'VER')],
    loadComponent: () => import('./features/usuarios/usuarios'),
  },
  {
    path: 'roles-permisos',
    canActivate: [permisoGuard('ADMINISTRACION', 'VER')],
    loadComponent: () => import('./features/roles-permisos/roles-permisos'),
  },
];

export default routes;
