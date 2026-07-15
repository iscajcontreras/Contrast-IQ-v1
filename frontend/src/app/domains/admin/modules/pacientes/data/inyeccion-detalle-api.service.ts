import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface PacienteDetalleCompleto {
  id: number;
  identificadorExterno: string;
  nombreCompleto: string | null;
  numeroExpediente: string;
  sexo: string;
  fechaNacimiento: string | null;
  pesoKg: number | null;
  tallaM: number | null;
  grupoEtnico: string | null;
  gfrMlMin: number | null;
  creatininaMgDl: number | null;
  alergias: string | null;
}

export interface ContrasteDetalleCompleto {
  agentePrincipal: string;
  concentracion: string | null;
  fabricante: string | null;
  numeroLote: string | null;
  loteFechaCaducidad: string | null;
  dosisContrasteGl: number | null;
  volumenTotalMl: number;
}

export interface MetadatosDetalleCompleto {
  fechaHoraInicio: string;
  fechaHoraFin: string | null;
  duracionSeg: number | null;
  sede: string;
  sala: string;
  inyector: string;
  operador: string;
  protocolo: string;
  identificadorAnatomico: string;
  estado: string;
  numeroAccesion: string | null;
  procedimientoProgramado: string | null;
  calibreAguja: string | null;
  accesoAguja: string | null;
  avanceSalinaMl: number | null;
  salinaJumpUsado: boolean | null;
  scanner: string | null;
  notas: string | null;
  retrasoEscaneoSeg: number | null;
  presionMaximaPsi: number | null;
  presionPromedioPsi: number | null;
  presionLimitePsi: number | null;
  edaHabilitado: boolean;
  ctdiVolMgy: number | null;
  dlpMgyCm: number | null;
}

export interface PuntoPresionDetalle {
  tiempoSeg: number;
  presionPsi: number;
}

export interface PuntoFlujoDetalle {
  tiempoSeg: number;
  flujoContrasteMlS: number;
  flujoSalinaMlS: number;
}

export interface ComparativoFaseDetalle {
  numeroFase: number;
  planeadoAgente: string | null;
  planeadoVolumenMl: number | null;
  planeadoVelocidadFlujoMlS: number | null;
  programadoAgente: string | null;
  programadoVolumenMl: number | null;
  programadoVelocidadFlujoMlS: number | null;
  realAgente: string | null;
  realVolumenProgramadoMl: number | null;
  realVolumenRealMl: number | null;
  realVelocidadFlujoMlS: number | null;
}

export interface InyeccionDetalleCompletoResponse {
  inyeccionId: number;
  paciente: PacienteDetalleCompleto;
  contraste: ContrasteDetalleCompleto;
  metadatos: MetadatosDetalleCompleto;
  seriePresion: PuntoPresionDetalle[];
  serieFlujo: PuntoFlujoDetalle[];
  comparativoFases: ComparativoFaseDetalle[];
}

// Dashboard paciente v2: detalle completo de una inyeccion especifica
// (ficha clinica, contraste, metadatos operativos, series de presion/flujo
// y comparativo planeado/programado/real por fase).
@Injectable({ providedIn: 'root' })
export class InyeccionDetalleApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/inyecciones`;

  obtenerDetalleCompleto(inyeccionId: number): Observable<InyeccionDetalleCompletoResponse> {
    return this.http.get<InyeccionDetalleCompletoResponse>(`${this.base}/${inyeccionId}/detalle-completo`);
  }
}
