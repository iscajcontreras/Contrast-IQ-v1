import { Component, PLATFORM_ID, TemplateRef, inject, isDevMode, signal, viewChild } from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  EventoExtravasacion,
  ExtravasacionesApiService,
} from '@/app/domains/admin/modules/extravasaciones/data/extravasaciones-api.service';

// Alertas de extravasacion (EDA fuera de rango): julio 2026, modulo
// nuevo -- el backend (ExtravasacionController/ExtravasacionService) ya
// existia desde antes, pero ninguna pantalla lo consumia; la unica
// senal visible era el conteo en la tarjeta "Alertas de extravasacion"
// del dashboard de Inyecciones y el chip "Solo alertas EDA" de ese mismo
// dashboard. Esta pantalla es a donde esa tarjeta ahora redirige.
@Component({
  selector: 'alertas-extravasacion',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCard,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
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
            Alertas de extravasacion
          </div>
          <div class="text-neutral-500">
            Eventos EDA (deteccion de extravasacion) pendientes o ya revisados clinicamente.
          </div>
        </div>
        <div class="flex-auto"></div>
        <button matButton="outlined" (click)="actualizar.set(actualizar() + 1)">
          <mat-icon svgIcon="refresh-cw" />
          Actualizar
        </button>
      </div>

      <!-- Filtros -->
      <mat-card class="flex flex-wrap items-end gap-3 p-4" appearance="outlined">
        <mat-form-field class="w-48" subscriptSizing="dynamic">
          <mat-label>Desde</mat-label>
          <input matInput type="date" [value]="desde()" (change)="desde.set($any($event.target).value)" />
        </mat-form-field>
        <mat-form-field class="w-48" subscriptSizing="dynamic">
          <mat-label>Hasta</mat-label>
          <input matInput type="date" [value]="hasta()" (change)="hasta.set($any($event.target).value)" />
        </mat-form-field>

        <mat-form-field class="w-56" subscriptSizing="dynamic">
          <mat-label>Estado EDA</mat-label>
          <mat-select [(value)]="estadoEda" placeholder="Todos">
            <mat-option [value]="undefined">Todos</mat-option>
            <mat-option value="FUERA_DE_RANGO">Fuera de rango</mat-option>
            <mat-option value="EN_RANGO">En rango</mat-option>
            <mat-option value="SIN_REFERENCIA">Sin referencia</mat-option>
          </mat-select>
        </mat-form-field>

        <div class="flex items-center">
          <mat-chip-listbox>
            <mat-chip-option
              [selected]="soloPendientes()"
              (click)="soloPendientes.set(!soloPendientes())"
            >
              Solo pendientes de revisar
            </mat-chip-option>
          </mat-chip-listbox>
        </div>
      </mat-card>

      <!-- Tabla -->
      <mat-card appearance="outlined">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Fecha</th>
                <th class="px-6 py-2 font-normal">Sala</th>
                <th class="px-6 py-2 font-normal">Inyector</th>
                <th class="px-6 py-2 font-normal">Estado EDA</th>
                <th class="px-6 py-2 font-normal">Revision</th>
                <th class="px-6 py-2 font-normal"></th>
              </tr>
            </thead>
            <tbody>
              @for (fila of eventos.value()?.content ?? []; track fila.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2">{{ fila.fechaHora | date: 'dd-MM-yyyy HH:mm' }}</td>
                  <td class="px-6 py-2">{{ fila.sala }}</td>
                  <td class="px-6 py-2">{{ fila.inyector }}</td>
                  <td class="px-6 py-2">
                    @if (fila.estadoEda === 'FUERA_DE_RANGO') {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Fuera de rango</span>
                    } @else if (fila.estadoEda === 'EN_RANGO') {
                      <span class="rounded-full bg-green-50 px-2 py-0.5 text-green-700">En rango</span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Sin referencia</span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    @if (fila.revisado) {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700" [title]="fila.accionTomada ?? ''">
                        Revisado{{ fila.accionTomada ? ': ' + fila.accionTomada : '' }}
                      </span>
                    } @else {
                      <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">Pendiente</span>
                    }
                  </td>
                  <td class="px-6 py-2 text-right">
                    @if (!fila.revisado) {
                      <button matButton="text" (click)="abrirRevision(fila)">Revisar</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" class="py-4 text-center text-neutral-500">
                    Sin eventos en el periodo/filtros seleccionados.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="eventos.value()?.totalElements ?? 0"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPagina($event)"
        ></mat-paginator>
      </mat-card>
    </div>

    <!-- Dialog: marcar como revisado -->
    <ng-template #dialogRevision>
      <div class="flex max-h-[85vh] w-full max-w-md flex-col gap-4 p-6">
        <div class="text-lg font-medium">Revisar alerta de extravasacion</div>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Accion tomada</mat-label>
          <input matInput [(ngModel)]="accionTomada" maxlength="255" placeholder="Ej. Se aplico frio local, sin secuelas" />
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Notas (opcional)</mat-label>
          <textarea matInput [(ngModel)]="notas" maxlength="2000" rows="4"></textarea>
        </mat-form-field>

        <div class="flex justify-end gap-2">
          <button matButton (click)="dialogRef?.close()">Cancelar</button>
          <button matButton="filled" [disabled]="guardando()" (click)="confirmarRevision()">
            Guardar
          </button>
        </div>
      </div>
    </ng-template>
  `,
})
export default class AlertasExtravasacion {
  private api = inject(ExtravasacionesApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private matDialog = inject(MatDialog);
  private route = inject(ActivatedRoute);

  private readonly dialogRevisionTpl = viewChild.required<TemplateRef<unknown>>('dialogRevision');
  dialogRef: MatDialogRef<unknown> | null = null;

  // Rango por defecto: ultimos 30 dias (incluyendo hoy) -- mismo criterio
  // que Merma de insumos.
  private hoy = new Date();
  private hace30dias = new Date(this.hoy.getTime() - 29 * 24 * 60 * 60 * 1000);

  // Julio 2026: si se llega desde la tarjeta "Alertas de extravasacion"
  // del dashboard de Inyecciones, se usan el rango de fechas y el
  // estadoEda que trae la URL (ver contrast-injector.ts:irAExtravasaciones
  // -- el chip "Solo alertas EDA" de ese dashboard se traduce aqui a
  // estadoEda=FUERA_DE_RANGO).
  private queryParams = this.route.snapshot.queryParamMap;

  desde = signal(this.queryParams.get('desde') ?? this.formatearFecha(this.hace30dias));
  hasta = signal(this.queryParams.get('hasta') ?? this.formatearFecha(this.hoy));
  estadoEda = signal<string | undefined>(this.queryParams.get('estadoEda') ?? undefined);
  soloPendientes = signal(false);
  pageIndex = signal(0);
  pageSize = signal(20);
  actualizar = signal(0);

  eventoSeleccionado: EventoExtravasacion | null = null;
  accionTomada = '';
  notas = '';
  guardando = signal(false);

  eventos = rxResource({
    params: () => ({
      desde: this.desde(),
      hasta: this.hasta(),
      estadoEda: this.estadoEda(),
      soloPendientes: this.soloPendientes(),
      page: this.pageIndex(),
      size: this.pageSize(),
      _t: this.actualizar(),
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () =>
          this.api.buscar({
            desde: `${params.desde}T00:00:00`,
            hasta: `${params.hasta}T23:59:59`,
            estadoEda: params.estadoEda,
            revisado: params.soloPendientes ? false : undefined,
            page: params.page,
            size: params.size,
          }),
        { content: [], totalElements: 0 }
      ),
  });

  onPagina(evento: PageEvent) {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
  }

  abrirRevision(fila: EventoExtravasacion) {
    this.eventoSeleccionado = fila;
    this.accionTomada = '';
    this.notas = '';
    this.dialogRef = this.matDialog.open(this.dialogRevisionTpl(), { panelClass: 'w-full max-w-md'.split(' ') });
  }

  confirmarRevision() {
    if (!this.eventoSeleccionado) return;
    this.guardando.set(true);
    this.api.revisar(this.eventoSeleccionado.id, { accionTomada: this.accionTomada, notas: this.notas }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.dialogRef?.close();
        this.actualizar.set(this.actualizar() + 1);
      },
      error: (error) => {
        this.guardando.set(false);
        if (isDevMode()) console.error('No se pudo guardar la revision', error);
      },
    });
  }

  private formatearFecha(fecha: Date): string {
    return fecha.toISOString().slice(0, 10);
  }
}
