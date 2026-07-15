import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface PrediccionFalla {
  inyectorId: number;
  numeroSerie: string;
  sala: string;
  sede: string;
  estado: string;
  ciclosDesdeMantenimiento: number;
  umbralCiclos: number;
  porcentajeUso: number;
  riesgoFalla: boolean;
  fechaUltimoMantenimiento: string | null;
  diasDesdeMantenimiento: number;
}

export interface CalibracionProgramada {
  inyectorId: number;
  numeroSerie: string;
  sala: string;
  sede: string;
  ultimaCalibracion: string | null;
  proximaCalibracion: string;
  diasRestantes: number;
  vencida: boolean;
}

export interface TicketSoporte {
  id: number;
  inyectorId: number;
  inyectorNumeroSerie: string;
  sala: string;
  creadoPor: string;
  titulo: string;
  descripcion: string;
  prioridad: string;
  estado: string;
  numeroTicketFabricante: string | null;
  respuestaFabricante: string | null;
  fechaCreacion: string;
  fechaCierre: string | null;
}

export interface CrearTicketRequest {
  inyectorId: number;
  titulo: string;
  descripcion: string;
  prioridad: string;
}

export interface ActualizarTicketRequest {
  estado: string;
  numeroTicketFabricante?: string;
  respuestaFabricante?: string;
}

// "Mantenimiento predictivo": prediccion de falla por ciclos de uso,
// calendario de calibracion, y tickets de soporte con el fabricante.
@Injectable({ providedIn: 'root' })
export class MantenimientoApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/mantenimiento-predictivo`;
  private baseTickets = `${environment.apiBaseUrl}/tickets-soporte`;

  predicciones(): Observable<PrediccionFalla[]> {
    return this.http.get<PrediccionFalla[]>(`${this.base}/predicciones`);
  }

  calendarioCalibracion(): Observable<CalibracionProgramada[]> {
    return this.http.get<CalibracionProgramada[]>(`${this.base}/calendario-calibracion`);
  }

  listarTickets(): Observable<TicketSoporte[]> {
    return this.http.get<TicketSoporte[]>(this.baseTickets);
  }

  crearTicket(datos: CrearTicketRequest): Observable<TicketSoporte> {
    return this.http.post<TicketSoporte>(this.baseTickets, datos);
  }

  actualizarTicket(id: number, datos: ActualizarTicketRequest): Observable<TicketSoporte> {
    return this.http.patch<TicketSoporte>(`${this.baseTickets}/${id}`, datos);
  }

  getInyectores(): Observable<{ id: number; etiqueta: string }[]> {
    return this.http.get<{ id: number; etiqueta: string }[]>(`${environment.apiBaseUrl}/catalogos/inyectores`);
  }
}
