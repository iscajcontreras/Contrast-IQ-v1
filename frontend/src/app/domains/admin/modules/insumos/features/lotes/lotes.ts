import {
  Component,
  TemplateRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelect } from '@angular/material/select';
import { MatOption } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { firstValueFrom, of } from 'rxjs';
import {
  LotesApiService,
  Lote,
  TrazabilidadLote,
} from '@/app/domains/admin/modules/insumos/data/lotes-api.service';

@Component({
  selector: 'lotes-contraste',
  imports: [
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
    FormField,
  ],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <!-- Header -->
      <div class="flex items-center justify-between gap-x-3">
        <div class="flex flex-col gap-y-0.5">
          <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
            Trazabilidad de insumos
          </div>
          <div class="text-neutral-500">
            Lotes de agente de contraste recibidos y su trazabilidad por paciente.
          </div>
        </div>
        <div class="flex-auto"></div>
        <button matButton="filled" (click)="abrirCrear()">
          <mat-icon svgIcon="plus" />
          Registrar lote
        </button>
      </div>

      <!-- Filtros -->
      <mat-card class="flex flex-wrap items-end gap-3 p-4" appearance="outlined">
        <mat-form-field class="w-52">
          <mat-select [(value)]="sedeId" placeholder="Todas las sedes">
            <mat-option [value]="undefined">Todas las sedes</mat-option>
            @for (sede of sedes.value(); track sede.id) {
              <mat-option [value]="sede.id">{{ sede.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-52">
          <mat-select [(value)]="agenteId" placeholder="Todos los agentes">
            <mat-option [value]="undefined">Todos los agentes</mat-option>
            @for (agente of agentes.value(); track agente.id) {
              <mat-option [value]="agente.id">{{ agente.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-chip-listbox class="ml-auto">
          <mat-chip-option
            [selected]="proximosACaducar()"
            (click)="proximosACaducar.set(!proximosACaducar())"
          >
            Proximos a caducar (30 dias)
          </mat-chip-option>
        </mat-chip-listbox>
      </mat-card>

      <!-- Tabla -->
      <mat-card appearance="outlined">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Lote</th>
                <th class="px-6 py-2 font-normal">Agente</th>
                <th class="px-6 py-2 font-normal">Sede</th>
                <th class="px-6 py-2 font-normal">Cantidad</th>
                <th class="px-6 py-2 font-normal">Caducidad</th>
                <th class="px-6 py-2 font-normal">Estado</th>
                <th class="px-6 py-2 font-normal"></th>
              </tr>
            </thead>
            <tbody>
              @for (l of lotes.value()?.content ?? []; track l.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 font-medium">{{ l.numeroLote }}</td>
                  <td class="px-6 py-2">{{ l.agente }}</td>
                  <td class="px-6 py-2">{{ l.sede }}</td>
                  <td class="px-6 py-2">{{ l.cantidadMl }} ml</td>
                  <td class="px-6 py-2">{{ l.fechaCaducidad }}</td>
                  <td class="px-6 py-2">
                    @if (l.vencido) {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Vencido</span>
                    } @else if (l.diasParaCaducar <= 30) {
                      <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">
                        Vence en {{ l.diasParaCaducar }} d
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Vigente</span>
                    }
                  </td>
                  <td class="px-6 py-2 text-right whitespace-nowrap">
                    <button matButton="text" (click)="abrirTrazabilidad(l)">
                      Ver trazabilidad
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="lotes.value()?.totalElements ?? 0"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPagina($event)"
        ></mat-paginator>
      </mat-card>
    </div>

    <!-- Dialog: registrar lote -->
    <ng-template #dialogCrear>
      <div class="flex w-full max-w-md flex-col gap-y-4 p-6">
        <div class="text-lg font-semibold">Registrar lote de contraste</div>

        <mat-form-field class="w-full">
          <mat-label>Agente de contraste</mat-label>
          <mat-select [formField]="loteForm.agenteId">
            @for (agente of agentes.value(); track agente.id) {
              <mat-option [value]="agente.id">{{ agente.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Sede</mat-label>
          <mat-select [formField]="loteForm.sedeId">
            @for (sede of sedes.value(); track sede.id) {
              <mat-option [value]="sede.id">{{ sede.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Numero de lote</mat-label>
          <input matInput [formField]="loteForm.numeroLote" />
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Fecha de caducidad</mat-label>
          <input matInput type="date" [formField]="loteForm.fechaCaducidad" />
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Cantidad recibida (ml)</mat-label>
          <input matInput type="number" [formField]="loteForm.cantidadMl" />
        </mat-form-field>

        @if (error()) {
          <div class="text-sm text-red-600">{{ error() }}</div>
        }

        <div class="mt-2 flex justify-end gap-x-2">
          <button matButton="text" (click)="dialogRef?.close()">Cancelar</button>
          <button matButton="filled" (click)="guardar()">Guardar</button>
        </div>
      </div>
    </ng-template>

    <!-- Dialog: trazabilidad -->
    <ng-template #dialogTrazabilidad>
      <div class="flex w-full max-w-2xl flex-col gap-y-3 p-6">
        <div class="text-lg font-semibold">
          Trazabilidad del lote {{ loteSeleccionado()?.numeroLote }}
        </div>
        <div class="text-sm text-neutral-500">
          {{ trazabilidad.value()?.length ?? 0 }} inyeccion(es) con este lote.
        </div>

        <div class="max-h-96 overflow-y-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="py-2 pr-4 font-normal">Fecha</th>
                <th class="py-2 pr-4 font-normal">Paciente</th>
                <th class="py-2 pr-4 font-normal">Sede / Sala</th>
                <th class="py-2 pr-4 font-normal">Protocolo</th>
                <th class="py-2 pr-4 font-normal">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (t of trazabilidad.value() ?? []; track t.inyeccionId) {
                <tr class="border-t border-neutral-100">
                  <td class="py-2 pr-4">{{ t.fechaHoraInyeccion | date: 'short' }}</td>
                  <td class="py-2 pr-4">{{ t.pacienteNombre ?? t.pacienteIdentificador }}</td>
                  <td class="py-2 pr-4">{{ t.sede }} / {{ t.sala }}</td>
                  <td class="py-2 pr-4">{{ t.protocolo }}</td>
                  <td class="py-2 pr-4">{{ t.estadoInyeccion }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="py-4 text-center text-neutral-500">
                    Ninguna inyeccion registrada usa este lote todavia.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <div class="mt-2 flex justify-end">
          <button matButton="filled" (click)="dialogRef?.close()">Cerrar</button>
        </div>
      </div>
    </ng-template>
  `,
})
export default class LotesContraste {
  private api = inject(LotesApiService);
  private matDialog = inject(MatDialog);
  dialogRef: MatDialogRef<unknown> | null = null;

  private readonly dialogCrearTpl = viewChild.required<TemplateRef<unknown>>('dialogCrear');
  private readonly dialogTrazabilidadTpl = viewChild.required<TemplateRef<unknown>>('dialogTrazabilidad');

  // --- Filtros ---
  sedeId = signal<number | undefined>(undefined);
  agenteId = signal<number | undefined>(undefined);
  proximosACaducar = signal(false);
  pageIndex = signal(0);
  pageSize = signal(20);

  // --- Catalogos ---
  sedes = rxResource({ stream: () => this.api.getSedes() });
  agentes = rxResource({ stream: () => this.api.getAgentes() });

  // --- Listado ---
  lotes = rxResource({
    params: () => ({
      sedeId: this.sedeId(),
      agenteId: this.agenteId(),
      proximosACaducar: this.proximosACaducar() || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
    }),
    stream: ({ params }) => this.api.buscar(params),
  });

  // --- Dialog: crear ---
  protected error = signal<string | null>(null);
  private loteFormModel = signal({
    agenteId: 0,
    sedeId: 0,
    numeroLote: '',
    fechaCaducidad: '',
    cantidadMl: 0,
  });
  protected loteForm = form(this.loteFormModel, (f) => {
    // 0 es el valor centinela de "sin seleccionar/sin capturar" (ningun
    // id real ni cantidad valida es 0) -- required() no lo detecta como
    // vacio en un campo numerico, por eso se valida explicitamente.
    validate(f.agenteId, (ctx) => (ctx.value() > 0 ? null : { kind: 'required', message: 'El agente es obligatorio' }));
    validate(f.sedeId, (ctx) => (ctx.value() > 0 ? null : { kind: 'required', message: 'La sede es obligatoria' }));
    required(f.numeroLote, { message: 'El numero de lote es obligatorio' });
    required(f.fechaCaducidad, { message: 'La fecha de caducidad es obligatoria' });
    validate(f.cantidadMl, (ctx) => (ctx.value() > 0 ? null : { kind: 'required', message: 'La cantidad es obligatoria' }));
  });

  abrirCrear() {
    this.error.set(null);
    this.loteFormModel.set({ agenteId: 0, sedeId: 0, numeroLote: '', fechaCaducidad: '', cantidadMl: 0 });
    this.dialogRef = this.matDialog.open(this.dialogCrearTpl(), { panelClass: 'w-full max-w-md'.split(' ') });
  }

  async guardar() {
    this.error.set(null);
    const datos = this.loteFormModel();
    try {
      await firstValueFrom(
        this.api.crear({
          agenteId: datos.agenteId,
          sedeId: datos.sedeId,
          numeroLote: datos.numeroLote,
          fechaCaducidad: datos.fechaCaducidad,
          cantidadMl: datos.cantidadMl,
        })
      );
      this.dialogRef?.close();
      this.lotes.reload();
    } catch (e: any) {
      this.error.set(e?.error?.mensaje ?? 'No se pudo registrar el lote.');
    }
  }

  // --- Dialog: trazabilidad ---
  protected loteSeleccionado = signal<Lote | null>(null);
  protected trazabilidad = rxResource<TrazabilidadLote[], number | null>({
    params: () => this.loteSeleccionado()?.id ?? null,
    stream: ({ params }) => (params ? this.api.trazabilidad(params) : of([])),
  });

  abrirTrazabilidad(lote: Lote) {
    this.loteSeleccionado.set(lote);
    this.dialogRef = this.matDialog.open(this.dialogTrazabilidadTpl(), { panelClass: 'w-full max-w-2xl'.split(' ') });
  }

  onPagina(evento: PageEvent) {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
  }
}
