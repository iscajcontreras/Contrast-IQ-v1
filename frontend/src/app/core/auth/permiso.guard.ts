import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, of } from 'rxjs';
import { PermisosService } from '@/app/core/auth/permisos.service';

// Protege rutas individuales por permiso de modulo (ademas de authGuard,
// que solo verifica que haya sesion). Redirige a la pantalla por defecto
// de ContrastIQ si el usuario no tiene el permiso -- no a /auth/sign-in,
// porque el usuario SI esta autenticado, solo no tiene acceso a ese
// modulo.
export function permisoGuard(moduloCodigo: string, permisoCodigo = 'VER'): CanActivateFn {
  return () => {
    const permisos = inject(PermisosService);
    const router = inject(Router);
    const platformId = inject(PLATFORM_ID);

    if (!isPlatformBrowser(platformId)) {
      return of(true);
    }

    return permisos.asegurarCargado().pipe(
      map(() => {
        if (permisos.tienePermisoSync(moduloCodigo, permisoCodigo)) {
          return true;
        }
        router.navigateByUrl('/admin/dashboards/contrast-injector');
        return false;
      })
    );
  };
}
