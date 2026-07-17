import { DatePipe, DecimalPipe, isPlatformBrowser } from '@angular/common';
import {
  Component,
  PLATFORM_ID,
  TemplateRef,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { FirmaCanvas } from '@/app/core/firma-canvas/firma-canvas';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  PacientesApiService,
  PacienteResumen,
} from '@/app/domains/admin/modules/pacientes/data/pacientes-api.service';

@Component({
  selector: 'dashboard-paciente',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    FirmaCanvas,
  ],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-7xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <!-- Header -->
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Paciente
        </div>
        <div class="text-neutral-500">
          Historial clinico-operativo completo de un paciente especifico.
        </div>
      </div>

      <!-- Buscador -->
      <mat-card class="p-4" appearance="outlined">
        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Buscar paciente por nombre o numero de expediente (MRN)</mat-label>
          <input
            matInput
            [value]="busqueda()"
            (input)="onBuscar($any($event.target).value)"
            placeholder="Ej. MRN-100234 o Maria Lopez"
          />
          <mat-icon matPrefix svgIcon="search" class="mr-2 text-neutral-400" />
        </mat-form-field>

        @if (busqueda().length >= 2 && !pacienteSeleccionado()) {
          <div class="mt-3 flex flex-col divide-y divide-neutral-100 border-t border-neutral-100">
            @for (p of resultados.value()?.content ?? []; track p.id) {
              <button
                class="flex items-center justify-between px-1 py-2 text-left hover:bg-neutral-50 dark:hover:bg-neutral-800"
                (click)="seleccionar(p)"
              >
                <span class="font-medium">{{ p.nombreCompleto ?? p.identificadorExterno }}</span>
                <span class="text-sm text-neutral-500">{{ p.identificadorExterno }}</span>
              </button>
            } @empty {
              <div class="py-3 text-sm text-neutral-500">Sin resultados.</div>
            }
          </div>
        }
      </mat-card>

      @if (pacienteSeleccionado()) {
        @let detalle = perfil.value();

        <!-- Ficha del paciente -->
        <mat-card class="flex flex-wrap items-center justify-between gap-4 p-4" appearance="outlined">
          <div class="flex items-center gap-x-4">
            <div
              class="flex size-12 shrink-0 items-center justify-center rounded-full bg-primary-600 text-lg font-medium text-white"
            >
              {{ iniciales(detalle?.nombreCompleto) }}
            </div>
            <div>
              <div class="text-lg font-semibold">
                {{ detalle?.nombreCompleto ?? detalle?.identificadorExterno }}
              </div>
              <div class="text-sm text-neutral-500">
                {{ detalle?.identificadorExterno }} &middot; {{ etiquetaSexo(detalle?.sexo) }}
                @if (detalle?.pesoKg) {
                  &middot; {{ detalle?.pesoKg }} kg
                }
                @if (detalle?.creatininaMgDl) {
                  &middot; Creatinina {{ detalle?.creatininaMgDl }} mg/dl
                }
              </div>
              @if (detalle?.alergias) {
                <div class="mt-1 flex items-center gap-x-1.5 text-sm text-amber-700">
                  <mat-icon svgIcon="triangle-alert" class="size-4" />
                  Alergias: {{ detalle?.alergias }}
                </div>
              }
            </div>
          </div>

          <div class="flex items-center gap-x-3">
            @if (detalle?.gfrMlMin) {
              <div
                class="rounded-full px-3 py-1 text-sm"
                [class.bg-red-50]="detalle?.riesgoRenal"
                [class.text-red-700]="detalle?.riesgoRenal"
                [class.bg-neutral-100]="!detalle?.riesgoRenal"
                [class.text-neutral-700]="!detalle?.riesgoRenal"
              >
                GFR {{ detalle?.gfrMlMin }} ml/min
                @if (detalle?.riesgoRenal) {
                  — riesgo renal, precaucion con el contraste
                }
              </div>
            }
            <button matButton="filled" (click)="abrirChecklist()">
              <mat-icon svgIcon="clipboard-check" class="mr-1 size-4" />
              Nuevo checklist pre-inyeccion
            </button>
            <button matButton="text" (click)="limpiar()">
              <mat-icon svgIcon="x" />
              Cerrar
            </button>
          </div>
        </mat-card>

        <!-- KPIs -->
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <mat-card class="p-4" appearance="outlined">
            <div class="text-neutral-500">Inyecciones totales</div>
            <div class="mt-1 text-2xl font-semibold">
              {{ detalle?.totalInyecciones ?? 0 }}
            </div>
          </mat-card>

          <mat-card class="p-4" appearance="outlined">
            <div class="text-neutral-500">Contraste recibido (historico)</div>
            <div class="mt-1 text-2xl font-semibold">
              {{ (detalle?.volumenTotalRecibidoMl ?? 0) | number: '1.0-0' }} ml
            </div>
          </mat-card>

          <mat-card
            class="p-4"
            [class.bg-red-50]="(detalle?.alertasEdaFueraDeRango ?? 0) > 0"
            appearance="outlined"
          >
            <div class="text-neutral-500">Extravasaciones fuera de rango</div>
            <div class="mt-1 text-2xl font-semibold text-red-600">
              {{ detalle?.alertasEdaFueraDeRango ?? 0 }}
            </div>
          </mat-card>

          <mat-card class="p-4" appearance="outlined">
            <div class="text-neutral-500">Ultima inyeccion</div>
            <div class="mt-1 text-xl font-semibold">
              {{ detalle?.ultimaInyeccion ? (detalle?.ultimaInyeccion | date: 'dd-MM-yyyy') : '—' }}
            </div>
          </mat-card>

          <mat-card class="p-4" appearance="outlined">
            <div class="text-neutral-500">Dosis de radiacion combinada (DLP)</div>
            <div class="mt-1 text-2xl font-semibold">
              {{ detalle?.dlpTotalMgyCm ? (detalle?.dlpTotalMgyCm | number: '1.0-1') + ' mGy·cm' : 'sin registro' }}
            </div>
          </mat-card>
        </div>

        <!-- Historial de reacciones (Seguridad del paciente) -->
        <mat-card appearance="outlined">
          <div class="p-6 pb-0 text-lg font-medium">Historial de reacciones</div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-left text-neutral-500">
                <tr>
                  <th class="px-6 py-2 font-normal">Fecha</th>
                  <th class="px-6 py-2 font-normal">Protocolo</th>
                  <th class="px-6 py-2 font-normal">Agente</th>
                  <th class="px-6 py-2 font-normal">Estado EDA</th>
                  <th class="px-6 py-2 font-normal">Revisado</th>
                  <th class="px-6 py-2 font-normal">Accion tomada</th>
                </tr>
              </thead>
              <tbody>
                @for (r of reacciones.value() ?? []; track r.eventoId) {
                  <tr class="border-t border-neutral-100">
                    <td class="px-6 py-2">{{ r.fechaHora | date: 'dd-MM-yyyy HH:mm' }}</td>
                    <td class="px-6 py-2">{{ r.protocolo }}</td>
                    <td class="px-6 py-2">{{ r.agentePrincipal }}</td>
                    <td class="px-6 py-2">
                      @if (r.estadoEda === 'FUERA_DE_RANGO') {
                        <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Fuera de rango</span>
                      } @else if (r.estadoEda === 'EN_RANGO') {
                        <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">En rango</span>
                      } @else {
                        <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Sin referencia</span>
                      }
                    </td>
                    <td class="px-6 py-2">{{ r.revisado ? 'Si' : 'Pendiente' }}</td>
                    <td class="px-6 py-2">{{ r.accionTomada ?? '—' }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="6" class="px-6 py-6 text-center text-neutral-500">
                      Este paciente no tiene reacciones registradas.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </mat-card>

        <!-- Historial de checklists pre-inyeccion -->
        <mat-card appearance="outlined">
          <div class="p-6 pb-0 text-lg font-medium">Checklists pre-inyeccion firmados</div>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-left text-neutral-500">
                <tr>
                  <th class="px-6 py-2 font-normal">Fecha</th>
                  <th class="px-6 py-2 font-normal">Sala</th>
                  <th class="px-6 py-2 font-normal">Operador</th>
                  <th class="px-6 py-2 font-normal">GFR al momento</th>
                  <th class="px-6 py-2 font-normal">Firmado por</th>
                </tr>
              </thead>
              <tbody>
                @for (c of checklists.value() ?? []; track c.id) {
                  <tr class="border-t border-neutral-100">
                    <td class="px-6 py-2">{{ c.fechaHora | date: 'dd-MM-yyyy HH:mm' }}</td>
                    <td class="px-6 py-2">{{ c.sala ?? '—' }}</td>
                    <td class="px-6 py-2">{{ c.operador }}</td>
                    <td class="px-6 py-2">
                      {{ c.gfrValorMomento ?? '—' }}
                      @if (c.riesgoRenalMomento) {
                        <span class="ml-1 rounded-full bg-red-50 px-2 py-0.5 text-red-600">riesgo</span>
                      }
                    </td>
                    <td class="px-6 py-2">{{ c.firmaNombre }}</td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="px-6 py-6 text-center text-neutral-500">
                      Sin checklists registrados todavia.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </mat-card>

        <!-- Historial de inyecciones -->
        <mat-card appearance="outlined">
          <div class="p-6 pb-0 text-lg font-medium">Historial de inyecciones</div>
          <div class="overflow-x-auto">
            <table class="w-full min-w-max text-sm">
              <thead class="text-left text-neutral-500">
                <tr>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">#</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Fecha</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Sede / Sala</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Protocolo</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Agente</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Volumen</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Dosis (DLP)</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Presion</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">EDA</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Operador</th>
                  <th class="px-6 py-2 font-normal whitespace-nowrap">Estado</th>
                  <th class="px-6 py-2 font-normal"></th>
                </tr>
              </thead>
              <tbody>
                @for (fila of historial.value() ?? []; track fila.inyeccionId) {
                  <tr class="border-t border-neutral-100">
                    <td class="px-6 py-2 whitespace-nowrap">#{{ fila.inyeccionId }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.fechaHoraInicio | date: 'dd-MM-yyyy HH:mm' }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.sede }} / {{ fila.sala }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.protocolo }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.agentePrincipal }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.volumenTotalMl }} ml</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.dlpMgyCm ? fila.dlpMgyCm + ' mGy·cm' : '—' }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.presionMaximaPsi ?? '—' }} psi</td>
                    <td class="px-6 py-2 whitespace-nowrap">
                      @if (fila.edaHabilitado) {
                        <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Habilitado</span>
                      } @else {
                        <span class="rounded-full bg-amber-50 px-2 py-0.5 text-amber-700">Deshabilitado</span>
                      }
                    </td>
                    <td class="px-6 py-2 whitespace-nowrap">{{ fila.operador }}</td>
                    <td class="px-6 py-2 whitespace-nowrap">
                      @if (fila.tieneAlertaEda) {
                        <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">
                          EDA fuera de rango
                        </span>
                      } @else {
                        <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">
                          {{ fila.estado }}
                        </span>
                      }
                    </td>
                    <td class="px-6 py-2 whitespace-nowrap">
                      <a
                        [routerLink]="['/admin', 'paciente', 'inyeccion', fila.inyeccionId]"
                        queryParamsHandling="preserve"
                        class="whitespace-nowrap text-primary-600 hover:underline"
                      >
                        Ver detalle
                      </a>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="12" class="px-6 py-6 text-center text-neutral-500">
                      Este paciente no tiene inyecciones registradas todavia.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </mat-card>
      }
    </div>

    <!-- Dialog: checklist pre-inyeccion -->
    <ng-template #dialogChecklist>
      <div class="flex w-full max-w-lg flex-col gap-y-3 p-6">
        <div class="text-lg font-semibold">Checklist pre-inyeccion</div>
        <div class="text-sm text-neutral-500">
          {{ pacienteSeleccionado()?.nombreCompleto ?? pacienteSeleccionado()?.identificadorExterno }}
        </div>

        @let detalleActual = perfil.value();

        <div class="flex flex-col gap-y-2 rounded-lg bg-neutral-50 p-3 text-sm dark:bg-neutral-800">
          <div>
            GFR actual:
            <strong>{{ detalleActual?.gfrMlMin ?? 'sin dato' }} ml/min</strong>
            @if (detalleActual?.riesgoRenal) {
              <span class="ml-1 rounded-full bg-red-50 px-2 py-0.5 text-red-600">riesgo renal</span>
            }
          </div>
          <div>
            Alergias declaradas:
            <strong>{{ detalleActual?.alergias ?? 'ninguna declarada' }}</strong>
          </div>
        </div>

        <mat-checkbox [(ngModel)]="chkIdentidad">Verifique la identidad del paciente</mat-checkbox>
        <mat-checkbox [(ngModel)]="chkGfr">Revise el GFR mostrado arriba</mat-checkbox>
        <mat-checkbox [(ngModel)]="chkAlergias">Revise las alergias declaradas arriba</mat-checkbox>
        <mat-checkbox [(ngModel)]="chkConsentimiento">El paciente firmo el consentimiento informado</mat-checkbox>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Observaciones (opcional)</mat-label>
          <textarea matInput rows="2" [(ngModel)]="observaciones"></textarea>
        </mat-form-field>

        <mat-form-field class="w-full" subscriptSizing="dynamic">
          <mat-label>Nombre de quien firma</mat-label>
          <input matInput [(ngModel)]="firmaNombre" />
        </mat-form-field>

        <firma-canvas #firma></firma-canvas>

        @if (errorChecklist()) {
          <div class="text-sm text-red-600">{{ errorChecklist() }}</div>
        }

        <div class="mt-2 flex justify-end gap-x-2">
          <button matButton="text" (click)="dialogRef?.close()">Cancelar</button>
          <button matButton="filled" (click)="guardarChecklist(firma)">Guardar checklist</button>
        </div>
      </div>
    </ng-template>
  `,
})
export default class DashboardPaciente {
  private api = inject(PacientesApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private matDialog = inject(MatDialog);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  dialogRef: MatDialogRef<unknown> | null = null;

  private readonly dialogChecklistTpl = viewChild.required<TemplateRef<unknown>>('dialogChecklist');

  // Julio 2026: bug reportado -- al entrar al detalle de una inyeccion
  // (/admin/paciente/inyeccion/:id) y volver con "Volver al dashboard del
  // paciente", este componente se destruye y se vuelve a crear (no hay
  // RouteReuseStrategy configurada), lo que borraba la busqueda y el
  // paciente seleccionado. Se corrige con el mismo patron ya usado en
  // alertas.ts / mermas.ts: la busqueda y el paciente seleccionado se
  // reflejan en los queryParams de la URL, y se rehidratan desde ahi al
  // construir el componente.
  private queryParams = this.route.snapshot.queryParamMap;

  protected busqueda = signal(this.queryParams.get('busqueda') ?? '');
  protected pacienteSeleccionado = signal<PacienteResumen | null>(this.construirPacienteDesdeUrl());

  protected resultados = rxResource({
    params: () => this.busqueda(),
    stream: ({ params }) =>
      params.length >= 2
        ? ssrSeguro(this.esNavegador, () => this.api.buscar(params), { content: [], totalElements: 0 })
        : of({ content: [], totalElements: 0 }),
  });

  protected perfil = rxResource({
    params: () => this.pacienteSeleccionado()?.id ?? null,
    stream: ({ params }) =>
      params
        ? ssrSeguro(this.esNavegador, () => this.api.obtenerDetalle(params), {
            id: 0,
            identificadorExterno: '',
            nombreCompleto: null,
            sexo: '',
            pesoKg: null,
            creatininaMgDl: null,
            gfrMlMin: null,
            riesgoRenal: false,
            alergias: null,
            totalInyecciones: 0,
            volumenTotalRecibidoMl: 0,
            dlpTotalMgyCm: null,
            ultimaInyeccion: null,
            alertasEdaFueraDeRango: 0,
            inyeccionesAbortadasOError: 0,
          })
        : of(null),
  });

  protected historial = rxResource({
    params: () => this.pacienteSeleccionado()?.id ?? null,
    stream: ({ params }) => (params ? ssrSeguro(this.esNavegador, () => this.api.historialInyecciones(params), []) : of([])),
  });

  protected reacciones = rxResource({
    params: () => this.pacienteSeleccionado()?.id ?? null,
    stream: ({ params }) => (params ? ssrSeguro(this.esNavegador, () => this.api.reacciones(params), []) : of([])),
  });

  protected checklists = rxResource({
    params: () => this.pacienteSeleccionado()?.id ?? null,
    stream: ({ params }) => (params ? ssrSeguro(this.esNavegador, () => this.api.historialChecklists(params), []) : of([])),
  });

  constructor() {
    // Cuando pacienteSeleccionado se rehidrata desde la URL solo se conoce
    // el id (y opcionalmente nombre/expediente si venian en los
    // queryParams); en cuanto "perfil" trae el detalle completo se
    // sincronizan los campos de exhibicion (nombreCompleto,
    // identificadorExterno, sexo), que es lo que lee el dialog de
    // checklist directamente de pacienteSeleccionado() en vez de perfil.value().
    effect(() => {
      const detalle = this.perfil.value();
      const actual = this.pacienteSeleccionado();
      if (!detalle || !actual || detalle.id !== actual.id) return;
      if (
        actual.nombreCompleto !== detalle.nombreCompleto ||
        actual.identificadorExterno !== detalle.identificadorExterno ||
        actual.sexo !== detalle.sexo
      ) {
        this.pacienteSeleccionado.set({
          id: detalle.id,
          nombreCompleto: detalle.nombreCompleto,
          identificadorExterno: detalle.identificadorExterno,
          sexo: detalle.sexo,
        });
      }
    });
  }

  onBuscar(valor: string) {
    this.busqueda.set(valor);
    this.pacienteSeleccionado.set(null);
    this.sincronizarUrl();
  }

  seleccionar(paciente: PacienteResumen) {
    this.pacienteSeleccionado.set(paciente);
    this.sincronizarUrl();
  }

  limpiar() {
    this.pacienteSeleccionado.set(null);
    this.busqueda.set('');
    this.sincronizarUrl();
  }

  // Reconstruye un paciente "stub" a partir de los queryParams (solo el id
  // es indispensable -- perfil.value() completa el resto en cuanto carga,
  // ver el effect() del constructor). Se usa al re-crear el componente
  // despues de volver desde /admin/paciente/inyeccion/:id.
  private construirPacienteDesdeUrl(): PacienteResumen | null {
    const id = this.queryParams.get('pacienteId');
    if (!id || Number.isNaN(Number(id))) return null;
    return {
      id: Number(id),
      identificadorExterno: this.queryParams.get('pacienteExpediente') ?? '',
      nombreCompleto: this.queryParams.get('pacienteNombre'),
      sexo: '',
    };
  }

  // Refleja busqueda/pacienteSeleccionado en la URL (replaceUrl para no
  // llenar el historial de navegacion con cada tecla/seleccion) de forma
  // que, al volver desde el detalle de una inyeccion, este componente se
  // recree pero se rehidrate desde queryParams en vez de arrancar vacio.
  private sincronizarUrl() {
    const paciente = this.pacienteSeleccionado();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        busqueda: this.busqueda() || null,
        pacienteId: paciente?.id ?? null,
        pacienteNombre: paciente?.nombreCompleto ?? null,
        pacienteExpediente: paciente?.identificadorExterno || null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  iniciales(nombre: string | null | undefined): string {
    if (!nombre) return '?';
    const partes = nombre.trim().split(/\s+/);
    return partes.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || '?';
  }

  etiquetaSexo(sexo: string | undefined): string {
    const etiquetas: Record<string, string> = { M: 'Masculino', F: 'Femenino', OTRO: 'Otro', NO_ESPECIFICADO: 'No especificado' };
    return sexo ? (etiquetas[sexo] ?? sexo) : '';
  }

  // --- Dialog: checklist pre-inyeccion ---
  protected chkIdentidad = false;
  protected chkGfr = false;
  protected chkAlergias = false;
  protected chkConsentimiento = false;
  protected observaciones = '';
  protected firmaNombre = '';
  protected errorChecklist = signal<string | null>(null);

  abrirChecklist() {
    this.chkIdentidad = false;
    this.chkGfr = false;
    this.chkAlergias = false;
    this.chkConsentimiento = false;
    this.observaciones = '';
    this.firmaNombre = '';
    this.errorChecklist.set(null);
    this.dialogRef = this.matDialog.open(this.dialogChecklistTpl(), { panelClass: 'w-full max-w-lg'.split(' ') });
  }

  guardarChecklist(firma: FirmaCanvas) {
    this.errorChecklist.set(null);

    if (!this.chkIdentidad || !this.chkGfr || !this.chkAlergias || !this.chkConsentimiento) {
      this.errorChecklist.set('Debes confirmar los 4 puntos del checklist antes de guardar.');
      return;
    }
    if (!this.firmaNombre.trim()) {
      this.errorChecklist.set('Falta el nombre de quien firma.');
      return;
    }
    const imagenFirma = firma.obtenerImagenBase64();
    if (!imagenFirma) {
      this.errorChecklist.set('Falta la firma.');
      return;
    }

    const paciente = this.pacienteSeleccionado();
    if (!paciente) return;

    this.api
      .crearChecklist({
        pacienteId: paciente.id,
        identidadVerificada: this.chkIdentidad,
        gfrRevisado: this.chkGfr,
        alergiasRevisadas: this.chkAlergias,
        consentimientoFirmado: this.chkConsentimiento,
        observaciones: this.observaciones || undefined,
        firmaNombre: this.firmaNombre,
        firmaImagenBase64: imagenFirma,
      })
      .subscribe({
        next: () => {
          this.dialogRef?.close();
          this.checklists.reload();
        },
        error: (e) => {
          this.errorChecklist.set(e?.error?.mensaje ?? 'No se pudo guardar el checklist.');
        },
      });
  }
}
