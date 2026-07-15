import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface ModuloDTO {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
}

export interface PermisoDTO {
  id: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
}

export interface RolDTO {
  id: number;
  nombre: string;
  cantidadUsuarios: number;
}

export interface MatrizCelda {
  moduloId: number;
  moduloCodigo: string;
  moduloNombre: string;
  permisoId: number;
  permisoCodigo: string;
  permisoNombre: string;
  otorgado: boolean;
}

// Endpoints de /api/administracion: solo accesibles para ADMIN (el
// backend lo exige con @PreAuthorize, ademas de @RequierePermiso en las
// mutaciones -- ver AdministracionController).
@Injectable({ providedIn: 'root' })
export class AdministracionApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/administracion`;

  getModulos(): Observable<ModuloDTO[]> {
    return this.http.get<ModuloDTO[]>(`${this.base}/modulos`);
  }

  getPermisos(): Observable<PermisoDTO[]> {
    return this.http.get<PermisoDTO[]>(`${this.base}/permisos`);
  }

  getRoles(): Observable<RolDTO[]> {
    return this.http.get<RolDTO[]>(`${this.base}/roles`);
  }

  getMatriz(rolId: number): Observable<MatrizCelda[]> {
    return this.http.get<MatrizCelda[]>(`${this.base}/roles/${rolId}/matriz`);
  }

  otorgar(rolId: number, moduloId: number, permisoId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/roles/${rolId}/matriz/otorgar`, { moduloId, permisoId });
  }

  revocar(rolId: number, moduloId: number, permisoId: number): Observable<void> {
    const params = new HttpParams().set('moduloId', moduloId).set('permisoId', permisoId);
    return this.http.delete<void>(`${this.base}/roles/${rolId}/matriz/revocar`, { params });
  }
}
