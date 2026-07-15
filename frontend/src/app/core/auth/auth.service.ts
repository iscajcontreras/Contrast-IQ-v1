import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '@/environments/environment';
import { PermisosService } from '@/app/core/auth/permisos.service';

const CLAVE_ACCESS_TOKEN = 'auth.access_token';
const CLAVE_REFRESH_TOKEN = 'auth.refresh_token';
const CLAVE_EXPIRA_EN = 'auth.expira_en';

interface RespuestaToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface PerfilUsuario {
  nombreCompleto: string;
  email: string;
  rol: string;
}

// Login directo contra POST /api/auth/login (email+password, sin
// redirect a ningun Authorization Server aparte): el backend valida y
// regresa un access token (JWT corto) + un refresh token (opaco, mas
// largo). El interceptor llama a refrescarToken() solo cuando el
// backend responde 401, y este servicio se encarga de renovarlo solo
// con el refresh token guardado, sin pedirle contrasena al usuario de
// nuevo. Reemplaza el flujo anterior de OAuth 2.1 + PKCE + redirect,
// que para una SPA de primera parte solo agregaba una pantalla extra
// (doble autenticacion) sin aportar seguridad adicional real.
//
// IMPORTANTE (SSR): este proyecto puede renderizar tambien en el
// servidor (Node), donde no existen localStorage/window. Por eso TODO
// acceso a esas APIs esta protegido con `esNavegador`.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private permisosService = inject(PermisosService);

  // Cuando el token queda invalido (ej. se reinicio el backend, o
  // simplemente vencio) y varios componentes piden datos al mismo
  // tiempo (dashboard, campanita, etc.), cada peticion 401 dispara su
  // propio refrescarToken() en paralelo -- como el refresh token es de
  // un solo uso, la primera lo rota y las demas fallan, generando una
  // rafaga de errores en consola aunque al final la sesion se cierre
  // correctamente. Este campo comparte UN SOLO intento de renovacion
  // entre todas las llamadas simultaneas.
  private refrescoEnCurso: Promise<boolean> | null = null;

  private accessToken = signal<string | null>(
    this.esNavegador ? localStorage.getItem(CLAVE_ACCESS_TOKEN) : null
  );
  private expiraEn = signal<number | null>(
    this.esNavegador && localStorage.getItem(CLAVE_EXPIRA_EN)
      ? Number(localStorage.getItem(CLAVE_EXPIRA_EN))
      : null
  );

  estaAutenticado = computed(() => {
    const token = this.accessToken();
    const expira = this.expiraEn();
    return !!token && !!expira && Date.now() < expira;
  });

  // Datos del usuario que inicio sesion (nombre real, email, rol). Se
  // llenan llamando a GET /api/auth/me -- ver cargarPerfil().
  perfil = signal<PerfilUsuario | null>(null);

  constructor() {
    // Si ya habia un token valido guardado (ej. recargaste la pagina),
    // trae el perfil real en vez de dejar el menu de usuario vacio.
    if (this.esNavegador && this.estaAutenticado()) {
      this.cargarPerfil();
      this.permisosService.asegurarCargado().subscribe();
    }
  }

  async cargarPerfil(): Promise<void> {
    try {
      const perfil = await firstValueFrom(
        this.http.get<PerfilUsuario>(`${environment.apiBaseUrl}/auth/me`)
      );
      this.perfil.set(perfil);
    } catch {
      this.perfil.set(null);
    }
  }

  obtenerAccessToken(): string | null {
    return this.accessToken();
  }

  // Login directo: el propio formulario de sign-in llama a esto con lo
  // que capturo el usuario (correo + contrasena), sin ningun redirect.
  async iniciarSesion(email: string, password: string): Promise<void> {
    const respuesta = await firstValueFrom(
      this.http.post<RespuestaToken>(`${environment.apiBaseUrl}/auth/login`, { email, password })
    );
    this.guardarTokens(respuesta);
    await this.cargarPerfil();
    this.permisosService.asegurarCargado().subscribe();
  }

  // --- Refresh token (llamado por el interceptor cuando el access_token expira) ---
  async refrescarToken(): Promise<boolean> {
    if (!this.esNavegador) return false;

    if (this.refrescoEnCurso) {
      return this.refrescoEnCurso;
    }

    const refreshToken = localStorage.getItem(CLAVE_REFRESH_TOKEN);
    if (!refreshToken) return false;

    this.refrescoEnCurso = (async () => {
      try {
        const respuesta = await firstValueFrom(
          this.http.post<RespuestaToken>(`${environment.apiBaseUrl}/auth/refresh`, { refreshToken })
        );
        this.guardarTokens(respuesta);
        return true;
      } catch {
        this.cerrarSesion();
        return false;
      } finally {
        this.refrescoEnCurso = null;
      }
    })();

    return this.refrescoEnCurso;
  }

  cerrarSesion(): void {
    if (this.esNavegador) {
      const refreshToken = localStorage.getItem(CLAVE_REFRESH_TOKEN);
      if (refreshToken) {
        // Revoca el refresh token del lado del servidor (best-effort: si
        // falla, igual se limpia todo del lado del cliente).
        this.http.post(`${environment.apiBaseUrl}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
      }
      localStorage.removeItem(CLAVE_ACCESS_TOKEN);
      localStorage.removeItem(CLAVE_REFRESH_TOKEN);
      localStorage.removeItem(CLAVE_EXPIRA_EN);
    }
    this.accessToken.set(null);
    this.expiraEn.set(null);
    this.perfil.set(null);
    // Sin esto, los permisos del usuario anterior seguirian visibles en
    // el menu si otra persona inicia sesion en el mismo navegador.
    this.permisosService.limpiar();
  }

  private guardarTokens(respuesta: RespuestaToken): void {
    const expiraEn = Date.now() + respuesta.expiresIn * 1000;

    if (this.esNavegador) {
      localStorage.setItem(CLAVE_ACCESS_TOKEN, respuesta.accessToken);
      localStorage.setItem(CLAVE_EXPIRA_EN, String(expiraEn));
      localStorage.setItem(CLAVE_REFRESH_TOKEN, respuesta.refreshToken);
    }

    this.accessToken.set(respuesta.accessToken);
    this.expiraEn.set(expiraEn);
  }
}
