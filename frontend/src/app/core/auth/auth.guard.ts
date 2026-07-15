import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// Protege las rutas /admin/**: si no hay una sesion valida, manda a
// sign-in en vez de dejar pasar la navegacion.
//
// BUGFIX (validacion Prioridad 1): esta app tiene SSR habilitado
// (angular.json -> outputMode: "server"), y con SSR una recarga completa
// (F5) de una ruta /admin/** se renderiza primero en el servidor Node
// antes de llegar al navegador. El token vive SOLO en localStorage (no
// hay cookie), y localStorage no existe en el servidor -- asi que
// AuthService.estaAutenticado() siempre daba false ahi, y este guard
// redirigia a sign-in server-side ANTES de que el cliente llegara a leer
// el token real. Resultado: cualquier F5 en una pantalla autenticada
// cerraba la sesion aunque el token siguiera siendo valido.
//
// La correccion: en el servidor no podemos verificar la sesion real (no
// tenemos el token ahi), asi que dejamos pasar la navegacion y delegamos
// la verificacion al cliente una vez hidratado. La seguridad real no
// depende de este guard de todos modos -- cada endpoint de /api/**
// exige el Bearer token valido (ver ResourceServerConfig), asi que un
// usuario sin sesion real simplemente vera peticiones fallar y quedara
// sin datos, sin haber accedido a nada.
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  if (auth.estaAutenticado()) {
    return true;
  }

  router.navigateByUrl('/auth/sign-in');
  return false;
};
