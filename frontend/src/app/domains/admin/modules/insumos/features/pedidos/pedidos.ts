import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCard } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelect } from '@angular/material/select';
import { MatOption } from '@angular/material/autocomplete';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  LotesApiService,
  PedidoReabastecimiento,
} from '@/app/domains/admin/modules/insumos/data/lotes-api.service';

@Component({
  selector: 'pedidos-reabastecimiento',
  imports: [DatePipe, MatCard, MatFormFieldModule, MatSelect, MatOption],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Pedidos de reabastecimiento
        </div>
        <div class="text-neutral-500">
          Reorden de stock, generado automaticamente cuando el inventario cae por debajo del minimo.
        </div>
      </div>

      <mat-card appearance="outlined">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Solicitado</th>
                <th class="px-6 py-2 font-normal">Sede</th>
                <th class="px-6 py-2 font-normal">Agente</th>
                <th class="px-6 py-2 font-normal">Cantidad</th>
                <th class="px-6 py-2 font-normal">Origen</th>
                <th class="px-6 py-2 font-normal">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (p of pedidos.value() ?? []; track p.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2">{{ p.fechaSolicitud | date: 'dd-MM-yyyy HH:mm' }}</td>
                  <td class="px-6 py-2">{{ p.sede }}</td>
                  <td class="px-6 py-2">{{ p.agente }}</td>
                  <td class="px-6 py-2">{{ p.cantidadSolicitadaMl }} ml</td>
                  <td class="px-6 py-2">
                    @if (p.generadoAutomaticamente) {
                      <span class="rounded-full bg-teal-50 px-2 py-0.5 text-teal-700">Automatico</span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Manual</span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    <mat-form-field subscriptSizing="dynamic" class="w-40">
                      <mat-select [value]="p.estado" (selectionChange)="cambiarEstado(p, $event.value)">
                        <mat-option value="PENDIENTE">Pendiente</mat-option>
                        <mat-option value="ENVIADO">Enviado</mat-option>
                        <mat-option value="RECIBIDO">Recibido</mat-option>
                        <mat-option value="CANCELADO">Cancelado</mat-option>
                      </mat-select>
                    </mat-form-field>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="6" class="px-6 py-6 text-center text-neutral-500">
                    No hay pedidos de reabastecimiento registrados.
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
export default class PedidosReabastecimiento {
  private api = inject(LotesApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  protected pedidos = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.listarPedidos(), []) });

  cambiarEstado(pedido: PedidoReabastecimiento, nuevoEstado: string) {
    this.api.actualizarPedido(pedido.id, nuevoEstado).subscribe(() => this.pedidos.reload());
  }
}
