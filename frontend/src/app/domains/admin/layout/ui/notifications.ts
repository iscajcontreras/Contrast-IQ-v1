import { CdkConnectedOverlay, CdkOverlayOrigin } from '@angular/cdk/overlay';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatDivider } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { formatDistance } from 'date-fns';
import { AlertasService, Alerta } from '@/app/core/alertas/alertas.service';

// Adaptado del componente de notificaciones del template Fuse para
// mostrar alertas REALES del sistema (mantenimiento, stock,
// comunicacion, extravasaciones fuera de rango), combinando:
// - Historial reciente por HTTP (GET /api/alertas)
// - Alertas que llegan en vivo mientras la pagina esta abierta (WebSocket)
@Component({
  selector: 'notifications',
  imports: [
    MatIconButton,
    MatIcon,
    CdkConnectedOverlay,
    CdkOverlayOrigin,
    MatDivider,
  ],
  template: `
    <button
      matIconButton
      cdkOverlayOrigin
      class="relative"
      (click)="toggle()"
      #trigger="cdkOverlayOrigin"
    >
      <mat-icon svgIcon="bell" />
      @if (pendientes() > 0) {
        <span
          class="absolute top-1.5 right-1.5 flex size-4 items-center justify-center rounded-full bg-red-600 text-[10px] font-medium text-white"
        >
          {{ pendientes() > 9 ? '9+' : pendientes() }}
        </span>
      }
    </button>

    <ng-template
      cdkConnectedOverlay
      [cdkConnectedOverlayOrigin]="trigger"
      [cdkConnectedOverlayOpen]="open()"
      [cdkConnectedOverlayHasBackdrop]="true"
      [cdkConnectedOverlayBackdropClass]="'transparent'.split(' ')"
      (detach)="toggle(false)"
      (backdropClick)="toggle(false)"
    >
      <div
        class="z-10 flex max-h-120 w-full max-w-xs flex-col overflow-y-auto rounded-lg bg-white shadow-(--mat-sys-level2) dark:bg-neutral-800"
      >
        <!-- Header -->
        <div class="flex flex-col bg-neutral-100 dark:bg-neutral-800">
          <div class="flex items-center p-4 pb-3 pl-6">
            <div class="flex items-center gap-x-3">
              <mat-icon
                class="size-4.5"
                svgIcon="bell"
              />
              <div class="text-xl font-semibold tracking-tighter">
                Alertas
              </div>
            </div>
            <div class="flex-auto"></div>
            <div
              class="flex items-center gap-x-1.5 text-xs"
              [class.text-green-600]="conectado()"
              [class.text-neutral-400]="!conectado()"
            >
              <span class="size-2 rounded-full bg-current"></span>
              {{ conectado() ? 'En vivo' : 'Sin conexion' }}
            </div>
          </div>
          <mat-divider />
        </div>

        <!-- List -->
        <div class="flex flex-col">
          @if (alertas().length === 0) {
            <div class="p-6 text-center text-sm text-neutral-500">
              No hay alertas pendientes.
            </div>
          }
          @for (alerta of alertas(); track alerta.id; let last = $last) {
            <div class="flex gap-x-2 py-3 pr-4 pl-6">
              <div class="flex-auto">
                <div class="font-semibold">
                  {{ alerta.sala ? alerta.sala + ' — ' : '' }}{{ etiquetaTipo(alerta.tipo) }}
                </div>
                <div class="line-clamp-2">{{ alerta.mensaje }}</div>
                <div class="mt-1 flex items-center gap-x-2 text-xs text-neutral-500">
                  <span
                    class="rounded-full px-1.5 py-0.5"
                    [class.bg-red-100]="alerta.severidad === 'CRITICA'"
                    [class.text-red-700]="alerta.severidad === 'CRITICA'"
                    [class.bg-amber-100]="alerta.severidad === 'ADVERTENCIA'"
                    [class.text-amber-700]="alerta.severidad === 'ADVERTENCIA'"
                    [class.bg-neutral-200]="alerta.severidad === 'INFO'"
                  >
                    {{ alerta.severidad }}
                  </span>
                  {{ timeAgo(alerta.fechaHora) }}
                </div>
              </div>
              <button
                matIconButton
                (click)="marcarResuelta(alerta)"
                title="Marcar como resuelta"
              >
                <mat-icon svgIcon="check" />
              </button>
            </div>

            @if (!last) {
              <mat-divider
                class="[--mat-divider-color:var(--color-neutral-200)] dark:[--mat-divider-color:var(--color-neutral-700)]"
              />
            }
          }
        </div>
      </div>
    </ng-template>
  `,
})
export class Notifications implements OnInit {
  private alertasService = inject(AlertasService);

  protected open = signal(false);

  // Combina el historial (HTTP) con lo que llega en vivo, sin duplicar
  // por id, mas reciente primero.
  private historial = signal<Alerta[]>([]);
  protected alertas = computed(() => {
    const vivos = this.alertasService.alertasEnVivo();
    const idsVivos = new Set(vivos.map((a) => a.id));
    const combinadas = [...vivos, ...this.historial().filter((a) => !idsVivos.has(a.id))];
    return combinadas.filter((a) => !a.resuelta).slice(0, 20);
  });
  protected pendientes = computed(() => this.alertas().length);
  protected conectado = computed(() => this.alertasService.conectado());

  constructor() {
    // Cada vez que llega una alerta en vivo nueva, refresca tambien el
    // conteo visible sin esperar a un refresh manual del historial.
    effect(() => {
      this.alertasService.alertasEnVivo();
    });
  }

  ngOnInit(): void {
    this.alertasService.conectar();
    this.alertasService.listar({ resuelta: false, size: 20 }).subscribe({
      next: (pagina) => this.historial.set(pagina.content),
      error: () => this.historial.set([]),
    });
  }

  toggle(force: boolean | null = null) {
    this.open.update((value) => (force === null ? !value : force));
  }

  marcarResuelta(alerta: Alerta) {
    this.alertasService.resolver(alerta.id).subscribe(() => {
      this.historial.update((lista) => lista.filter((a) => a.id !== alerta.id));
      this.alertasService.alertasEnVivo.update((lista) => lista.filter((a) => a.id !== alerta.id));
    });
  }

  timeAgo(fecha: string) {
    return formatDistance(new Date(fecha), new Date(), { addSuffix: true });
  }

  etiquetaTipo(tipo: string): string {
    const etiquetas: Record<string, string> = {
      EQUIPO_MANTENIMIENTO: 'Mantenimiento de equipo',
      STOCK_BAJO: 'Stock bajo',
      FALLA_COMUNICACION: 'Falla de comunicacion',
      EDA_FUERA_DE_RANGO: 'Extravasacion fuera de rango',
      OTRO: 'Alerta operativa',
    };
    return etiquetas[tipo] ?? tipo;
  }
}
