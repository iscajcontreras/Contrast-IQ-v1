import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import { MermasApiService } from '@/app/domains/admin/modules/insumos/data/lotes-api.service';

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
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
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

      <!-- Filtro de fechas -->
      <mat-card class="flex flex-wrap items-end gap-3 p-4" appearance="outlined">
        <mat-form-field class="w-48">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [value]="desde()" (change)="desde.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field class="w-48">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [value]="hasta()" (change)="hasta.set($any($event.target).value)" />
        </mat-form-field>
      </mat-card>

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
          Detalle por inyeccion (ordenado por merma, casos abortados resaltados)
        </div>
        <div class="overflow-x-auto p-4">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-2 py-2 font-normal">Fecha</th>
                <th class="px-2 py-2 font-normal">Paciente</th>
                <th class="px-2 py-2 font-normal">Sede / Sala</th>
                <th class="px-2 py-2 font-normal">Estado</th>
                <th class="px-2 py-2 font-normal">Programado</th>
                <th class="px-2 py-2 font-normal">Real</th>
                <th class="px-2 py-2 font-normal">Merma</th>
                <th class="px-2 py-2 font-normal">%</th>
              </tr>
            </thead>
            <tbody>
              @for (fila of detalle.value()?.content ?? []; track fila.inyeccionId) {
                <tr
                  class="border-t border-neutral-100"
                  [class.bg-red-50]="fila.estado === 'ABORTADA' || fila.estado === 'ERROR'"
                >
                  <td class="px-2 py-2">{{ fila.fechaHoraInicio | date: 'short' }}</td>
                  <td class="px-2 py-2">{{ fila.paciente ?? '—' }} <span class="text-neutral-400">{{ fila.numeroExpediente }}</span></td>
                  <td class="px-2 py-2">{{ fila.sede }} / {{ fila.sala }}</td>
                  <td class="px-2 py-2">
                    @if (fila.estado === 'ABORTADA' || fila.estado === 'ERROR') {
                      <span class="rounded-full bg-red-100 px-2 py-0.5 text-red-700">
                        {{ fila.estado }}{{ fila.motivoAborto ? ' — ' + fila.motivoAborto : '' }}
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">{{ fila.estado }}</span>
                    }
                  </td>
                  <td class="px-2 py-2">{{ fila.volumenProgramadoMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.volumenRealMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2 text-red-600">{{ fila.volumenMermaMl | number: '1.0-1' }} ml</td>
                  <td class="px-2 py-2">{{ fila.porcentajeMerma }}%</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" class="py-4 text-center text-neutral-500">Sin datos en el periodo seleccionado.</td>
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
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  // Rango por defecto: ultimos 30 dias (incluyendo hoy).
  private hoy = new Date();
  private hace30dias = new Date(this.hoy.getTime() - 29 * 24 * 60 * 60 * 1000);

  desde = signal(this.formatearFecha(this.hace30dias));
  hasta = signal(this.formatearFecha(this.hoy));
  pageIndex = signal(0);
  pageSize = signal(20);
  // Se incrementa manualmente para forzar un reload sin cambiar filtros
  // (ej. boton "Actualizar" si los datos se movieron del lado del backend).
  actualizar = signal(0);

  resumen = rxResource({
    params: () => ({ desde: this.desde(), hasta: this.hasta(), _t: this.actualizar() }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.resumen(params.desde, params.hasta),
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
}
