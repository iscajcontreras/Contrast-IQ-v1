import { DatePipe, isPlatformBrowser } from '@angular/common';
import {
  Component,
  PLATFORM_ID,
  TemplateRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  MantenimientoApiService,
  TicketSoporte,
} from '@/app/domains/admin/modules/mantenimiento/data/mantenimiento-api.service';

@Component({
  selector: 'mantenimiento-predictivo',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatFormFieldModule,
    MatInputModule,
    MatSelect,
    MatOption,
  ],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Mantenimiento predictivo
        </div>
        <div class="text-neutral-500">
          Prediccion de falla por uso, calendario de calibracion y tickets con el fabricante.
        </div>
      </div>

      <!-- Prediccion de falla -->
      <mat-card appearance="outlined">
        <div class="p-6 pb-0 text-lg font-medium">Prediccion de falla por horas de uso</div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Inyector</th>
                <th class="px-6 py-2 font-normal">Sede / Sala</th>
                <th class="px-6 py-2 font-normal">Ultimo mantenimiento</th>
                <th class="px-6 py-2 font-normal">Uso desde entonces</th>
                <th class="px-6 py-2 font-normal">Riesgo</th>
              </tr>
            </thead>
            <tbody>
              @for (p of predicciones.value() ?? []; track p.inyectorId) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 font-medium">{{ p.numeroSerie }}</td>
                  <td class="px-6 py-2">{{ p.sede }} / {{ p.sala }}</td>
                  <td class="px-6 py-2">
                    {{ p.fechaUltimoMantenimiento ? (p.fechaUltimoMantenimiento | date: 'dd-MM-yyyy') : 'sin registro' }}
                  </td>
                  <td class="px-6 py-2">
                    <div class="flex items-center gap-x-2">
                      <div class="h-2 w-32 overflow-hidden rounded-full bg-neutral-100">
                        <div
                          class="h-full rounded-full"
                          [class.bg-red-500]="p.riesgoFalla"
                          [class.bg-teal-500]="!p.riesgoFalla"
                          [style.width.%]="p.porcentajeUso"
                        ></div>
                      </div>
                      <span class="text-xs text-neutral-500">
                        {{ p.ciclosDesdeMantenimiento }} / {{ p.umbralCiclos }} ciclos
                      </span>
                    </div>
                  </td>
                  <td class="px-6 py-2">
                    @if (p.riesgoFalla) {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Requiere mantenimiento</span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Normal</span>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="px-6 py-6 text-center text-neutral-500">Sin datos.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>

      <!-- Calendario de calibracion -->
      <mat-card appearance="outlined">
        <div class="p-6 pb-0 text-lg font-medium">Calendario de calibracion</div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Inyector</th>
                <th class="px-6 py-2 font-normal">Sede / Sala</th>
                <th class="px-6 py-2 font-normal">Ultima calibracion</th>
                <th class="px-6 py-2 font-normal">Proxima calibracion</th>
                <th class="px-6 py-2 font-normal">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (c of calibraciones.value() ?? []; track c.inyectorId) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 font-medium">{{ c.numeroSerie }}</td>
                  <td class="px-6 py-2">{{ c.sede }} / {{ c.sala }}</td>
                  <td class="px-6 py-2">
                    {{ c.ultimaCalibracion ? (c.ultimaCalibracion | date: 'dd-MM-yyyy') : 'sin registro' }}
                  </td>
                  <td class="px-6 py-2">{{ c.proximaCalibracion | date: 'dd-MM-yyyy' }}</td>
                  <td class="px-6 py-2">
                    @if (c.vencida) {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">
                        Vencida ({{ -c.diasRestantes }} dias)
                      </span>
                    } @else if (c.diasRestantes <= 30) {
                      <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">
                        En {{ c.diasRestantes }} dias
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">
                        En {{ c.diasRestantes }} dias
                      </span>
                    }
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="px-6 py-6 text-center text-neutral-500">Sin datos.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>

      <!-- Tickets de soporte -->
      <mat-card appearance="outlined">
        <div class="flex items-center justify-between p-6 pb-0">
          <div class="text-lg font-medium">Tickets de soporte con el fabricante</div>
          <button matButton="filled" (click)="abrirCrearTicket()">
            <mat-icon svgIcon="plus" class="mr-1 size-4" />
            Nuevo ticket
          </button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Fecha</th>
                <th class="px-6 py-2 font-normal">Inyector</th>
                <th class="px-6 py-2 font-normal">Titulo</th>
                <th class="px-6 py-2 font-normal">Prioridad</th>
                <th class="px-6 py-2 font-normal">Estado</th>
                <th class="px-6 py-2 font-normal"></th>
              </tr>
            </thead>
            <tbody>
              @for (t of tickets.value() ?? []; track t.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2">{{ t.fechaCreacion | date: 'dd-MM-yyyy' }}</td>
                  <td class="px-6 py-2">{{ t.inyectorNumeroSerie }} ({{ t.sala }})</td>
                  <td class="px-6 py-2">{{ t.titulo }}</td>
                  <td class="px-6 py-2">
                    <span
                      class="rounded-full px-2 py-0.5"
                      [class.bg-red-50]="t.prioridad === 'CRITICA'"
                      [class.text-red-600]="t.prioridad === 'CRITICA'"
                      [class.bg-amber-50]="t.prioridad === 'ALTA'"
                      [class.text-amber-700]="t.prioridad === 'ALTA'"
                      [class.bg-neutral-100]="t.prioridad !== 'CRITICA' && t.prioridad !== 'ALTA'"
                      [class.text-neutral-700]="t.prioridad !== 'CRITICA' && t.prioridad !== 'ALTA'"
                    >
                      {{ t.prioridad }}
                    </span>
                  </td>
                  <td class="px-6 py-2">
                    <mat-form-field subscriptSizing="dynamic" class="w-44">
                      <mat-select [value]="t.estado" (selectionChange)="cambiarEstadoTicket(t, $event.value)">
                        <mat-option value="ABIERTO">Abierto</mat-option>
                        <mat-option value="EN_PROCESO">En proceso</mat-option>
                        <mat-option value="ESPERANDO_FABRICANTE">Esperando fabricante</mat-option>
                        <mat-option value="CERRADO">Cerrado</mat-option>
                      </mat-select>
                    </mat-form-field>
                  </td>
                  <td class="px-6 py-2">{{ t.numeroTicketFabricante ?? '—' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" class="px-6 py-6 text-center text-neutral-500">Sin tickets registrados.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>
    </div>

    <!-- Dialog: nuevo ticket -->
    <ng-template #dialogTicket>
      <div class="flex w-full max-w-md flex-col gap-y-4 p-6">
        <div class="text-lg font-semibold">Nuevo ticket de soporte</div>

        <mat-form-field class="w-full">
          <mat-label>Inyector</mat-label>
          <mat-select [(ngModel)]="ticketInyectorId">
            @for (inv of inyectores.value(); track inv.id) {
              <mat-option [value]="inv.id">{{ inv.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Titulo</mat-label>
          <input matInput [(ngModel)]="ticketTitulo" />
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Descripcion</mat-label>
          <textarea matInput rows="3" [(ngModel)]="ticketDescripcion"></textarea>
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Prioridad</mat-label>
          <mat-select [(ngModel)]="ticketPrioridad">
            <mat-option value="BAJA">Baja</mat-option>
            <mat-option value="MEDIA">Media</mat-option>
            <mat-option value="ALTA">Alta</mat-option>
            <mat-option value="CRITICA">Critica</mat-option>
          </mat-select>
        </mat-form-field>

        @if (errorTicket()) {
          <div class="text-sm text-red-600">{{ errorTicket() }}</div>
        }

        <div class="mt-2 flex justify-end gap-x-2">
          <button matButton="text" (click)="dialogRef?.close()">Cancelar</button>
          <button matButton="filled" (click)="guardarTicket()">Crear ticket</button>
        </div>
      </div>
    </ng-template>
  `,
})
export default class MantenimientoPredictivo {
  private api = inject(MantenimientoApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private matDialog = inject(MatDialog);
  dialogRef: MatDialogRef<unknown> | null = null;

  private readonly dialogTicketTpl = viewChild.required<TemplateRef<unknown>>('dialogTicket');

  protected predicciones = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.predicciones(), []) });
  protected calibraciones = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.calendarioCalibracion(), []) });
  protected tickets = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.listarTickets(), []) });
  protected inyectores = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getInyectores(), []) });

  cambiarEstadoTicket(ticket: TicketSoporte, nuevoEstado: string) {
    this.api.actualizarTicket(ticket.id, { estado: nuevoEstado }).subscribe(() => this.tickets.reload());
  }

  // --- Dialog: nuevo ticket ---
  protected ticketInyectorId: number | undefined;
  protected ticketTitulo = '';
  protected ticketDescripcion = '';
  protected ticketPrioridad = 'MEDIA';
  protected errorTicket = signal<string | null>(null);

  abrirCrearTicket() {
    this.ticketInyectorId = undefined;
    this.ticketTitulo = '';
    this.ticketDescripcion = '';
    this.ticketPrioridad = 'MEDIA';
    this.errorTicket.set(null);
    this.dialogRef = this.matDialog.open(this.dialogTicketTpl(), { panelClass: 'w-full max-w-md'.split(' ') });
  }

  async guardarTicket() {
    this.errorTicket.set(null);
    if (!this.ticketInyectorId || !this.ticketTitulo.trim() || !this.ticketDescripcion.trim()) {
      this.errorTicket.set('Completa todos los campos.');
      return;
    }

    try {
      await firstValueFrom(
        this.api.crearTicket({
          inyectorId: this.ticketInyectorId,
          titulo: this.ticketTitulo,
          descripcion: this.ticketDescripcion,
          prioridad: this.ticketPrioridad,
        })
      );
      this.dialogRef?.close();
      this.tickets.reload();
    } catch (e: any) {
      this.errorTicket.set(e?.error?.mensaje ?? 'No se pudo crear el ticket.');
    }
  }
}
