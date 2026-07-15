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
