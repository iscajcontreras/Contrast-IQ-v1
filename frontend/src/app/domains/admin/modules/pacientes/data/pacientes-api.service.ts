import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface PacienteResumen {
  id: number;
  identificadorExterno: string;
  nombreCompleto: string | null;
  sexo: string;
}

export interface PaginaPacientes {
  content: PacienteResumen[];
  totalElements: number;
}

export interface PacienteDetalle {
  id: number;
  identificadorExterno: string;
  nombreCompleto: string | null;
  sexo: string;
  pesoKg: number | null;
  creatininaMgDl: number | null;
  gfrMlMin: number | null;
  riesgoRenal: boolean;
  alergias: string | null;
  totalInyecciones: number;
  volumenTotalRecibidoMl: number;
  dlpTotalMgyCm: number | null;
  ultimaInyeccion: string | null;
  alertasEdaFueraDeRango: number;
  inyeccionesAbortadasOError: number;
}

export interface HistorialInyeccionPaciente {
  inyeccionId: number;
  fechaHoraInicio: string;
  sede: string;
  sala: string;
  protocolo: string;
  identificadorAnatomico: string;
  agentePrincipal: string;
  volumenTotalMl: number;
  dlpMgyCm: number | null;
  presionMaximaPsi: number | null;
  edaHabilitado: boolean;
  estado: string;
  tieneAlertaEda: boolean;
  tieneSeriePresion: boolean;
  operador: string;
}

export interface ReaccionPaciente {
  eventoId: number;
  inyeccionId: number;
  fechaHora: string;
  estadoEda: string;
  revisado: boolean;
  accionTomada: string | null;
  notas: string | null;
  protocolo: string;
  agentePrincipal: string;
}

export interface ChecklistPreInyeccion {
  id: number;
  pacienteId: number;
  sala: string | null;
  operador: string;
  identidadVerificada: boolean;
  gfrRevisado: boolean;
  gfrValorMomento: number | null;
  riesgoRenalMomento: boolean;
  alergiasRevisadas: boolean;
  alergiasMomento: string | null;
  consentimientoFirmado: boolean;
  observaciones: string | null;
  firmaNombre: string;
  firmaImagenBase64: string;
  fechaHora: string;
}

export interface CrearChecklistRequest {
  pacienteId: number;
  salaId?: number | null;
  identidadVerificada: boolean;
  gfrRevisado: boolean;
  alergiasRevisadas: boolean;
  consentimientoFirmado: boolean;
  observaciones?: string;
  firmaNombre: string;
  firmaImagenBase64: string;
}

// Dashboard de Paciente: buscar, ver perfil clinico-operativo e
// historial completo de inyecciones de una persona especifica.
@Injectable({ providedIn: 'root' })
export class PacientesApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/pacientes`;

  buscar(busqueda: string, page = 0, size = 10): Observable<PaginaPacientes> {
    const params = new HttpParams().set('busqueda', busqueda).set('page', page).set('size', size);
    return this.http.get<PaginaPacientes>(this.base, { params });
  }

  obtenerDetalle(id: number): Observable<PacienteDetalle> {
    return this.http.get<PacienteDetalle>(`${this.base}/${id}`);
  }

  historialInyecciones(id: number): Observable<HistorialInyeccionPaciente[]> {
    return this.http.get<HistorialInyeccionPaciente[]>(`${this.base}/${id}/inyecciones`);
  }

  reacciones(id: number): Observable<ReaccionPaciente[]> {
    return this.http.get<ReaccionPaciente[]>(`${this.base}/${id}/reacciones`);
  }

  // --- Checklist pre-inyeccion ---
  historialChecklists(pacienteId: number): Observable<ChecklistPreInyeccion[]> {
    return this.http.get<ChecklistPreInyeccion[]>(`${environment.apiBaseUrl}/checklists/paciente/${pacienteId}`);
  }

  crearChecklist(datos: CrearChecklistRequest): Observable<ChecklistPreInyeccion> {
    return this.http.post<ChecklistPreInyeccion>(`${environment.apiBaseUrl}/checklists`, datos);
  }
}
