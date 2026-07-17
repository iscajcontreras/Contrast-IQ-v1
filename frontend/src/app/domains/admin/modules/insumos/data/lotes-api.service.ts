import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface Lote {
  id: number;
  numeroLote: string;
  agente: string;
  sede: string;
  fechaCaducidad: string;
  cantidadMl: number;
  recibidoEn: string;
  activo: boolean;
  vencido: boolean;
  diasParaCaducar: number;
  // Julio 2026: indicador visual "ya tiene inyecciones realizadas" -- ver
  // LoteService.buscar (backend), calculado en batch por pagina.
  tieneInyecciones: boolean;
}

export interface PaginaLotes {
  content: Lote[];
  totalElements: number;
}

export interface CrearLoteRequest {
  agenteId: number;
  sedeId: number;
  numeroLote: string;
  fechaCaducidad: string;
  cantidadMl: number;
}

export interface TrazabilidadLote {
  inyeccionId: number;
  fechaHoraInyeccion: string;
  pacienteIdentificador: string;
  pacienteNombre: string | null;
  sede: string;
  sala: string;
  inyector: string;
  protocolo: string;
  operador: string;
  estadoInyeccion: string;
}

export interface OpcionCatalogo {
  id: number;
  etiqueta: string;
}

export interface PedidoReabastecimiento {
  id: number;
  sede: string;
  agente: string;
  cantidadSolicitadaMl: number;
  estado: string;
  generadoAutomaticamente: boolean;
  fechaSolicitud: string;
  fechaEnvio: string | null;
  fechaRecepcion: string | null;
  notas: string | null;
}

// Trazabilidad de insumos: registrar lotes de contraste y, ante un
// recall del fabricante, consultar de inmediato que pacientes lo
// recibieron (ver LoteController.trazabilidad en el backend).
@Injectable({ providedIn: 'root' })
export class LotesApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/lotes`;
  private basePedidos = `${environment.apiBaseUrl}/pedidos-reabastecimiento`;

  getSedes(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/sedes`);
  }

  getAgentes(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/agentes-contraste`);
  }

  listarPedidos(): Observable<PedidoReabastecimiento[]> {
    return this.http.get<PedidoReabastecimiento[]>(this.basePedidos);
  }

  actualizarPedido(id: number, estado: string): Observable<PedidoReabastecimiento> {
    return this.http.patch<PedidoReabastecimiento>(`${this.basePedidos}/${id}`, { estado });
  }

  buscar(filtros: { sedeId?: number; agenteId?: number; soloVigentes?: boolean; proximosACaducar?: boolean; page?: number; size?: number }): Observable<PaginaLotes> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([k, v]) => {
      if (v !== undefined && v !== null) params = params.set(k, String(v));
    });
    return this.http.get<PaginaLotes>(this.base, { params });
  }

  crear(datos: CrearLoteRequest): Observable<Lote> {
    return this.http.post<Lote>(this.base, datos);
  }

  trazabilidad(loteId: number): Observable<TrazabilidadLote[]> {
    return this.http.get<TrazabilidadLote[]>(`${this.base}/${loteId}/trazabilidad`);
  }
}

// Merma de insumos (contraste + solucion salina): volumen programado vs.
// realmente inyectado por fase (inyeccion_fases), agregado en las 4
// vistas pedidas por el stakeholder -- ver MermaController en el
// backend, que es la fuente de verdad de estos tipos.
export interface MermaResumen {
  volumenProgramadoMl: number;
  volumenRealMl: number;
  volumenMermaMl: number;
  porcentajeMerma: number;
  volumenMermaPeriodoAnteriorMl: number | null;
  variacionPorcentual: number | null;
}

export interface MermaPorSede {
  sedeId: number;
  sede: string;
  volumenProgramadoMl: number;
  volumenRealMl: number;
  volumenMermaMl: number;
  porcentajeMerma: number;
}

export interface MermaPorInsumo {
  agenteId: number;
  nombreComercial: string;
  tipo: 'CONTRASTE' | 'SOLUCION_SALINA';
  fabricante: string | null;
  volumenProgramadoMl: number;
  volumenRealMl: number;
  volumenMermaMl: number;
  porcentajeMerma: number;
}

export interface MermaInyeccion {
  inyeccionId: number;
  fechaHoraInicio: string;
  paciente: string | null;
  numeroExpediente: string | null;
  sede: string;
  sala: string;
  estado: string;
  motivoAborto: string | null;
  volumenProgramadoMl: number;
  volumenRealMl: number;
  volumenMermaMl: number;
  porcentajeMerma: number;
}

export interface PaginaMermaInyeccion {
  content: MermaInyeccion[];
  totalElements: number;
}

// Julio 2026: filtros "completos" (los mismos que la barra del dashboard
// de Inyecciones de contraste), usados SOLO por resumenFiltrado -- ver
// nota en mermas.ts sobre por que por-sede/por-insumo/por-inyeccion se
// quedan solo con fecha por ahora.
export interface FiltroMermaCompleto {
  fechaInicio: string;
  fechaFin: string;
  salaId?: number;
  identificadorAnatomicoId?: number;
  agenteId?: number;
  estado?: string;
  soloConAlertaEda?: boolean;
}

@Injectable({ providedIn: 'root' })
export class MermasApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/insumos/mermas`;

  resumen(desde: string, hasta: string): Observable<MermaResumen> {
    return this.http.get<MermaResumen>(`${this.base}/resumen`, { params: { desde, hasta } });
  }

  // Llega desde el dashboard de Inyecciones de contraste (tarjeta "Merma
  // con estos filtros") -- misma fuente que alimenta esa tarjeta
  // (MermaController.resumenFiltrado). No trae tendencia (ver
  // MermaResumenDTO / MermaService.resumenConFiltros en el backend).
  resumenFiltrado(filtro: FiltroMermaCompleto): Observable<MermaResumen> {
    let params = new HttpParams();
    Object.entries(filtro).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    });
    return this.http.get<MermaResumen>(`${this.base}/resumen-filtrado`, { params });
  }

  porSede(desde: string, hasta: string): Observable<MermaPorSede[]> {
    return this.http.get<MermaPorSede[]>(`${this.base}/por-sede`, { params: { desde, hasta } });
  }

  porInsumo(desde: string, hasta: string): Observable<MermaPorInsumo[]> {
    return this.http.get<MermaPorInsumo[]>(`${this.base}/por-insumo`, { params: { desde, hasta } });
  }

  porInyeccion(desde: string, hasta: string, page: number, size: number): Observable<PaginaMermaInyeccion> {
    return this.http.get<PaginaMermaInyeccion>(`${this.base}/por-inyeccion`, {
      params: { desde, hasta, page: String(page), size: String(size) },
    });
  }

  // --- Catalogos para los filtros extra (sala, identificador, agente) ---
  getSalas(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/salas`);
  }

  getIdentificadoresAnatomicos(): Observable<OpcionCatalogo[]> {
    return this.http.get<OpcionCatalogo[]>(`${environment.apiBaseUrl}/catalogos/identificadores-anatomicos`);
  }
}
