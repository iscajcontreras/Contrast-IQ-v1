import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  DatosPacienteHis,
  IntegracionClinicaApiService,
} from '@/app/domains/admin/modules/integracion-clinica/data/integracion-clinica-api.service';

@Component({
  selector: 'integracion-clinica',
  imports: [DatePipe, FormsModule, MatButtonModule, MatIconModule, MatCard, MatFormFieldModule, MatInputModule],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Integracion clinica
        </div>
        <div class="text-neutral-500">
          Datos del paciente desde el HIS, y sincronizacion real con el inyector.
        </div>
      </div>

      <!-- Traer datos del paciente desde el HIS -->
      <mat-card class="p-6" appearance="outlined">
        <div class="text-lg font-medium">Traer datos del paciente desde el HIS</div>

        @if (!hisHabilitado()) {
          <div class="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
            <mat-icon svgIcon="triangle-alert" class="mr-1 size-4 align-text-bottom" />
            Esta integracion todavia esta en modo <strong>simulado</strong>: mientras no se confirme
            el HIS real del hospital y su endpoint, esta busqueda solo devuelve los datos que ya
            existen localmente para ese paciente.
          </div>
        }

        <div class="mt-4 flex items-end gap-x-3">
          <mat-form-field subscriptSizing="dynamic" class="w-64">
            <mat-label>Numero de expediente (MRN)</mat-label>
            <input matInput [(ngModel)]="mrnBusqueda" (keyup.enter)="buscarEnHis()" />
          </mat-form-field>
          <button matButton="filled" (click)="buscarEnHis()" [disabled]="buscandoHis()">
            Buscar en el HIS
          </button>
        </div>

        @if (errorHis()) {
          <div class="mt-3 text-sm text-red-600">{{ errorHis() }}</div>
        }

        @if (resultadoHis(); as datos) {
          <div class="mt-4 rounded-lg bg-neutral-50 p-4 dark:bg-neutral-800">
            <div class="grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
              <div><span class="text-neutral-500">Nombre:</span> {{ datos.nombreCompleto ?? '—' }}</div>
              <div><span class="text-neutral-500">Sexo:</span> {{ datos.sexo ?? '—' }}</div>
              <div><span class="text-neutral-500">Peso:</span> {{ datos.pesoKg ?? '—' }} kg</div>
              <div><span class="text-neutral-500">Alergias:</span> {{ datos.alergias ?? 'ninguna' }}</div>
            </div>
            <div class="mt-2 text-xs text-neutral-500">Fuente: {{ datos.fuente }}</div>
            <button matButton="outlined" class="mt-3" (click)="sincronizarDesdeHis()">
              <mat-icon svgIcon="refresh-cw" class="mr-1 size-4" />
              Guardar estos datos en el paciente local
            </button>
          </div>
        }
      </mat-card>

      <!-- Sincronizacion real con el inyector -->
      <mat-card class="p-6" appearance="outlined">
        <div class="flex items-center justify-between">
          <div class="text-lg font-medium">Sincronizacion con el inyector</div>
          <button matButton="filled" (click)="sincronizarInyector()" [disabled]="sincronizando()">
            <mat-icon svgIcon="refresh-cw" class="mr-1 size-4" />
            Sincronizar ahora
          </button>
        </div>
        <div class="mt-2 text-sm text-neutral-500">
          Importa los archivos exportados por la IRiS Workstation desde la carpeta configurada en el
          backend (<code>app.sincronizacion.carpeta</code>). El job automatico corre cada 15 minutos
          si esta habilitado.
        </div>

        @if (mensajeSincronizacion()) {
          <div class="mt-3 rounded-lg bg-teal-50 p-3 text-sm text-teal-800">
            {{ mensajeSincronizacion() }}
          </div>
        }
      </mat-card>

      <!-- Historial de sincronizacion -->
      <mat-card appearance="outlined">
        <div class="p-6 pb-0 text-lg font-medium">Historial de sincronizacion</div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Fecha</th>
                <th class="px-6 py-2 font-normal">Fuente</th>
                <th class="px-6 py-2 font-normal">Registros importados</th>
                <th class="px-6 py-2 font-normal">Estado</th>
                <th class="px-6 py-2 font-normal">Detalle</th>
              </tr>
            </thead>
            <tbody>
              @for (l of historial.value()?.content ?? []; track l.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2">{{ l.fechaHora | date: 'dd-MM-yyyy HH:mm' }}</td>
                  <td class="px-6 py-2">{{ l.fuente }}</td>
                  <td class="px-6 py-2">{{ l.registrosImportados }}</td>
                  <td class="px-6 py-2">
                    @if (l.estado === 'EXITOSO') {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Exitoso</span>
                    } @else if (l.estado === 'PARCIAL') {
                      <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">Parcial</span>
                    } @else {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Fallido</span>
                    }
                  </td>
                  <td class="max-w-md truncate px-6 py-2 text-neutral-500" [title]="l.detalle">
                    {{ l.detalle }}
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="px-6 py-6 text-center text-neutral-500">
                    Sin sincronizaciones registradas todavia.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </mat-card>
    </div>
  `,
})
export default class IntegracionClinica {
  private api = inject(IntegracionClinicaApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  protected hisHabilitado = signal(false); // el backend siempre responde "simulado" mientras esto no cambie
  protected mrnBusqueda = '';
  protected buscandoHis = signal(false);
  protected errorHis = signal<string | null>(null);
  protected resultadoHis = signal<DatosPacienteHis | null>(null);

  protected sincronizando = signal(false);
  protected mensajeSincronizacion = signal<string | null>(null);

  protected historial = rxResource({
    stream: () =>
      ssrSeguro(this.esNavegador, () => this.api.historialSincronizacion(), { content: [], totalElements: 0 }),
  });

  async buscarEnHis() {
    this.errorHis.set(null);
    this.resultadoHis.set(null);
    if (!this.mrnBusqueda.trim()) return;

    this.buscandoHis.set(true);
    try {
      const datos = await firstValueFrom(this.api.buscarEnHis(this.mrnBusqueda.trim()));
      this.resultadoHis.set(datos);
      this.hisHabilitado.set(!datos.simulado);
    } catch {
      this.errorHis.set('No se encontro ese paciente (ni localmente, ni en el HIS).');
    } finally {
      this.buscandoHis.set(false);
    }
  }

  async sincronizarDesdeHis() {
    if (!this.mrnBusqueda.trim()) return;
    await firstValueFrom(this.api.sincronizarDesdeHis(this.mrnBusqueda.trim()));
    this.mensajeSincronizacion.set('Datos del paciente actualizados desde el HIS.');
  }

  sincronizarInyector() {
    this.sincronizando.set(true);
    this.mensajeSincronizacion.set(null);
    this.api.sincronizarInyector().subscribe({
      next: (lote) => {
        this.mensajeSincronizacion.set(
          `Sincronizacion completada: ${lote.registrosImportados} registros importados (${lote.estado}).`
        );
        this.sincronizando.set(false);
        this.historial.reload();
      },
      error: () => this.sincronizando.set(false),
    });
  }
}
