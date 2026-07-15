import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface ComparativaSede {
  sedeId: number;
  sede: string;
  totalInyecciones: number;
  volumenTotalMl: number;
  inyeccionesFallidas: number;
  tasaFallaPorcentaje: number;
}

// "Reportes ejecutivos": comparativa de KPIs entre sedes, exportable a
// Excel para direccion / jefatura de radiologia.
@Injectable({ providedIn: 'root' })
export class ReportesApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/reportes`;

  comparativaSedes(desde: string, hasta: string): Observable<ComparativaSede[]> {
    return this.http.get<ComparativaSede[]>(`${this.base}/comparativa-sedes`, {
      params: { desde, hasta },
    });
  }

  descargarExcel(desde: string, hasta: string): Observable<Blob> {
    return this.http.get(`${this.base}/comparativa-sedes/excel`, {
      params: { desde, hasta },
      responseType: 'blob',
    });
  }
}
