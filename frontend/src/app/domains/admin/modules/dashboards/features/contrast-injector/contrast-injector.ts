import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, TemplateRef, computed, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelect } from '@angular/material/select';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexDataLabels,
  ApexFill,
  ApexGrid,
  ApexStroke,
  ApexTooltip,
  ApexXAxis,
  ApexYAxis,
  ChartComponent,
} from 'ng-apexcharts';
import { Theming } from '@/app/core/theming';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  ContrastInjectorApiService,
  OpcionFiltro,
  InyeccionResumen,
  PuntoPresion,
} from '@/app/domains/admin/modules/dashboards/data/contrast-injector';

// Rangos de fecha rapidos para el filtro "Rango" del dashboard.
type RangoPreset = 'hoy' | 'ayer' | '7d' | '30d' | 'mes' | 'mes_anterior' | 'anio' | 'personalizado';

function formatoISO(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

// Convierte 'YYYY-MM-DD' (como llega del backend) a 'DD-MM-YYYY'.
function formatoFechaCorta(fechaIso: string): string {
  const [anio, mes, dia] = fechaIso.split('-');
  return `${dia}-${mes}-${anio}`;
}

function calcularRango(preset: Exclude<RangoPreset, 'personalizado'>): { desde: string; hasta: string } {
  const hoy = new Date();

  switch (preset) {
    case 'ayer': {
      const ayer = new Date(hoy);
      ayer.setDate(ayer.getDate() - 1);
      return { desde: formatoISO(ayer), hasta: formatoISO(ayer) };
    }
    case '7d': {
      const inicio = new Date(hoy);
      inicio.setDate(inicio.getDate() - 6);
      return { desde: formatoISO(inicio), hasta: formatoISO(hoy) };
    }
    case '30d': {
      const inicio = new Date(hoy);
      inicio.setDate(inicio.getDate() - 29);
      return { desde: formatoISO(inicio), hasta: formatoISO(hoy) };
    }
    case 'mes': {
      const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      return { desde: formatoISO(inicio), hasta: formatoISO(hoy) };
    }
    case 'mes_anterior': {
      const inicio = new Date(hoy.getFullYear(), hoy.getMonth() - 1, 1);
      const fin = new Date(hoy.getFullYear(), hoy.getMonth(), 0);
      return { desde: formatoISO(inicio), hasta: formatoISO(fin) };
    }
    case 'anio': {
      const inicio = new Date(hoy.getFullYear(), 0, 1);
      return { desde: formatoISO(inicio), hasta: formatoISO(hoy) };
    }
    default:
      // 'hoy'
      return { desde: formatoISO(hoy), hasta: formatoISO(hoy) };
  }
}

@Component({
  selector: 'contrast-injector-dashboard',
  imports: [
    DecimalPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatFormFieldModule,
    MatInputModule,
    MatSelect,
    MatOption,
    MatChipsModule,
    MatPaginatorModule,
    ChartComponent,
  ],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <!-- Header -->
      <div class="flex items-center justify-between gap-x-3">
        <div class="flex flex-col gap-y-0.5">
          <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
            Panel de inyectores de contraste
          </div>
          <div class="text-neutral-500">
            Vista operativa diaria de todas las salas.
          </div>
        </div>
        <div class="flex-auto"></div>
        <div class="flex flex-col items-end gap-y-1">
          <button matButton="outlined" (click)="recargarTodo()">
            <mat-icon svgIcon="refresh-cw" />
            Actualizar
          </button>
          <span class="text-xs text-neutral-400">Se actualiza solo cada 60 segundos</span>
        </div>
      </div>

      <!-- Barra de filtros: cuadricula compacta 2x3 -->
      <mat-card
        class="grid grid-cols-1 gap-x-3 gap-y-1 p-4 sm:grid-cols-3"
        appearance="outlined"
      >
        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Rango de fechas</mat-label>
          <mat-select [(value)]="rango">
            <mat-option value="hoy">Hoy</mat-option>
            <mat-option value="ayer">Ayer</mat-option>
            <mat-option value="7d">Ultimos 7 dias</mat-option>
            <mat-option value="30d">Ultimos 30 dias</mat-option>
            <mat-option value="mes">Este mes</mat-option>
            <mat-option value="mes_anterior">Mes anterior</mat-option>
            <mat-option value="anio">Este año</mat-option>
            <mat-option value="personalizado">Personalizado...</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Sala</mat-label>
          <mat-select
            [(value)]="salaId"
            placeholder="Todas las salas"
          >
            <mat-option [value]="undefined">Todas las salas</mat-option>
            @for (sala of salas.value(); track sala.id) {
              <mat-option [value]="sala.id">{{ sala.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Identificador anatomico</mat-label>
          <mat-select
            [(value)]="identificadorAnatomicoId"
            placeholder="Todos"
          >
            <mat-option [value]="undefined">Todos</mat-option>
            @for (id of identificadores.value(); track id.id) {
              <mat-option [value]="id.id">{{ id.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Agente de contraste</mat-label>
          <mat-select
            [(value)]="agenteId"
            placeholder="Todos"
          >
            <mat-option [value]="undefined">Todos</mat-option>
            @for (agente of agentes.value(); track agente.id) {
              <mat-option [value]="agente.id">{{ agente.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Estado</mat-label>
          <mat-select [(value)]="estado" placeholder="Todos">
            <mat-option [value]="undefined">Todos</mat-option>
            <mat-option value="COMPLETADA">Completada</mat-option>
            <mat-option value="ABORTADA">Abortada</mat-option>
            <mat-option value="ERROR">Error</mat-option>
          </mat-select>
        </mat-form-field>

        <div class="flex items-center">
          <mat-chip-listbox>
            <mat-chip-option
              [selected]="soloAlertaEda()"
              (click)="soloAlertaEda.set(!soloAlertaEda())"
            >
              Solo alertas EDA
            </mat-chip-option>
          </mat-chip-listbox>
        </div>

        @if (rango() === 'personalizado') {
          <mat-form-field class="w-full" subscriptSizing="dynamic">
            <mat-label>Desde</mat-label>
            <input
              matInput
              type="date"
              [value]="fechaDesdePersonalizada()"
              (input)="onFechaDesdeInput($any($event.target).value)"
            />
          </mat-form-field>

          <mat-form-field class="w-full" subscriptSizing="dynamic">
            <mat-label>Hasta</mat-label>
            <input
              matInput
              type="date"
              [value]="fechaHastaPersonalizada()"
              (input)="onFechaHastaInput($any($event.target).value)"
            />
          </mat-form-field>
        }
      </mat-card>


      <!-- KPIs -->
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <mat-card
          class="p-4"
          appearance="outlined"
        >
          <div class="text-neutral-500">Inyecciones en el periodo</div>
          <div class="mt-1 text-2xl font-semibold">
            {{ kpis.value()?.inyeccionesEnPeriodo ?? 0 | number: '1.0-0' }}
          </div>
        </mat-card>

        <mat-card
          class="p-4"
          appearance="outlined"
        >
          <div class="text-neutral-500">Contraste utilizado</div>
          <div class="mt-1 text-2xl font-semibold">
            {{ (kpis.value()?.volumenTotalMl ?? 0) / 1000 | number: '1.1-1' }} L
          </div>
          <div class="text-sm text-neutral-500">
            promedio {{ kpis.value()?.volumenPromedioMl ?? 0 | number: '1.0-0' }} ml
          </div>
        </mat-card>

        <mat-card
          class="cursor-pointer p-4 transition-colors hover:bg-neutral-50"
          appearance="outlined"
          role="button"
          tabindex="0"
          title="Ver detalle en Merma de insumos"
          (click)="irAMermas()"
          (keydown.enter)="irAMermas()"
        >
          <div class="flex items-center justify-between text-neutral-500">
            Merma con estos filtros
            <mat-icon svgIcon="arrow-up-right" class="size-4 text-neutral-400" />
          </div>
          <div class="mt-1 text-2xl font-semibold">
            {{ mermaFiltrada.value()?.volumenMermaMl ?? 0 | number: '1.0-0' }} ml
          </div>
          <div class="text-sm text-neutral-500">
            {{ mermaFiltrada.value()?.porcentajeMerma ?? 0 | number: '1.0-1' }}% de lo programado
          </div>
        </mat-card>

        <mat-card
          class="cursor-pointer p-4 transition-colors hover:bg-red-100"
          [class.bg-red-50]="(kpis.value()?.alertasEdaFueraDeRango ?? 0) > 0"
          appearance="outlined"
          role="button"
          tabindex="0"
          title="Ver alertas de extravasacion"
          (click)="irAExtravasaciones()"
          (keydown.enter)="irAExtravasaciones()"
        >
          <div class="flex items-center justify-between text-neutral-500">
            Alertas de extravasacion
            <mat-icon svgIcon="arrow-up-right" class="size-4 text-neutral-400" />
          </div>
          <div class="mt-1 text-2xl font-semibold text-red-600">
            {{ kpis.value()?.alertasEdaFueraDeRango ?? 0 }}
          </div>
        </mat-card>

        <mat-card
          class="cursor-pointer p-4 transition-colors hover:bg-neutral-50"
          appearance="outlined"
          role="button"
          tabindex="0"
          title="Ver mantenimiento predictivo de inyectores"
          (click)="irAMantenimiento()"
          (keydown.enter)="irAMantenimiento()"
        >
          <div class="flex items-center justify-between text-neutral-500">
            Inyectores activos
            <mat-icon svgIcon="arrow-up-right" class="size-4 text-neutral-400" />
          </div>
          <div class="mt-1 text-2xl font-semibold">
            {{ kpis.value()?.inyectoresActivos ?? 0 }} /
            {{ kpis.value()?.inyectoresTotales ?? 0 }}
          </div>
        </mat-card>
      </div>

      <!-- Graficos -->
      <div class="grid w-full grid-cols-1 gap-4 lg:grid-cols-3">
        <mat-card
          class="overflow-hidden lg:col-span-2"
          appearance="outlined"
        >
          <div class="p-6 pb-0 text-lg font-medium">Uso de agente de contraste</div>
          <div class="flex flex-auto flex-col">
            <apx-chart
              class="h-72"
              [chart]="usoContrasteChart().chart"
              [series]="usoContrasteChart().series"
              [xaxis]="usoContrasteChart().xaxis"
              [yaxis]="usoContrasteChart().yaxis"
              [dataLabels]="usoContrasteChart().dataLabels"
              [fill]="usoContrasteChart().fill"
              [grid]="usoContrasteChart().grid"
              [stroke]="usoContrasteChart().stroke"
              [tooltip]="usoContrasteChart().tooltip"
            ></apx-chart>
          </div>
        </mat-card>

        <mat-card
          class="overflow-hidden"
          appearance="outlined"
        >
          <div class="p-6 pb-0 text-lg font-medium">Distribucion por protocolo</div>
          <div class="flex flex-col gap-3 p-6">
            @for (fila of distribucion.value(); track fila.identificadorAnatomico) {
              <div>
                <div class="flex justify-between text-sm">
                  <span>{{ fila.identificadorAnatomico }}</span>
                  <span class="text-neutral-500">{{ fila.porcentaje }}%</span>
                </div>
                <div class="mt-1 h-2 rounded-full bg-neutral-100">
                  <div
                    class="h-full rounded-full bg-primary-500"
                    [style.width.%]="fila.porcentaje"
                  ></div>
                </div>
              </div>
            }
          </div>
        </mat-card>
      </div>

      <!-- Tabla de inyecciones recientes -->
      <mat-card appearance="outlined">
        <div class="flex items-center justify-between p-6 pb-0">
          <div class="text-lg font-medium">Inyecciones recientes</div>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">No.</th>
                <th class="px-6 py-2 font-normal">Fecha</th>
                <th class="px-6 py-2 font-normal">Hora</th>
                <th class="px-6 py-2 font-normal">Sala</th>
                <th class="px-6 py-2 font-normal">Protocolo</th>
                <th class="px-6 py-2 font-normal">Agente</th>
                <th class="px-6 py-2 font-normal">Volumen</th>
                <th class="px-6 py-2 font-normal">Presion</th>
                <th class="px-6 py-2 font-normal">EDA</th>
                <th class="px-6 py-2 font-normal">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (fila of inyecciones.value()?.content ?? []; track fila.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 text-neutral-500">#{{ fila.id }}</td>
                  <td class="px-6 py-2">{{ fila.fechaHoraInicio | date: 'dd-MM-yyyy' }}</td>
                  <td class="px-6 py-2">{{ fila.fechaHoraInicio | date: 'HH:mm' }}</td>
                  <td class="px-6 py-2">{{ fila.sala }}</td>
                  <td class="px-6 py-2">{{ fila.protocolo }}</td>
                  <td class="px-6 py-2">{{ fila.agentePrincipal }}</td>
                  <td class="px-6 py-2">{{ fila.volumenTotalMl }} ml</td>
                  <td class="px-6 py-2">
                    @if (fila.tieneSeriePresion) {
                      <button
                        matButton="text"
                        class="whitespace-nowrap"
                        (click)="abrirPresion(fila)"
                      >
                        {{ fila.presionMaximaPsi }} psi
                        <mat-icon svgIcon="chart-line" class="ml-1 size-4" />
                      </button>
                    } @else {
                      <span title="Sin datos de presion disponibles para esta inyeccion">
                        {{ fila.presionMaximaPsi ?? '—' }} psi
                      </span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    @if (fila.edaHabilitado) {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Habilitado</span>
                    } @else {
                      <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">Deshabilitado</span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    @if (fila.tieneAlertaEda) {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">
                        EDA fuera de rango
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">
                        {{ fila.estado }}
                      </span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="inyecciones.value()?.totalElements ?? 0"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPagina($event)"
        ></mat-paginator>
      </mat-card>
    </div>

    <!-- Dialog: grafica de presion vs. tiempo de una inyeccion -->
    <ng-template #dialogPresion>
      <div class="flex w-full max-w-2xl flex-col gap-y-3 p-6">
        <div class="text-lg font-semibold">
          Presion durante la inyeccion #{{ inyeccionSeleccionada()?.id }}
        </div>
        <div class="text-sm text-neutral-500">
          Max. {{ inyeccionSeleccionada()?.presionMaximaPsi }} psi &middot;
          Prom. {{ inyeccionSeleccionada()?.presionPromedioPsi }} psi
          @if (inyeccionSeleccionada()?.presionLimitePsi) {
            &middot; Limite {{ inyeccionSeleccionada()?.presionLimitePsi }} psi
          }
        </div>
        <apx-chart
          [chart]="presionChart().chart"
          [series]="presionChart().series"
          [xaxis]="presionChart().xaxis"
          [yaxis]="presionChart().yaxis"
          [dataLabels]="presionChart().dataLabels"
          [stroke]="presionChart().stroke"
          [grid]="presionChart().grid"
          [tooltip]="presionChart().tooltip"
        ></apx-chart>
        <div class="mt-2 flex justify-end">
          <button matButton="filled" (click)="dialogRef?.close()">Cerrar</button>
        </div>
      </div>
    </ng-template>
  `,
})
export default class ContrastInjectorDashboard {
  private api = inject(ContrastInjectorApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private theming = inject(Theming);
  private matDialog = inject(MatDialog);
  private router = inject(Router);
  dialogRef: MatDialogRef<unknown> | null = null;

  private readonly dialogPresionTpl = viewChild.required<TemplateRef<unknown>>('dialogPresion');

  constructor() {
    // Julio 2026: el usuario señalo que este dashboard no se actualizaba
    // solo cuando entraban inyecciones nuevas -- antes SOLO se refrescaba
    // al cambiar un filtro o al presionar "Actualizar" a mano. Se agrega
    // un sondeo cada 60s (solo en el navegador, nunca durante SSR) que
    // llama al mismo recargarTodo() del boton manual. Esto NO reemplaza
    // la sincronizacion real del inyector (que sigue corriendo cada 15
    // min en el backend, ver SincronizacionInyectorService) -- solo
    // asegura que esta pantalla refleje los datos ya sincronizados sin
    // que el usuario tenga que recargar la pagina o dar clic el mismo.
    if (this.esNavegador) {
      interval(60_000)
        .pipe(takeUntilDestroyed())
        .subscribe(() => this.recargarTodo());
    }
  }

  // Julio 2026: tarjetas KPI clickeables, a peticion explicita del
  // usuario, hacia el modulo dedicado mas cercano a cada concepto (ver
  // investigacion previa: "Inyecciones en el periodo" y "Contraste
  // utilizado" NO tienen a donde redirigir -- la unica vista de
  // inyecciones es la tabla que ya esta en esta misma pantalla, asi que
  // esas 2 tarjetas se quedan sin click).
  irAMermas() {
    const { desde, hasta } = this.rangoFechas();
    this.router.navigate(['/admin/insumos/mermas'], {
      queryParams: {
        fechaInicio: `${desde}T00:00:00`,
        fechaFin: `${hasta}T23:59:59`,
        salaId: this.salaId(),
        identificadorAnatomicoId: this.identificadorAnatomicoId(),
        agenteId: this.agenteId(),
        estado: this.estado(),
        soloConAlertaEda: this.soloAlertaEda() || undefined,
      },
    });
  }

  // Nota: el filtro "estado" del dashboard (COMPLETADA/ABORTADA/ERROR,
  // sobre Inyeccion) NO corresponde a nada en la pantalla de
  // extravasaciones (que filtra por estadoEda: SIN_REFERENCIA/EN_RANGO/
  // FUERA_DE_RANGO, sobre EventoExtravasacion) -- son conceptos
  // distintos, asi que no se manda. Lo que si corresponde 1:1: el rango
  // de fechas, y el chip "Solo alertas EDA" -> preseleccionar
  // estadoEda=FUERA_DE_RANGO alla (que es exactamente lo que ese chip
  // significa en este dashboard).
  irAExtravasaciones() {
    const { desde, hasta } = this.rangoFechas();
    this.router.navigate(['/admin/extravasaciones/alertas'], {
      queryParams: {
        desde,
        hasta,
        estadoEda: this.soloAlertaEda() ? 'FUERA_DE_RANGO' : undefined,
      },
    });
  }

  // Nota: /admin/mantenimiento es la pantalla de "Mantenimiento
  // predictivo" (riesgo de falla, calibracion) -- lista los mismos
  // inyectores pero con ese enfoque, no con un conteo activo/inactivo
  // puro. Es el destino mas cercano que existe hoy para "Inyectores
  // activos"; no hay una pantalla de inventario/estado de equipos aparte.
  irAMantenimiento() {
    this.router.navigate(['/admin/mantenimiento']);
  }

  // --- Estado de filtros (signals, atados directamente a los mat-select) ---
  // Default cambiado a "Ultimos 30 dias" a peticion del usuario (antes
  // era "hoy", que dejaba el dashboard casi vacio en sedes con poco
  // volumen diario).
  rango = signal<RangoPreset>('30d');
  salaId = signal<number | undefined>(undefined);
  identificadorAnatomicoId = signal<number | undefined>(undefined);
  agenteId = signal<number | undefined>(undefined);
  estado = signal<string | undefined>(undefined);
  soloAlertaEda = signal(false);
  pageIndex = signal(0);
  pageSize = signal(20);

  // Rango personalizado (solo se usan cuando rango() === 'personalizado')
  fechaDesdePersonalizada = signal(formatoISO(new Date()));
  fechaHastaPersonalizada = signal(formatoISO(new Date()));

  // El input type="date" puede disparar 'input' con el valor vacio
  // mientras el usuario borra/edita la fecha -- ignoramos esos eventos
  // para no lanzar una consulta al backend con fecha vacia (causaba
  // MethodArgumentTypeMismatchException por 'T00:00:00' sin fecha).
  protected onFechaDesdeInput(valor: string): void {
    if (valor) this.fechaDesdePersonalizada.set(valor);
  }

  protected onFechaHastaInput(valor: string): void {
    if (valor) this.fechaHastaPersonalizada.set(valor);
  }

  private rangoFechas = computed(() => {
    const preset = this.rango();
    if (preset === 'personalizado') {
      const hoyIso = formatoISO(new Date());
      const desde = this.fechaDesdePersonalizada();
      const hasta = this.fechaHastaPersonalizada();
      // si el usuario aun no completa ambas fechas (o el campo quedo
      // momentaneamente vacio mientras edita), no disparamos una
      // consulta con fecha vacia -- se usa hoy como respaldo final
      return { desde: desde || hasta || hoyIso, hasta: hasta || desde || hoyIso };
    }
    return calcularRango(preset);
  });

  // --- Catalogos para los <mat-select> (se cargan una sola vez) ---
  salas = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getSalas(), []) });
  identificadores = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getIdentificadoresAnatomicos(), []) });
  agentes = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getAgentesContraste(), []) });

  // --- Datos del dashboard, recalculados cuando cambia el rango de fechas ---
  kpis = rxResource({
    params: () => this.rangoFechas(),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.getKpis(params.desde, params.hasta),
        {
          inyeccionesEnPeriodo: 0,
          volumenTotalMl: 0,
          volumenPromedioMl: 0,
          alertasEdaFueraDeRango: 0,
          inyectoresActivos: 0,
          inyectoresTotales: 0,
        }
      ),
  });

  usoContraste = rxResource({
    params: () => this.rangoFechas(),
    stream: ({ params }) => ssrSeguro(this.esNavegador, () => this.api.getUsoContraste(params.desde, params.hasta), []),
  });

  distribucion = rxResource({
    params: () => this.rangoFechas(),
    stream: ({ params }) => ssrSeguro(this.esNavegador, () => this.api.getDistribucionProtocolo(params.desde, params.hasta), []),
  });

  // --- Tabla, recalculada cuando cambia cualquier filtro ---
  inyecciones = rxResource({
    params: () => ({
      ...this.rangoFechas(),
      salaId: this.salaId(),
      identificadorAnatomicoId: this.identificadorAnatomicoId(),
      agenteId: this.agenteId(),
      estado: this.estado(),
      soloConAlertaEda: this.soloAlertaEda() || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () =>
          this.api.getInyecciones({
            fechaInicio: `${params.desde}T00:00:00`,
            fechaFin: `${params.hasta}T23:59:59`,
            salaId: params.salaId,
            identificadorAnatomicoId: params.identificadorAnatomicoId,
            agenteId: params.agenteId,
            estado: params.estado,
            soloConAlertaEda: params.soloConAlertaEda,
            page: params.page,
            size: params.size,
          }),
        { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }
      ),
  });

  // Merma julio 2026: misma barra de filtros que "inyecciones" (sin
  // pagina/tamanio, es un agregado), pegando a /resumen-filtrado en vez
  // de /inyecciones -- alimenta la tarjeta "Merma con estos filtros".
  mermaFiltrada = rxResource({
    params: () => ({
      ...this.rangoFechas(),
      salaId: this.salaId(),
      identificadorAnatomicoId: this.identificadorAnatomicoId(),
      agenteId: this.agenteId(),
      estado: this.estado(),
      soloConAlertaEda: this.soloAlertaEda() || undefined,
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () =>
          this.api.getMermaFiltrada({
            fechaInicio: `${params.desde}T00:00:00`,
            fechaFin: `${params.hasta}T23:59:59`,
            salaId: params.salaId,
            identificadorAnatomicoId: params.identificadorAnatomicoId,
            agenteId: params.agenteId,
            estado: params.estado,
            soloConAlertaEda: params.soloConAlertaEda,
          }),
        {
          volumenProgramadoMl: 0,
          volumenRealMl: 0,
          volumenMermaMl: 0,
          porcentajeMerma: 0,
          volumenMermaPeriodoAnteriorMl: null,
          variacionPorcentual: null,
        }
      ),
  });

  usoContrasteChart = computed(() => {
    const datos = this.usoContraste.value() ?? [];
    const isDark = this.theming.isDark();

    const chart: ApexChart = { type: 'bar', fontFamily: 'inherit', toolbar: { show: false } };
    const series: ApexAxisChartSeries = [
      { name: 'Volumen (ml)', data: datos.map((d) => d.volumenMl) },
    ];
    const xaxis: ApexXAxis = { categories: datos.map((d) => formatoFechaCorta(d.fecha)) };
    const yaxis: ApexYAxis = {};
    const dataLabels: ApexDataLabels = { enabled: false };
    const fill: ApexFill = { opacity: 1 };
    const grid: ApexGrid = { borderColor: isDark ? '#333' : '#eee' };
    const stroke: ApexStroke = { width: 0 };
    const tooltip: ApexTooltip = { theme: isDark ? 'dark' : 'light' };

    return { chart, series, xaxis, yaxis, dataLabels, fill, grid, stroke, tooltip };
  });

  onPagina(evento: PageEvent) {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
  }

  // --- Dialog: grafica de presion vs. tiempo (boton "Ver presion") ---
  protected inyeccionSeleccionada = signal<InyeccionResumen | null>(null);
  protected serieePresionDatos = signal<PuntoPresion[]>([]);

  abrirPresion(fila: InyeccionResumen) {
    this.inyeccionSeleccionada.set(fila);
    this.api.getSeriePresion(fila.id).subscribe((datos) => this.serieePresionDatos.set(datos));
    this.dialogRef = this.matDialog.open(this.dialogPresionTpl(), { panelClass: 'w-full max-w-2xl'.split(' ') });
  }

  presionChart = computed(() => {
    const isDark = this.theming.isDark();
    const datos = this.serieePresionDatos();

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

  recargarTodo() {
    this.kpis.reload();
    this.usoContraste.reload();
    this.distribucion.reload();
    this.inyecciones.reload();
    // Bug corregido julio 2026: faltaba refrescar la tarjeta "Merma con
    // estos filtros" -- se quedaba desactualizada aun despues de dar
    // clic en "Actualizar" (se agrego despues de escribir este metodo la
    // primera vez y no se incluyo aqui).
    this.mermaFiltrada.reload();
  }
}
