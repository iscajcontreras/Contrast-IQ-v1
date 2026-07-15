import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface UsuarioResumen {
  id: number;
  nombreCompleto: string;
  email: string;
  rol: string;
  sede: string | null;
  sedeId: number | null;
  activo: boolean;
  // Unico valor posible hoy es 'LOCAL' -- el backend removio el login
  // federado con Google como limpieza de deuda tecnica (Prioridad 4).
  proveedor: 'LOCAL';
  // "En linea" = tiene un refresh token vigente (no revocado, no
  // expirado); no es presencia en tiempo real. ultimoLogin es el login
  // exitoso mas reciente (null si nunca ha iniciado sesion).
  online: boolean;
  ultimoLogin: string | null;
}

export interface PaginaUsuarios {
  content: UsuarioResumen[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CrearUsuarioRequest {
  nombreCompleto: string;
  email: string;
  password: string;
  rolId: number;
  sedeId?: number | null;
}

export interface ActualizarUsuarioRequest {
  nombreCompleto: string;
  rolId: number;
  sedeId?: number | null;
}

export interface OpcionCatalogo {
  id: number;
  etiqueta: string;
}

// Endpoints de /api/usuarios: solo accesibles para ADMIN (el backend lo
// exige con @PreAuthorize, esto no es solo una restriccion visual).
@Injectable({ providedIn: 'root' })
export class UsuariosApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/usuarios`;

  getSedes(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/sedes`);
  }

  getRoles(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/roles`);
  }

  buscar(filtros: { sedeId?: number; rolId?: number; activo?: boolean; busqueda?: string; page?: number; size?: number }): Observable<PaginaUsuarios> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
    });
    return this.http.get<PaginaUsuarios>(this.base, { params });
  }

  crear(datos: CrearUsuarioRequest): Observable<UsuarioResumen> {
    return this.http.post<UsuarioResumen>(this.base, datos);
  }

  actualizar(id: number, datos: ActualizarUsuarioRequest): Observable<UsuarioResumen> {
    return this.http.put<UsuarioResumen>(`${this.base}/${id}`, datos);
  }

  cambiarEstado(id: number, activo: boolean): Observable<UsuarioResumen> {
    return this.http.patch<UsuarioResumen>(`${this.base}/${id}/estado`, { activo });
  }

  // "Historial de accesos visible en la UI"
  historialAccesos(usuarioId: number, page = 0, size = 20): Observable<PaginaHistorialAccesos> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PaginaHistorialAccesos>(
      `${environment.apiBaseUrl}/historial-accesos/usuario/${usuarioId}`,
      { params }
    );
  }
}

export interface HistorialAcceso {
  id: number;
  emailUsado: string;
  exitoso: boolean;
  metodo: 'LOCAL';
  ipOrigen: string | null;
  userAgent: string | null;
  fechaHora: string;
}

export interface PaginaHistorialAccesos {
  content: HistorialAcceso[];
  totalElements: number;
}
