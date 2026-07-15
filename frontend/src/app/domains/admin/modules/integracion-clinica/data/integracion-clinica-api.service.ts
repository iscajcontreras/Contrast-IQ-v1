import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface DatosPacienteHis {
  identificadorExterno: string;
  nombreCompleto: string | null;
  sexo: string | null;
  pesoKg: number | null;
  alergias: string | null;
  simulado: boolean;
  fuente: string;
}

export interface LoteSincronizacion {
  id: number;
  fuente: string;
  fechaHora: string;
  registrosImportados: number;
  estado: string;
  usuario: string;
  detalle: string | null;
}

export interface PaginaLotesSincronizacion {
  content: LoteSincronizacion[];
  totalElements: number;
}

// "Integracion clinica": traer datos del paciente desde el HIS, y
// sincronizacion real con el inyector (importacion desde archivo).
@Injectable({ providedIn: 'root' })
export class IntegracionClinicaApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/integracion-clinica`;

  buscarEnHis(identificadorExterno: string): Observable<DatosPacienteHis> {
    return this.http.get<DatosPacienteHis>(`${this.base}/his/${identificadorExterno}`);
  }

  sincronizarDesdeHis(identificadorExterno: string): Observable<DatosPacienteHis> {
    return this.http.post<DatosPacienteHis>(`${this.base}/his/${identificadorExterno}/sincronizar`, {});
  }

  sincronizarInyector(): Observable<LoteSincronizacion> {
    return this.http.post<LoteSincronizacion>(`${this.base}/sincronizar-inyector`, {});
  }

  historialSincronizacion(page = 0, size = 10): Observable<PaginaLotesSincronizacion> {
    return this.http.get<PaginaLotesSincronizacion>(`${this.base}/historial-sincronizacion`, {
      params: { page, size },
    });
  }
}
