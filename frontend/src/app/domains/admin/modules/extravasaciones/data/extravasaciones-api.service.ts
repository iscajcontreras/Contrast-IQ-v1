import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

const API_BASE = environment.apiBaseUrl;

// Espejo de EventoExtravasacionDTO (backend).
export interface EventoExtravasacion {
  id: number;
  inyeccionId: number;
  fechaHora: string;
  estadoEda: string; // SIN_REFERENCIA | EN_RANGO | FUERA_DE_RANGO
  revisado: boolean;
  sala: string;
  inyector: string;
  accionTomada: string | null;
}

export interface PaginaExtravasaciones {
  content: EventoExtravasacion[];
  totalElements: number;
}

export interface FiltroExtravasaciones {
  desde?: string;
  hasta?: string;
  estadoEda?: string;
  revisado?: boolean;
  page?: number;
  size?: number;
}

export interface RevisarExtravasacionRequest {
  accionTomada?: string;
  notas?: string;
}

// Julio 2026: modulo nuevo (antes solo existia el backend -- ver
// ExtravasacionController/ExtravasacionService -- sin ninguna pantalla
// que lo consumiera). Se creo a peticion explicita del usuario para que
// la tarjeta "Alertas de extravasacion" del dashboard de Inyecciones
// tuviera a donde redirigir.
@Injectable({ providedIn: 'root' })
export class ExtravasacionesApiService {
  private http = inject(HttpClient);

  buscar(filtro: FiltroExtravasaciones): Observable<PaginaExtravasaciones> {
    let params = new HttpParams();
    Object.entries(filtro).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    });
    return this.http.get<PaginaExtravasaciones>(`${API_BASE}/extravasaciones`, { params });
  }

  revisar(id: number, request: RevisarExtravasacionRequest): Observable<EventoExtravasacion> {
    return this.http.patch<EventoExtravasacion>(`${API_BASE}/extravasaciones/${id}/revisar`, request);
  }
}
