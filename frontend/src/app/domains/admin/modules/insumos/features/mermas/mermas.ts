import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import { LotesApiService, MermasApiService } from '@/app/domains/admin/modules/insumos/data/lotes-api.service';

// Merma de insumos (contraste + solucion salina): responde a la
// observacion de un stakeholder en reunion -- "no vi en ningun lado
// donde se muestran las mermas" -- con las 4 vistas pedidas:
//   1) KPI agregado del periodo, con tendencia vs. el periodo anterior.
//   2) Merma por sede (para comparar cual sede desperdicia mas).
//   3) Merma por tipo de insumo (contraste vs. solucion salina, por marca).
//   4) Tabla detallada por inyeccion individual (para investigar casos
//      puntuales, ej. procedimientos abortados), ordenada por merma
//      descendente y resaltando las filas ABORTADA/ERROR.
@Component({
  selector: 'mermas-insumos',
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
  ],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <!-- Header -->
      <div class="flex items-center justify-between gap-x-3">
        <div class="flex flex-col gap-y-0.5">
          <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
            Merma de insumos
          </div>
          <div class="text-neutral-500">
            Volumen programado vs. realmente inyectado de contraste y solucion salina.
          </div>
        </div>
        <div class="flex-auto"></div>
        <button matButton="outlined" (click)="actualizar.set(actualizar() + 1)">
          <mat-icon svgIcon="refresh-cw" />
          Actualizar
        </button>
      </div>

      <!-- Filtros: 2 filas fijas, con el nombre de que filtra cada campo. -->
      <mat-card class="flex flex-col gap-3 p-4" appearance="outlined">
        <!-- Fila 1: rango de fechas + ubicacion del procedimiento -->
        <div class="flex flex-wrap items-end gap-3">
          <mat-form-field class="w-48" subscriptSizing="dynamic">
            <mat-label>Fecha desde</mat-label>
            <input matInput type="date" [value]="desde()" (change)="desde.set($any($event.target).value)" />
          </mat-form-field>
          <mat-form-field class="w-48" subscriptSizing="dynamic">
            <mat-label>Fecha hasta</mat-label>
            <input matInput type="date" [value]="hasta()" (change)="hasta.set($any($event.target).value)" />
          </mat-form-field>

          <mat-form-field class="w-48" subscriptSizing="dynamic">
            <mat-label>Sala de aplicacion</mat-label>
            <mat-select [(value)]="salaId" placeholder="Todas las salas">
              <mat-option [value]="undefined">Todas las salas</mat-option>
              @for (sala of salas.value(); track sala.id) {
                <mat-option [value]="sala.id">{{ sala.etiqueta }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field class="w-56" subscriptSizing="dynamic">
            <mat-label>Region anatomica inyectada</mat-label>
            <mat-select [(value)]="identificadorAnatomicoId" placeholder="Todas las regiones">
              <mat-option [value]="undefined">Todas las regiones</mat-option>
              @for (id of identificadores.value(); track id.id) {
                <mat-option [value]="id.id">{{ id.etiqueta }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>

        <!-- Fila 2: insumo utilizado + estado de la inyeccion + alertas -->
        <div class="flex flex-wrap items-end gap-3">
          <mat-form-field class="w-56" subscriptSizing="dynamic">
            <mat-label>Agente de contraste utilizado</mat-label>
            <mat-select [(value)]="agenteId" placeholder="Todos los agentes">
              <mat-option [value]="undefined">Todos los agentes</mat-option>
              @for (agente of agentes.value(); track agente.id) {
                <mat-option [value]="agente.id">{{ agente.etiqueta }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field class="w-48" subscriptSizing="dynamic">
            <mat-label>Estado de la inyeccion</mat-label>
            <mat-select [(value)]="estado" placeholder="Todos los estados">
              <mat-option [value]="undefined">Todos los estados</mat-option>
              <mat-option value="COMPLETADA">Completada</mat-option>
              <mat-option value="ABORTADA">Abortada</mat-option>
              <mat-option value="ERROR">Error</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="flex flex-col gap-y-1">
            <span class="text-xs text-neutral-500">Alertas de seguridad</span>
            <mat-chip-listbox>
              <mat-chip-option [selected]="soloAlertaEda()" (click)="soloAlertaEda.set(!soloAlertaEda())">
                Solo con alerta de extravasacion (EDA)
              </mat-chip-option>
            </mat-chip-listbox>
          </div>
        </div>
      </mat-card>

      @if (hayFiltrosExtra()) {
        <div class="rounded-lg bg-blue-50 px-4 py-2 text-sm text-blue-700">
          El resumen de arriba ya aplica sala/identificador/agente/estado/alertas EDA (llegaste con estos filtros
          desde el dashboard de Inyecciones). Los desgloses por sede, por insumo y por inyeccion de abajo, por ahora,
          solo aplican el rango de fechas.
        </div>
      }

      <!-- 1) KPI agregado con tendencia -->
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <mat-card class="flex flex-col gap-y-1 p-5" appearance="outlined">
          <div class="text-sm text-neutral-500">Volumen programado</div>
          <div class="text-2xl font-semibold tracking-tight">
            {{ (resumen.value()?.volumenProgramadoMl ?? 0) | number: '1.0-1' }} ml
          </div>
        </mat-card>

        <mat-card class="flex flex-col gap-y-1 p-5" appearance="outlined">
          <div class="text-sm text-neutral-500">Volumen realmente inyectado</div>
          <div class="text-2xl font-semibold tracking-tight">
            {{ (resumen.value()?.volumenRealMl ?? 0) | number: '1.0-1' }} ml
          </div>
        </mat-card>

        <mat-card class="flex flex-col gap-y-1 p-5" appearance="outlined">
          <div class="text-sm text-neutral-500">Merma del periodo</div>
          <div class="flex items-baseline gap-x-2">
            <div class="text-2xl font-semibold tracking-tight text-red-600">
              {{ (resumen.value()?.volumenMermaMl ?? 0) | number: '1.0-1' }} ml
            </div>
            <div class="text-neutral-500">
              ({{ resumen.value()?.porcentajeMerma ?? 0 }}%)
            </div>
          </div>
          @if (resumen.value()?.variacionPorcentual !== null && resumen.value()?.variacionPorcentual !== undefined) {
            @if ((resumen.value()?.variacionPorcentual ?? 0) > 0) {
              <div class="text-sm text-red-600">
                ▲ {{ resumen.value()?.variacionPorcentual }}% vs. periodo anterior
              </div>
            } @else {
              <div class="text-sm text-green-600">
                ▼ {{ resumen.value()?.variacionPorcentual }}% vs. periodo anterior
              </div>
            }
          } @else {
            <div class="text-sm text-neutral-400">Sin punto de comparacion en el periodo anterior.</div>
          }
        </mat-card>
      </div>

      <!-- 2) Merma por sede -->
      <mat-card appearance="outlined">
        <div class="p-4 pb-0 text-base font-semibold">Merma por sede</div>
        <div class="overflow-x-auto p-4">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-2 py-2 font-normal">Sede</th>
                <th class="px-2 py-2 font-normal">Programado</th>
                <th class="px-2 py-2 font-normal">Real</th>
                <th class="px-2 py-2 font-normal">Merma</th>
                <th class="px-2 py-2 font-normal">%</th>
              </tr>
            </thead>
            <tbody>
              @for (fila of porSede.value() ?? []; track fila.sedeId) {
                <tr class="border-t border-neutral-100">
                  <td class="px-2 py-2 font-medium">{{ fila.sede }}</td>
                  <td class="px-2 py-2">{{ fila.volumenProgramadoMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.volumenRealMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 text-red-600">{{ fila.volumenMermaMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.porcentajeMerma }}%</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="py-4 text-center text-neutral-500">Sin datos en el periodo seleccionado.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>

      <!-- 3) Merma por tipo de insumo -->
      <mat-card appearance="outlined">
        <div class="p-4 pb-0 text-base font-semibold">Merma por tipo de insumo</div>
        <div class="overflow-x-auto p-4">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-2 py-2 font-normal">Insumo</th>
                <th class="px-2 py-2 font-normal">Tipo</th>
                <th class="px-2 py-2 font-normal">Fabricante</th>
                <th class="px-2 py-2 font-normal">Programado</th>
                <th class="px-2 py-2 font-normal">Real</th>
                <th class="px-2 py-2 font-normal">Merma</th>
                <th class="px-2 py-2 font-normal">%</th>
              </tr>
            </thead>
            <tbody>
              @for (fila of porInsumo.value() ?? []; track fila.agenteId) {
                <tr class="border-t border-neutral-100">
                  <td class="px-2 py-2 font-medium">{{ fila.nombreComercial }}</td>
                  <td class="px-2 py-2">
                    @if (fila.tipo === 'CONTRASTE') {
                      <span class="rounded-full bg-blue-50 px-2 py-0.5 text-blue-700">Contraste</span>
                    } @else {
                      <span class="rounded-full bg-cyan-50 px-2 py-0.5 text-cyan-700">Solucion salina</span>
                    }
                  </td>
                  <td class="px-2 py-2">{{ fila.fabricante ?? '—' }}</td>
                  <td class="px-2 py-2">{{ fila.volumenProgramadoMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.volumenRealMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 text-red-600">{{ fila.volumenMermaMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.porcentajeMerma }}%</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="py-4 text-center text-neutral-500">Sin datos en el periodo seleccionado.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>

      <!-- 4) Detalle por inyeccion individual -->
      <mat-card appearance="outlined">
        <div class="p-4 pb-0 text-base font-semibold">
          Detalle por inyeccion (mas reciente primero, casos abortados resaltados)
        </div>
        <div class="overflow-x-auto p-4">
          <table class="w-full min-w-max text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-2 py-2 font-normal whitespace-nowrap">#</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Fecha</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Paciente</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Sede / Sala</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Estado</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Programado</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Real</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">Merma</th>
                <th class="px-2 py-2 font-normal whitespace-nowrap">%</th>
              </tr>
            </thead>
            <tbody>
              @for (fila of detalle.value()?.content ?? []; track fila.inyeccionId) {
                <tr
                  class="border-t border-neutral-100"
                  [class.bg-red-50]="fila.estado === 'ABORTADA' || fila.estado === 'ERROR'"
                >
                  <td class="px-2 py-2 whitespace-nowrap">
                    <a
                      [routerLink]="['/admin', 'paciente', 'inyeccion', fila.inyeccionId]"
                      class="text-primary-600 hover:underline"
                    >
                      #{{ fila.inyeccionId }}
                    </a>
                  </td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.fechaHoraInicio | date: 'short' }}</td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.paciente ?? '—' }} <span class="text-neutral-400">{{ fila.numeroExpediente }}</span></td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.sede }} / {{ fila.sala }}</td>
                  <td class="px-2 py-2 whitespace-nowrap">
                    @if (fila.estado === 'ABORTADA' || fila.estado === 'ERROR') {
                      <span class="rounded-full bg-red-100 px-2 py-0.5 text-red-700">
                        {{ fila.estado }}{{ fila.motivoAborto ? ' — ' + fila.motivoAborto : '' }}
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">{{ fila.estado }}</span>
                    }
                  </td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.volumenProgramadoMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.volumenRealMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 whitespace-nowrap text-red-600">{{ fila.volumenMermaMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 whitespace-nowrap">{{ fila.porcentajeMerma }}%</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="9" class="py-4 text-center text-neutral-500">Sin datos en el periodo seleccionado.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="detalle.value()?.totalElements ?? 0"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPagina($event)"
        ></mat-paginator>
      </mat-card>
    </div>
  `,
})
export default class MermasInsumos {
  private api = inject(MermasApiService);
  private lotesApi = inject(LotesApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private route = inject(ActivatedRoute);

  // Rango por defecto: ultimos 30 dias (incluyendo hoy) -- salvo que se
  // llegue desde el dashboard de Inyecciones con fechaInicio/fechaFin en
  // la URL (tarjeta "Merma con estos filtros"), en cuyo caso se usan esas.
  private hoy = new Date();
  private hace30dias = new Date(this.hoy.getTime() - 29 * 24 * 60 * 60 * 1000);
  private queryParams = this.route.snapshot.queryParamMap;

  desde = signal(this.queryParams.get('fechaInicio')?.slice(0, 10) ?? this.formatearFecha(this.hace30dias));
  hasta = signal(this.queryParams.get('fechaFin')?.slice(0, 10) ?? this.formatearFecha(this.hoy));

  // Julio 2026: filtros extra (mismos que la barra del dashboard de
  // Inyecciones de contraste), sembrados desde query params cuando se
  // llega via la tarjeta "Merma con estos filtros" -- ver hayFiltrosExtra().
  salaId = signal(this.numeroOUndefined(this.queryParams.get('salaId')));
  identificadorAnatomicoId = signal(this.numeroOUndefined(this.queryParams.get('identificadorAnatomicoId')));
  agenteId = signal(this.numeroOUndefined(this.queryParams.get('agenteId')));
  estado = signal(this.queryParams.get('estado') ?? undefined);
  soloAlertaEda = signal(this.queryParams.get('soloConAlertaEda') === 'true');

  pageIndex = signal(0);
  pageSize = signal(20);
  // Se incrementa manualmente para forzar un reload sin cambiar filtros
  // (ej. boton "Actualizar" si los datos se movieron del lado del backend).
  actualizar = signal(0);

  salas = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getSalas(), []) });
  identificadores = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getIdentificadoresAnatomicos(), []) });
  agentes = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.lotesApi.getAgentes(), []) });

  hayFiltrosExtra(): boolean {
    return (
      this.salaId() !== undefined ||
      this.identificadorAnatomicoId() !== undefined ||
      this.agenteId() !== undefined ||
      this.estado() !== undefined ||
      this.soloAlertaEda()
    );
  }

  resumen = rxResource({
    params: () => ({
      desde: this.desde(),
      hasta: this.hasta(),
      salaId: this.salaId(),
      identificadorAnatomicoId: this.identificadorAnatomicoId(),
      agenteId: this.agenteId(),
      estado: this.estado(),
      soloConAlertaEda: this.soloAlertaEda() || undefined,
      _t: this.actualizar(),
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () =>
          this.hayFiltrosExtra()
            ? this.api.resumenFiltrado({
                fechaInicio: `${params.desde}T00:00:00`,
                fechaFin: `${params.hasta}T23:59:59`,
                salaId: params.salaId,
                identificadorAnatomicoId: params.identificadorAnatomicoId,
                agenteId: params.agenteId,
                estado: params.estado,
                soloConAlertaEda: params.soloConAlertaEda,
              })
            : this.api.resumen(params.desde, params.hasta),
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

  porSede = rxResource({
    params: () => ({ desde: this.desde(), hasta: this.hasta(), _t: this.actualizar() }),
    stream: ({ params }) =>
      ssrSeguro(this.esNavegador, () => this.api.porSede(params.desde, params.hasta), []),
  });

  porInsumo = rxResource({
    params: () => ({ desde: this.desde(), hasta: this.hasta(), _t: this.actualizar() }),
    stream: ({ params }) =>
      ssrSeguro(this.esNavegador, () => this.api.porInsumo(params.desde, params.hasta), []),
  });

  detalle = rxResource({
    params: () => ({
      desde: this.desde(),
      hasta: this.hasta(),
      page: this.pageIndex(),
      size: this.pageSize(),
      _t: this.actualizar(),
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.porInyeccion(params.desde, params.hasta, params.page, params.size),
        { content: [], totalElements: 0 }
      ),
  });

  onPagina(evento: PageEvent) {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
  }

  private formatearFecha(fecha: Date): string {
    return fecha.toISOString().slice(0, 10);
  }

  private numeroOUndefined(valor: string | null): number | undefined {
    if (!valor) return undefined;
    const n = Number(valor);
    return Number.isFinite(n) ? n : undefined;
  }
}
