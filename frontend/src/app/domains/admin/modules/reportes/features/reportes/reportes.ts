import { DecimalPipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import { ReportesApiService } from '@/app/domains/admin/modules/reportes/data/reportes-api.service';

function formatoISO(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

@Component({
  selector: 'reportes-ejecutivos',
  imports: [DecimalPipe, MatButtonModule, MatIconModule, MatCard, MatFormFieldModule, MatInputModule],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Reportes ejecutivos
        </div>
        <div class="text-neutral-500">
          Comparativa de indicadores entre sedes, exportable a Excel.
        </div>
      </div>

      <mat-card class="flex flex-wrap items-end gap-3 p-4" appearance="outlined">
        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Desde</mat-label>
          <input
            matInput
            type="date"
            [value]="desde()"
            (input)="desde.set($any($event.target).value)"
          />
        </mat-form-field>

        <mat-form-field subscriptSizing="dynamic">
          <mat-label>Hasta</mat-label>
          <input
            matInput
            type="date"
            [value]="hasta()"
            (input)="hasta.set($any($event.target).value)"
          />
        </mat-form-field>

        <button matButton="filled" class="ml-auto" (click)="exportarExcel()" [disabled]="exportando()">
          <mat-icon svgIcon="file-spreadsheet" class="mr-1 size-4" />
          Exportar a Excel
        </button>
      </mat-card>

      <mat-card appearance="outlined">
        <div class="p-6 pb-0 text-lg font-medium">Comparativa por sede</div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Sede</th>
                <th class="px-6 py-2 font-normal">Inyecciones totales</th>
                <th class="px-6 py-2 font-normal">Volumen total</th>
                <th class="px-6 py-2 font-normal">Inyecciones fallidas</th>
                <th class="px-6 py-2 font-normal">Tasa de falla</th>
              </tr>
            </thead>
            <tbody>
              @for (c of comparativa.value() ?? []; track c.sedeId) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 font-medium">{{ c.sede }}</td>
                  <td class="px-6 py-2">{{ c.totalInyecciones }}</td>
                  <td class="px-6 py-2">{{ c.volumenTotalMl | number: '1.0-0' }} ml</td>
                  <td class="px-6 py-2">{{ c.inyeccionesFallidas }}</td>
                  <td class="px-6 py-2">
                    <span
                      class="rounded-full px-2 py-0.5"
                      [class.bg-red-50]="c.tasaFallaPorcentaje > 8"
                      [class.text-red-600]="c.tasaFallaPorcentaje > 8"
                      [class.bg-neutral-100]="c.tasaFallaPorcentaje <= 8"
                      [class.text-neutral-700]="c.tasaFallaPorcentaje <= 8"
                    >
                      {{ c.tasaFallaPorcentaje }}%
                    </span>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="px-6 py-6 text-center text-neutral-500">
                    Sin datos para el rango seleccionado.
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
export default class ReportesEjecutivos {
  private api = inject(ReportesApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  protected desde = signal(formatoISO(new Date(new Date().setDate(new Date().getDate() - 29))));
  protected hasta = signal(formatoISO(new Date()));
  protected exportando = signal(false);

  protected comparativa = rxResource({
    params: () => ({ desde: this.desde(), hasta: this.hasta() }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.comparativaSedes(`${params.desde}T00:00:00`, `${params.hasta}T23:59:59`),
        []
      ),
  });

  exportarExcel() {
    this.exportando.set(true);
    this.api.descargarExcel(`${this.desde()}T00:00:00`, `${this.hasta()}T23:59:59`).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `comparativa_sedes_${this.desde()}_${this.hasta()}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => this.exportando.set(false),
    });
  }
}
