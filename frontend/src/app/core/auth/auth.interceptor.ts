import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '@/environments/environment';
import { AuthService } from './auth.service';

// Adjunta "Authorization: Bearer <access_token>" a las llamadas dirigidas
// al backend (environment.apiBaseUrl). Si el backend responde 401 (token
// vencido), intenta un refresh_token una sola vez y reintenta la llamada
// original; si el refresh tambien falla, manda al usuario a /auth/sign-in.
//
// IMPORTANTE: nunca se adjunta el header a las rutas publicas de auth
// (login/registro/refresh/etc.) aunque haya un token guardado. Si ese
// token quedo invalido (ej. cambio la llave de firma del backend, o
// simplemente vencio), Spring Security rechaza la peticion con 401 en
// cuanto ve un Bearer token invalido en el header -- ANTES de revisar si
// la ruta es publica. Sin esta exclusion, un token viejo en localStorage
// puede bloquear hasta el login mismo, de forma muy confusa de diagnosticar.
const RUTAS_PUBLICAS_AUTH = [
  '/auth/login',
  '/auth/refresh',
  '/auth/logout',
  '/auth/registro',
  '/auth/olvidar-password',
  '/auth/restablecer-password',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  const esLlamadaAlBackend = req.url.startsWith(environment.apiBaseUrl);
  const esRutaPublica = RUTAS_PUBLICAS_AUTH.some((ruta) => req.url.includes(ruta));
  const token = auth.obtenerAccessToken();

  const request = esLlamadaAlBackend && token && !esRutaPublica
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && esLlamadaAlBackend && !esRutaPublica) {
        // BUGFIX (validacion Prioridad 1): en el servidor (SSR) nunca hay
        // token (localStorage no existe ahi -- ver AuthService), asi que
        // CUALQUIER llamada al backend durante el render inicial sale sin
        // Authorization y el backend responde 401 legitimamente. Antes,
        // este bloque redirigia a /auth/sign-in tambien en el servidor
        // (auth.refrescarToken() ya regresa false de inmediato ahi),
        // pisando por completo al fix de auth.guard.ts: aunque el guard
        // dejara pasar la navegacion, el primer request fallido durante
        // el mismo render mandaba al usuario a sign-in de todos modos.
        // En el servidor simplemente dejamos que el error siga su curso
        // sin navegar -- el cliente, ya hidratado con el token real,
        // reintentara con sus propias llamadas.
        if (!esNavegador) {
          return throwError(() => error);
        }

        return from(auth.refrescarToken()).pipe(
          switchMap((exito) => {
            if (!exito) {
              router.navigateByUrl('/auth/sign-in');
              return throwError(() => error);
            }
            const nuevoToken = auth.obtenerAccessToken();
            const reintento = req.clone({ setHeaders: { Authorization: `Bearer ${nuevoToken}` } });
            return next(reintento);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
