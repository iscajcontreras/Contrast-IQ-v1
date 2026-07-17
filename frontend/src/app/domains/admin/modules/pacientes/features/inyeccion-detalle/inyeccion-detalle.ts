import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, computed, inject } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexGrid,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
} from 'ng-apexcharts';
import { Theming } from '@/app/core/theming';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import { InyeccionDetalleApiService } from '@/app/domains/admin/modules/pacientes/data/inyeccion-detalle-api.service';

@Component({
  selector: 'inyeccion-detalle',
  imports: [DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatCard, ChartComponent],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <!-- Header -->
      <div class="flex items-center justify-between gap-x-3">
        <div class="flex flex-col gap-y-0.5">
          <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
            Detalle de inyeccion #{{ inyeccionId() }}
          </div>
          <div class="text-neutral-500">
            Vista clinico-operativa completa de una inyeccion especifica.
          </div>
        </div>
        <div class="flex-auto"></div>
        <button matButton="outlined" routerLink="/admin/paciente" queryParamsHandling="preserve">
          <mat-icon svgIcon="arrow-left" class="mr-1 size-4" />
          Volver al dashboard de paciente
        </button>
      </div>

      @if (detalle.isLoading()) {
        <mat-card class="p-6 text-center text-neutral-500" appearance="outlined">
          Cargando detalle de la inyeccion...
        </mat-card>
      } @else if (detalle.error()) {
        <mat-card class="p-6 text-center text-red-600" appearance="outlined">
          No se pudo cargar el detalle de esta inyeccion.
        </mat-card>
      } @else if (!detalle.value()) {
        <mat-card class="p-6 text-center text-neutral-500" appearance="outlined">
          Sin datos para esta inyeccion.
        </mat-card>
      } @else {
        @let d = detalle.value()!;

        <!-- Ficha del paciente -->
        <mat-card class="p-4" appearance="outlined">
          <div class="p-2 pb-3 text-lg font-medium">Paciente</div>
          <div class="grid grid-cols-2 gap-x-4 gap-y-2 px-2 text-sm sm:grid-cols-4">
            <div>
              <div class="text-neutral-500">Nombre</div>
              <div class="font-medium">{{ d.paciente.nombreCompleto ?? d.paciente.identificadorExterno }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Expediente (MRN)</div>
              <div class="font-medium">{{ d.paciente.numeroExpediente }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Fecha de nacimiento</div>
              <div class="font-medium">
                {{ d.paciente.fechaNacimiento ? (d.paciente.fechaNacimiento | date: 'dd-MM-yyyy') : '—' }}
                @if (edad() !== null) {
                  ({{ edad() }} años)
                }
              </div>
            </div>
            <div>
              <div class="text-neutral-500">Sexo</div>
              <div class="font-medium">{{ etiquetaSexo(d.paciente.sexo) }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Peso</div>
              <div class="font-medium">{{ d.paciente.pesoKg ?? '—' }} kg</div>
            </div>
            <div>
              <div class="text-neutral-500">Talla</div>
              <div class="font-medium">{{ d.paciente.tallaM ?? '—' }} m</div>
            </div>
            <div>
              <div class="text-neutral-500">Grupo etnico</div>
              <div class="font-medium">{{ d.paciente.grupoEtnico ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Creatinina</div>
              <div class="font-medium">{{ d.paciente.creatininaMgDl ?? '—' }} mg/dl</div>
            </div>
            <div>
              <div class="text-neutral-500">GFR</div>
              <div class="font-medium">{{ d.paciente.gfrMlMin ?? '—' }} ml/min</div>
            </div>
            <div class="col-span-2 sm:col-span-4">
              @if (d.paciente.alergias) {
                <div class="flex items-center gap-x-1.5 text-amber-700">
                  <mat-icon svgIcon="triangle-alert" class="size-4" />
                  Alergias: {{ d.paciente.alergias }}
                </div>
              }
            </div>
          </div>
        </mat-card>

        <!-- Ficha del contraste -->
        <mat-card class="p-4" appearance="outlined">
          <div class="p-2 pb-3 text-lg font-medium">Contraste</div>
          <div class="grid grid-cols-2 gap-x-4 gap-y-2 px-2 text-sm sm:grid-cols-4">
            <div>
              <div class="text-neutral-500">Agente</div>
              <div class="font-medium">{{ d.contraste.agentePrincipal }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Concentracion</div>
              <div class="font-medium">{{ d.contraste.concentracion ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Fabricante</div>
              <div class="font-medium">{{ d.contraste.fabricante ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Numero de lote</div>
              <div class="font-medium">{{ d.contraste.numeroLote ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Caducidad del lote</div>
              <div class="font-medium">
                {{ d.contraste.loteFechaCaducidad ? (d.contraste.loteFechaCaducidad | date: 'dd-MM-yyyy') : '—' }}
              </div>
            </div>
            <div>
              <div class="text-neutral-500">Dosis</div>
              <div class="font-medium">{{ d.contraste.dosisContrasteGl ?? '—' }} g/l</div>
            </div>
            <div>
              <div class="text-neutral-500">Volumen total</div>
              <div class="font-medium">{{ d.contraste.volumenTotalMl }} ml</div>
            </div>
          </div>
        </mat-card>

        <!-- Metadatos de la inyeccion -->
        <mat-card class="p-4" appearance="outlined">
          <div class="p-2 pb-3 text-lg font-medium">Metadatos de la inyeccion</div>
          <div class="grid grid-cols-2 gap-x-4 gap-y-2 px-2 text-sm sm:grid-cols-4">
            <div>
              <div class="text-neutral-500">Inicio</div>
              <div class="font-medium">{{ d.metadatos.fechaHoraInicio | date: 'dd-MM-yyyy HH:mm' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Fin</div>
              <div class="font-medium">
                {{ d.metadatos.fechaHoraFin ? (d.metadatos.fechaHoraFin | date: 'dd-MM-yyyy HH:mm') : '—' }}
              </div>
            </div>
            <div>
              <div class="text-neutral-500">Duracion</div>
              <div class="font-medium">{{ d.metadatos.duracionSeg ?? '—' }} seg</div>
            </div>
            <div>
              <div class="text-neutral-500">Estado</div>
              <div class="font-medium">{{ d.metadatos.estado }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Sede / Sala</div>
              <div class="font-medium">{{ d.metadatos.sede }} / {{ d.metadatos.sala }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Inyector</div>
              <div class="font-medium">{{ d.metadatos.inyector }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Operador</div>
              <div class="font-medium">{{ d.metadatos.operador }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Protocolo</div>
              <div class="font-medium">{{ d.metadatos.protocolo }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Identificador anatomico</div>
              <div class="font-medium">{{ d.metadatos.identificadorAnatomico }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Numero de accesion</div>
              <div class="font-medium">{{ d.metadatos.numeroAccesion ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Procedimiento programado</div>
              <div class="font-medium">{{ d.metadatos.procedimientoProgramado ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Calibre de aguja</div>
              <div class="font-medium">{{ d.metadatos.calibreAguja ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Acceso de aguja</div>
              <div class="font-medium">{{ d.metadatos.accesoAguja ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Avance de solucion salina</div>
              <div class="font-medium">{{ d.metadatos.avanceSalinaMl ?? '—' }} ml</div>
            </div>
            <div>
              <div class="text-neutral-500">Salina jump</div>
              <div class="font-medium">
                {{ d.metadatos.salinaJumpUsado === null ? '—' : d.metadatos.salinaJumpUsado ? 'Si' : 'No' }}
              </div>
            </div>
            <div>
              <div class="text-neutral-500">Scanner</div>
              <div class="font-medium">{{ d.metadatos.scanner ?? '—' }}</div>
            </div>
            <div>
              <div class="text-neutral-500">Retraso de escaneo</div>
              <div class="font-medium">{{ d.metadatos.retrasoEscaneoSeg ?? '—' }} seg</div>
            </div>
            <div>
              <div class="text-neutral-500">Presion max. / prom. / limite</div>
              <div class="font-medium">
                {{ d.metadatos.presionMaximaPsi ?? '—' }} /
                {{ d.metadatos.presionPromedioPsi ?? '—' }} /
                {{ d.metadatos.presionLimitePsi ?? '—' }} psi
              </div>
            </div>
            <div>
              <div class="text-neutral-500">EDA</div>
              <div class="font-medium">
                @if (d.metadatos.edaHabilitado) {
                  <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Habilitado</span>
                } @else {
                  <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">Deshabilitado</span>
                }
              </div>
            </div>
            <div>
              <div class="text-neutral-500">CTDIvol / DLP</div>
              <div class="font-medium">
                {{ d.metadatos.ctdiVolMgy ?? '—' }} mGy /
                {{ d.metadatos.dlpMgyCm ?? '—' }} mGy&middot;cm
              </div>
            </div>
            <div class="col-span-2 sm:col-span-4">
              <div class="text-neutral-500">Notas</div>
              <div class="font-medium">{{ d.metadatos.notas ?? '—' }}</div>
            </div>
          </div>
        </mat-card>

        <!-- Graficos -->
        <div class="grid w-full grid-cols-1 gap-4 lg:grid-cols-2">
          <mat-card class="overflow-hidden" appearance="outlined">
            <div class="p-6 pb-0 text-lg font-medium">Presion vs. tiempo</div>
            <div class="flex flex-auto flex-col">
              <apx-chart
                class="h-72"
                [chart]="presionChart().chart"
                [series]="presionChart().series"
                [xaxis]="presionChart().xaxis"
                [yaxis]="presionChart().yaxis"
                [dataLabels]="presionChart().dataLabels"
                [stroke]="presionChart().stroke"
                [grid]="presionChart().grid"
                [tooltip]="presionChart().tooltip"
              ></apx-chart>
            </div>
          </mat-card>

          <mat-card class="overflow-hidden" appearance="outlined">
            <div class="p-6 pb-0 text-lg font-medium">Flujo vs. tiempo</div>
            <div class="flex flex-auto flex-col">
              <apx-chart
                class="h-72"
                [chart]="flujoChart().chart"
                [series]="flujoChart().series"
                [xaxis]="flujoChart().xaxis"
                [yaxis]="flujoChart().yaxis"
                [dataLabels]="flujoChart().dataLabels"
                [stroke]="flujoChart().stroke"
                [grid]="flujoChart().grid"
                [tooltip]="flujoChart().tooltip"
              ></apx-chart>
            </div>
          </mat-card>
        </div>

        <!-- Comparativo planeado / programado / real -->
        <mat-card appearance="outlined">
          <div class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 p-6 pb-0">
            <div class="text-lg font-medium">Comparativo por fase: planeado / programado / real</div>
            <!-- Merma total: pregunta explicita del usuario -- "hay algun
                 lugar donde pueda ver si hubo merma de esa inyeccion".
                 Se calcula sumando programado/real de todas las fases
                 (mismo campo que usa el modulo de Merma de insumos, solo
                 que aqui es el total de ESTA inyeccion, no de un rango
                 de fechas). -->
            @if (mermaTotal(); as merma) {
              <div class="text-sm">
                Merma total de esta inyeccion:
                <span class="font-semibold" [class.text-red-600]="merma.volumenMermaMl > 0">
                  {{ merma.volumenMermaMl | number: '1.0-1' }} ml
                </span>
                @if (merma.volumenProgramadoMl > 0) {
                  <span class="text-neutral-500">({{ merma.porcentaje | number: '1.0-1' }}%)</span>
                }
              </div>
            }
          </div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-left text-neutral-500">
                <tr>
                  <th class="px-6 py-2 font-normal">Fase</th>
                  <th class="px-6 py-2 font-normal">Planeado</th>
                  <th class="px-6 py-2 font-normal">Programado</th>
                  <th class="px-6 py-2 font-normal">Real</th>
                  <th class="px-6 py-2 font-normal">Merma</th>
                </tr>
              </thead>
              <tbody>
                @for (fase of d.comparativoFases; track fase.numeroFase) {
                  <tr class="border-t border-neutral-100">
                    <td class="px-6 py-2 font-medium">Fase {{ fase.numeroFase }}</td>
                    <td class="px-6 py-2">
                      @if (fase.planeadoAgente || fase.planeadoVolumenMl || fase.planeadoVelocidadFlujoMlS) {
                        <div>{{ fase.planeadoAgente ?? '—' }}</div>
                        <div class="text-neutral-500">
                          {{ fase.planeadoVolumenMl ?? '—' }} ml &middot;
                          {{ fase.planeadoVelocidadFlujoMlS ?? '—' }} ml/s
                        </div>
                      } @else {
                        <span class="text-neutral-400">Sin dato</span>
                      }
                    </td>
                    <td class="px-6 py-2">
                      @if (fase.programadoAgente || fase.programadoVolumenMl || fase.programadoVelocidadFlujoMlS) {
                        <div>{{ fase.programadoAgente ?? '—' }}</div>
                        <div class="text-neutral-500">
                          {{ fase.programadoVolumenMl ?? '—' }} ml &middot;
                          {{ fase.programadoVelocidadFlujoMlS ?? '—' }} ml/s
                        </div>
                      } @else {
                        <span class="text-neutral-400">Sin dato</span>
                      }
                    </td>
                    <td class="px-6 py-2">
                      @if (fase.realAgente || fase.realVolumenRealMl || fase.realVelocidadFlujoMlS) {
                        <div>{{ fase.realAgente ?? '—' }}</div>
                        <div class="text-neutral-500">
                          programado {{ fase.realVolumenProgramadoMl ?? '—' }} ml &middot;
                          real {{ fase.realVolumenRealMl ?? '—' }} ml &middot;
                          {{ fase.realVelocidadFlujoMlS ?? '—' }} ml/s
                        </div>
                      } @else {
                        <span class="text-neutral-400">Sin dato</span>
                      }
                    </td>
                    <td class="px-6 py-2">
                      @if (fase.realVolumenProgramadoMl !== null && fase.realVolumenRealMl !== null) {
                        @let mermaFase = fase.realVolumenProgramadoMl - fase.realVolumenRealMl;
                        <span [class.text-red-600]="mermaFase > 0">
                          {{ mermaFase | number: '1.0-1' }} ml
                        </span>
                      } @else {
                        <span class="text-neutral-400">Sin dato</span>
                      }
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="px-6 py-6 text-center text-neutral-500">
                      Sin fases registradas para esta inyeccion.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </mat-card>
      }
    </div>
  `,
})
export default class InyeccionDetalle {
  private api = inject(InyeccionDetalleApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private theming = inject(Theming);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected inyeccionId = toSignal(
    this.route.paramMap.pipe(map((params) => Number(params.get('id')))),
    { initialValue: NaN },
  );

  protected detalle = rxResource({
    params: () => this.inyeccionId(),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.obtenerDetalleCompleto(params),
        {
          inyeccionId: 0,
          paciente: {
            id: 0,
            identificadorExterno: '',
            nombreCompleto: null,
            numeroExpediente: '',
            sexo: '',
            fechaNacimiento: null,
            pesoKg: null,
            tallaM: null,
            grupoEtnico: null,
            gfrMlMin: null,
            creatininaMgDl: null,
            alergias: null,
          },
          contraste: {
            agentePrincipal: '',
            concentracion: null,
            fabricante: null,
            numeroLote: null,
            loteFechaCaducidad: null,
            dosisContrasteGl: null,
            volumenTotalMl: 0,
          },
          metadatos: {
            fechaHoraInicio: '',
            fechaHoraFin: null,
            duracionSeg: null,
            sede: '',
            sala: '',
            inyector: '',
            operador: '',
            protocolo: '',
            identificadorAnatomico: '',
            estado: '',
            numeroAccesion: null,
            procedimientoProgramado: null,
            calibreAguja: null,
            accesoAguja: null,
            avanceSalinaMl: null,
            salinaJumpUsado: null,
            scanner: null,
            notas: null,
            retrasoEscaneoSeg: null,
            presionMaximaPsi: null,
            presionPromedioPsi: null,
            presionLimitePsi: null,
            edaHabilitado: false,
            ctdiVolMgy: null,
            dlpMgyCm: null,
          },
          seriePresion: [],
          serieFlujo: [],
          comparativoFases: [],
        }
      ),
  });

  // Julio 2026: el usuario pregunto si hay algun lugar en esta pantalla
  // que muestre la merma de la inyeccion -- no lo habia, solo se veian
  // programado/real por fase por separado. Se suma programado y real de
  // TODAS las fases (mismo criterio que MermaService en el backend) para
  // dar un total de la inyeccion completa, ademas del desglose por fase
  // agregado en la tabla de abajo.
  protected mermaTotal = computed(() => {
    const fases = this.detalle.value()?.comparativoFases ?? [];
    let volumenProgramadoMl = 0;
    let volumenRealMl = 0;
    for (const fase of fases) {
      volumenProgramadoMl += fase.realVolumenProgramadoMl ?? 0;
      volumenRealMl += fase.realVolumenRealMl ?? 0;
    }
    const volumenMermaMl = volumenProgramadoMl - volumenRealMl;
    const porcentaje = volumenProgramadoMl > 0 ? (volumenMermaMl / volumenProgramadoMl) * 100 : 0;
    return { volumenProgramadoMl, volumenRealMl, volumenMermaMl, porcentaje };
  });

  protected edad = computed(() => {
    const fechaNacimiento = this.detalle.value()?.paciente.fechaNacimiento;
    if (!fechaNacimiento) return null;
    const nacimiento = new Date(fechaNacimiento);
    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const aunNoCumpleAnios =
      hoy.getMonth() < nacimiento.getMonth() ||
      (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() < nacimiento.getDate());
    if (aunNoCumpleAnios) edad--;
    return edad;
  });

  etiquetaSexo(sexo: string | undefined): string {
    const etiquetas: Record<string, string> = { M: 'Masculino', F: 'Femenino', OTRO: 'Otro', NO_ESPECIFICADO: 'No especificado' };
    return sexo ? (etiquetas[sexo] ?? sexo) : '';
  }

  protected presionChart = computed(() => {
    const isDark = this.theming.isDark();
    const datos = this.detalle.value()?.seriePresion ?? [];

    const chart: ApexChart = { type: 'line', height: 280, toolbar: { show: false } };
    const series: ApexAxisChartSeries = [
      { name: 'Presion (psi)', data: datos.map((p) => ({ x: p.tiempoSeg, y: p.presionPsi })) },
    ];
    const xaxis: ApexXAxis = {
      type: 'numeric',
      title: { text: 'Segundos desde el inicio' },
      labels: { style: { colors: isDark ? '#a3a3a3' : '#525252' } },
    };
    const yaxis: ApexYAxis = {
      title: { text: 'psi' },
      labels: { style: { colors: isDark ? '#a3a3a3' : '#525252' } },
    };
    const dataLabels: ApexDataLabels = { enabled: false };
    const stroke: ApexStroke = { width: 2, curve: 'smooth' };
    const grid: ApexGrid = { borderColor: isDark ? '#404040' : '#e5e5e5' };
    const tooltip: ApexTooltip = { theme: isDark ? 'dark' : 'light' };

    return { chart, series, xaxis, yaxis, dataLabels, stroke, grid, tooltip };
  });

  protected flujoChart = computed(() => {
    const isDark = this.theming.isDark();
    const datos = this.detalle.value()?.serieFlujo ?? [];

    const chart: ApexChart = { type: 'line', height: 280, toolbar: { show: false } };
    const series: ApexAxisChartSeries = [
      { name: 'Flujo contraste (ml/s)', data: datos.map((p) => ({ x: p.tiempoSeg, y: p.flujoContrasteMlS })) },
      { name: 'Flujo salina (ml/s)', data: datos.map((p) => ({ x: p.tiempoSeg, y: p.flujoSalinaMlS })) },
    ];
    const xaxis: ApexXAxis = {
      type: 'numeric',
      title: { text: 'Segundos desde el inicio' },
      labels: { style: { colors: isDark ? '#a3a3a3' : '#525252' } },
    };
    const yaxis: ApexYAxis = {
      title: { text: 'ml/s' },
      labels: { style: { colors: isDark ? '#a3a3a3' : '#525252' } },
    };
    const dataLabels: ApexDataLabels = { enabled: false };
    const stroke: ApexStroke = { width: 2, curve: 'smooth' };
    const grid: ApexGrid = { borderColor: isDark ? '#404040' : '#e5e5e5' };
    const tooltip: ApexTooltip = { theme: isDark ? 'dark' : 'light' };

    return { chart, series, xaxis, yaxis, dataLabels, stroke, grid, tooltip };
  });
}
