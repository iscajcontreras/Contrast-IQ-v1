import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { Observable, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '@/environments/environment';

interface PermisoModulo {
  moduloCodigo: string;
  permisoCodigo: string;
}

// Cache en memoria de "moduloCodigo:permisoCodigo" para el usuario
// autenticado -- alimenta el filtrado del menu (Navigation) y
// permisoGuard. Patron adaptado del proyecto de referencia CEROGAS GPS:
// asegurarCargado() usa shareReplay(1) para que el guard y el filtro de
// menu, que suelen dispararse casi al mismo tiempo al navegar, no
// disparen dos peticiones HTTP en paralelo a /api/me/permisos.
@Injectable({ providedIn: 'root' })
export class PermisosService {
  private http = inject(HttpClient);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  private permisos = signal<Set<string>>(new Set());
  private cargado$: Observable<Set<string>> | null = null;

  asegurarCargado(): Observable<Set<string>> {
    // BUGFIX (mismo patron que auth.guard.ts, ver SolucionLogin.md): en
    // el servidor (SSR) no existe localStorage, asi que AuthService no
    // tiene ningun access_token que mandar -- una peticion a
    // /api/me/permisos ahi siempre responde 401, y ese 401 sin manejar
    // tumbaba el render SSR entero (uncaughtException) en CADA pagina,
    // porque este servicio se usa desde el layout/menu, que carga en
    // toda ruta /admin/**. En el servidor simplemente no hay permisos
    // que cargar; el cliente los pedira de verdad una vez hidratado.
    if (!this.esNavegador) {
      return of(new Set<string>());
    }

    if (this.permisos().size > 0) {
      return new Observable((subscriber) => {
        subscriber.next(this.permisos());
        subscriber.complete();
      });
    }

    if (!this.cargado$) {
      this.cargado$ = this.http
        .get<PermisoModulo[]>(`${environment.apiBaseUrl}/me/permisos`)
        .pipe(
          map((lista) => new Set(lista.map((p) => `${p.moduloCodigo}:${p.permisoCodigo}`))),
          tap((set) => this.permisos.set(set)),
          shareReplay(1)
        );
    }

    return this.cargado$;
  }

  tienePermisoSync(moduloCodigo: string, permisoCodigo = 'VER'): boolean {
    return this.permisos().has(`${moduloCodigo}:${permisoCodigo}`);
  }

  // Debe llamarse al cerrar sesion -- si no, los permisos del usuario
  // anterior quedarian visibles para el siguiente que inicie sesion en
  // el mismo navegador (ej. una PC compartida en una sede).
  limpiar(): void {
    this.permisos.set(new Set());
    this.cargado$ = null;
  }
}
