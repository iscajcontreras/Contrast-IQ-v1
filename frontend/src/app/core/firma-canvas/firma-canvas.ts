import {
  Component,
  ElementRef,
  afterNextRender,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

// Firma digital "de trazo": captura el dibujo del mouse/touch en un
// <canvas> y lo expone como PNG en base64. No es una firma electronica
// avanzada con certificado -- es la forma habitual y suficiente en la
// mayoria de los sistemas hospitalarios para dejar constancia de quien
// confirmo un checklist.
@Component({
  selector: 'firma-canvas',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="flex flex-col gap-y-1">
      <div
        class="relative w-full overflow-hidden rounded-lg border border-neutral-300 bg-white dark:border-neutral-600"
      >
        <canvas
          #canvas
          class="h-40 w-full touch-none"
          (pointerdown)="iniciarTrazo($event)"
          (pointermove)="continuarTrazo($event)"
          (pointerup)="terminarTrazo()"
          (pointerleave)="terminarTrazo()"
        ></canvas>
        @if (vacio()) {
          <div
            class="pointer-events-none absolute inset-0 flex items-center justify-center text-sm text-neutral-400"
          >
            Firma aqui con el mouse o el dedo
          </div>
        }
      </div>
      <button
        type="button"
        matButton="text"
        class="self-end"
        (click)="limpiar()"
      >
        <mat-icon svgIcon="eraser" class="mr-1 size-4" />
        Limpiar firma
      </button>
    </div>
  `,
})
export class FirmaCanvas {
  private canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private ctx: CanvasRenderingContext2D | null = null;
  private dibujando = false;

  protected vacio = signal(true);

  constructor() {
    afterNextRender(() => this.inicializarCanvas());
  }

  private inicializarCanvas() {
    const canvas = this.canvasRef().nativeElement;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;

    this.ctx = canvas.getContext('2d');
    if (!this.ctx) return;
    this.ctx.scale(dpr, dpr);
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';
    this.ctx.strokeStyle = '#1f2937';
  }

  private coordenadas(evento: PointerEvent): { x: number; y: number } {
    const canvas = this.canvasRef().nativeElement;
    const rect = canvas.getBoundingClientRect();
    return { x: evento.clientX - rect.left, y: evento.clientY - rect.top };
  }

  iniciarTrazo(evento: PointerEvent) {
    if (!this.ctx) return;
    this.dibujando = true;
    const { x, y } = this.coordenadas(evento);
    this.ctx.beginPath();
    this.ctx.moveTo(x, y);
  }

  continuarTrazo(evento: PointerEvent) {
    if (!this.dibujando || !this.ctx) return;
    const { x, y } = this.coordenadas(evento);
    this.ctx.lineTo(x, y);
    this.ctx.stroke();
    this.vacio.set(false);
  }

  terminarTrazo() {
    this.dibujando = false;
  }

  limpiar() {
    const canvas = this.canvasRef().nativeElement;
    this.ctx?.clearRect(0, 0, canvas.width, canvas.height);
    this.vacio.set(true);
  }

  // PNG en base64 (sin el prefijo data:image/png;base64,), o null si el
  // usuario no ha dibujado nada todavia.
  obtenerImagenBase64(): string | null {
    if (this.vacio()) return null;
    const dataUrl = this.canvasRef().nativeElement.toDataURL('image/png');
    return dataUrl.split(',')[1] ?? null;
  }
}
