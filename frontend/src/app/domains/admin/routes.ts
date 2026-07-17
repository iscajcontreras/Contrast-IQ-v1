import { Routes } from '@angular/router';
import { AdminLayout } from './layout/layout';
import { authGuard } from '@/app/core/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    children: [
      // Si ya pasaste el authGuard de arriba (o sea que SI tienes sesion
      // valida), '/admin' a secas debe llevarte al dashboard por
      // defecto -- no de vuelta a sign-in. Redirigir aqui a sign-in
      // (como estaba antes) provocaba un "el login no da error pero
      // tampoco me lleva a ningun lado": si autenticabas correctamente
      // y el navegador te mandaba a '/admin', esta ruta te regresaba
      // sola a la pantalla de login sin ningun mensaje de error.
      { path: '', pathMatch: 'full', redirectTo: 'dashboards/contrast-injector' },

      // -----------------------------------------------------------------------
      // Dashboards
      // -----------------------------------------------------------------------
      {
        path: 'dashboards',
        loadChildren: () => import('./modules/dashboards/routes'),
      },

      // -----------------------------------------------------------------------
      // Paciente (dashboard independiente)
      // -----------------------------------------------------------------------
      {
        path: 'paciente',
        loadChildren: () => import('./modules/pacientes/routes'),
      },

      // -----------------------------------------------------------------------
      // Administracion
      // -----------------------------------------------------------------------
      {
        path: 'administracion',
        loadChildren: () => import('./modules/administracion/routes'),
      },

      // -----------------------------------------------------------------------
      // Insumos
      // -----------------------------------------------------------------------
      {
        path: 'insumos',
        loadChildren: () => import('./modules/insumos/routes'),
      },

      // -----------------------------------------------------------------------
      // Mantenimiento predictivo
      // -----------------------------------------------------------------------
      {
        path: 'mantenimiento',
        loadChildren: () => import('./modules/mantenimiento/routes'),
      },

      // -----------------------------------------------------------------------
      // Extravasaciones (alertas EDA) -- julio 2026: modulo nuevo, a donde
      // ahora redirige la tarjeta "Alertas de extravasacion" del dashboard
      // de Inyecciones de contraste.
      // -----------------------------------------------------------------------
      {
        path: 'extravasaciones',
        loadChildren: () => import('./modules/extravasaciones/routes'),
      },

      // -----------------------------------------------------------------------
      // Reportes ejecutivos
      // -----------------------------------------------------------------------
      {
        path: 'reportes',
        loadChildren: () => import('./modules/reportes/routes'),
      },

      // -----------------------------------------------------------------------
      // Integracion clinica
      // -----------------------------------------------------------------------
      {
        path: 'integracion-clinica',
        loadChildren: () => import('./modules/integracion-clinica/routes'),
      },

      // -----------------------------------------------------------------------
      // Extras
      // -----------------------------------------------------------------------
      // Limpieza de deuda tecnica (Prioridad 4, 14 de julio de 2026): se
      // removieron las rutas/modulos de ejemplo del template Fuse sin uso
      // real en el negocio (academy, contacts, file-manager, help-center,
      // tasks, settings, notifications, documentation). `error` se
      // conserva porque alimenta la pagina 404 real de la app.
      {
        path: 'error',
        loadChildren: () => import('./modules/extras/error/routes'),
      },

      // 404
      {
        path: '404',
        pathMatch: 'full',
        loadComponent: () =>
          import('./modules/extras/error/features/error-404'),
      },

      // Catch all
      { path: '**', redirectTo: '404' },
    ],
  },
];

export default routes;
