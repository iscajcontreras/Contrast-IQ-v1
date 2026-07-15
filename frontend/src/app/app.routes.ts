import { Route } from '@angular/router';

// Limpieza de deuda tecnica (Prioridad 4, 14 de julio de 2026): se
// removieron las rutas 'home' (domains/website) y 'coming-soon'
// (domains/coming-soon) -- paginas publicas de marketing del template
// Fuse, sin uso confirmado en el negocio y sin ningun link real hacia
// ellas en el resto de la app.
export const routes: Route[] = [
  // Auth
  {
    path: 'auth',
    loadChildren: () => import('./domains/auth/routes'),
  },

  // Admin
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'admin',
  },
  {
    path: 'admin',
    loadChildren: () => import('./domains/admin/routes'),
  },
];
