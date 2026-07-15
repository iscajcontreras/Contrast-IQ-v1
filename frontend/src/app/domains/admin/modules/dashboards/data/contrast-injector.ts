import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

// Base de la API del backend Spring Boot, tomada de src/environments.
// En desarrollo apunta a http://localhost:8080/api; en produccion se
// reemplaza por environment.production.ts durante el build (ver angular.json).
const API_BASE = environment.apiBaseUrl;

export interface OpcionFiltro {
  id: number;
  etiqueta: string;
}

export interface DashboardKpi {
  inyeccionesEnPeriodo: number;
  volumenTotalMl: number;
  volumenPromedioMl: number;
  alertasEdaFueraDeRango: number;
  inyectoresActivos: number;
  inyectoresTotales: number;
}

export interface VolumenDiario {
  fecha: string;
  volumenMl: number;
  totalInyecciones: number;
}

export interface DistribucionProtocolo {
  identificadorAnatomico: string;
  total: number;
  porcentaje: number;
}

export interface InyeccionResumen {
  id: number;
  fechaHoraInicio: string;
  sala: string;
  inyector: string;
  protocolo: string;
  identificadorAnatomico: string;
  agentePrincipal: string;
  volumenCargadoMl: number | null;
  volumenTotalMl: number;
  volumenResidualMl: number | null;
  presionMaximaPsi: number | null;
  presionPromedioPsi: number | null;
  presionLimitePsi: number | null;
  edaHabilitado: boolean;
  ctdiVolMgy: number | null;
  dlpMgyCm: number | null;
  estado: string;
  tieneAlertaEda: boolean;
  tieneSeriePresion: boolean;
}

export interface PuntoPresion {
  tiempoSeg: number;
  presionPsi: number;
}

export interface PaginaInyecciones {
  content: InyeccionResumen[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltroInyecciones {
  fechaInicio?: string;
  fechaFin?: string;
  salaId?: number;
  inyectorId?: number;
  protocoloId?: number;
  identificadorAnatomicoId?: number;
  agenteId?: number;
  estado?: string;
  soloConAlertaEda?: boolean;
  page?: number;
  size?: number;
}

// Cliente HTTP hacia /api/*. Un metodo por endpoint del backend; todos los
// parametros de filtro son opcionales y se omiten de la query si vienen undefined.
@Injectable({ providedIn: 'root' })
export class ContrastInjectorApiService {
  private http = inject(HttpClient);

  // --- Catalogos (alimentan los <mat-select> de la barra de filtros) ---

  getSalas(sedeId?: number): Observable<OpcionFiltro[]> {
    let params = new HttpParams();
    if (sedeId) params = params.set('sedeId', sedeId);
    return this.http.get<OpcionFiltro[]>(`${API_BASE}/catalogos/salas`, { params });
  }

  getInyectores(salaId?: number): Observable<OpcionFiltro[]> {
    let params = new HttpParams();
    if (salaId) params = params.set('salaId', salaId);
    return this.http.get<OpcionFiltro[]>(`${API_BASE}/catalogos/inyectores`, { params });
  }

  getProtocolos(identificadorAnatomicoId?: number): Observable<OpcionFiltro[]> {
    let params = new HttpParams();
    if (identificadorAnatomicoId) {
      params = params.set('identificadorAnatomicoId', identificadorAnatomicoId);
    }
    return this.http.get<OpcionFiltro[]>(`${API_BASE}/catalogos/protocolos`, { params });
  }

  getAgentesContraste(): Observable<OpcionFiltro[]> {
    return this.http.get<OpcionFiltro[]>(`${API_BASE}/catalogos/agentes-contraste`);
  }

  getIdentificadoresAnatomicos(): Observable<OpcionFiltro[]> {
    return this.http.get<OpcionFiltro[]>(`${API_BASE}/catalogos/identificadores-anatomicos`);
  }

  // --- Dashboard (KPIs y graficos) ---

  getKpis(desde: string, hasta: string): Observable<DashboardKpi> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<DashboardKpi>(`${API_BASE}/dashboard/kpis`, { params });
  }

  getUsoContraste(desde: string, hasta: string): Observable<VolumenDiario[]> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<VolumenDiario[]>(`${API_BASE}/dashboard/uso-contraste`, { params });
  }

  getDistribucionProtocolo(desde: string, hasta: string): Observable<DistribucionProtocolo[]> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<DistribucionProtocolo[]>(
      `${API_BASE}/dashboard/distribucion-protocolo`,
      { params }
    );
  }

  // --- Tabla de inyecciones recientes (todos los filtros del dashboard) ---

  getInyecciones(filtro: FiltroInyecciones): Observable<PaginaInyecciones> {
    let params = new HttpParams();
    Object.entries(filtro).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    });
    return this.http.get<PaginaInyecciones>(`${API_BASE}/inyecciones`, { params });
  }

  // Grafica de presion vs. tiempo (boton "Ver presion" en la tabla)
  getSeriePresion(inyeccionId: number): Observable<PuntoPresion[]> {
    return this.http.get<PuntoPresion[]>(`${API_BASE}/inyecciones/${inyeccionId}/presion`);
  }
}
