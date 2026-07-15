import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCard } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom, of } from 'rxjs';
import {
  AdministracionApiService,
  MatrizCelda,
} from '@/app/domains/admin/modules/administracion/data/administracion-api.service';

// "Roles y permisos": matriz Modulo x Permiso para el rol seleccionado.
// Cada checkbox guarda al instante (otorgar = POST, revocar = DELETE) --
// sin boton "Guardar" general, igual que en el proyecto de referencia
// CEROGAS GPS del que se adapto este patron.
@Component({
  selector: 'roles-permisos',
  imports: [MatCard, MatChipsModule, MatCheckboxModule, MatProgressSpinnerModule],
  template: `
    <div
      class="@container mx-auto flex w-full max-w-5xl flex-auto flex-col gap-4 p-6 sm:gap-6 lg:p-10 lg:pt-8"
    >
      <div class="flex flex-col gap-y-0.5">
        <div class="text-xl font-semibold tracking-tighter sm:text-2xl">
          Roles y permisos
        </div>
        <div class="text-neutral-500">
          Que puede ver y hacer cada rol en cada modulo de ContrastIQ. Los cambios se guardan al instante.
        </div>
      </div>

      <!-- Selector de rol -->
      <mat-chip-listbox class="flex flex-wrap gap-2">
        @for (rol of roles.value() ?? []; track rol.id) {
          <mat-chip-option
            [selected]="rolSeleccionado() === rol.id"
            [disabled]="rol.nombre === 'ADMIN'"
            (click)="seleccionarRol(rol.id)"
          >
            {{ rol.nombre }}
            <span class="ml-1 text-xs text-neutral-500">({{ rol.cantidadUsuarios }} usuarios)</span>
          </mat-chip-option>
        }
      </mat-chip-listbox>

      @if (esRolAdmin()) {
        <div class="rounded-lg bg-blue-50 px-4 py-3 text-sm text-blue-700 dark:bg-blue-950 dark:text-blue-300">
          El rol ADMIN siempre tiene todos los permisos en todos los modulos y no se puede editar desde aqui.
        </div>
      }

      <!-- Matriz -->
      <mat-card appearance="outlined">
        @if (matriz.isLoading()) {
          <div class="flex items-center justify-center p-10">
            <mat-spinner diameter="32" />
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="text-left text-neutral-500">
                <tr>
                  <th class="px-6 py-2 font-normal">Modulo</th>
                  @for (permiso of permisos.value() ?? []; track permiso.id) {
                    <th class="px-4 py-2 text-center font-normal">{{ permiso.nombre }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (modulo of modulos.value() ?? []; track modulo.id) {
                  <tr class="border-t border-neutral-100">
                    <td class="px-6 py-2 font-medium">{{ modulo.nombre }}</td>
                    @for (permiso of permisos.value() ?? []; track permiso.id) {
                      <td class="px-4 py-2 text-center">
                        <mat-checkbox
                          [checked]="celda(modulo.id, permiso.id)?.otorgado ?? false"
                          [disabled]="esRolAdmin() || guardando()"
                          (change)="alternar(modulo.id, permiso.id, $event.checked)"
                        />
                      </td>
                    }
                  </tr>
                } @empty {
                  <tr>
                    <td [attr.colspan]="(permisos.value()?.length ?? 0) + 1" class="px-6 py-6 text-center text-neutral-500">
                      No hay modulos configurados.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </mat-card>

      @if (error()) {
        <div class="text-sm text-red-600">{{ error() }}</div>
      }
    </div>
  `,
})
export default class RolesPermisos {
  private api = inject(AdministracionApiService);

  protected rolSeleccionado = signal<number | null>(null);
  protected guardando = signal(false);
  protected error = signal<string | null>(null);

  protected roles = rxResource({
    stream: () => this.api.getRoles(),
  });

  protected modulos = rxResource({ stream: () => this.api.getModulos() });
  protected permisos = rxResource({ stream: () => this.api.getPermisos() });

  protected matriz = rxResource({
    params: () => this.rolSeleccionado(),
    stream: ({ params }) => (params ? this.api.getMatriz(params) : of([])),
  });

  protected esRolAdmin = computed(
    () => this.roles.value()?.find((r) => r.id === this.rolSeleccionado())?.nombre === 'ADMIN'
  );

  constructor() {
    // Selecciona el primer rol no-ADMIN por default en cuanto llega el
    // catalogo (ADMIN esta bloqueado para edicion, ver esRolAdmin()).
    effect(() => {
      const roles = this.roles.value();
      if (roles && roles.length > 0 && this.rolSeleccionado() === null) {
        const noAdmin = roles.find((r) => r.nombre !== 'ADMIN');
        this.rolSeleccionado.set(noAdmin?.id ?? roles[0].id);
      }
    });
  }

  seleccionarRol(rolId: number) {
    this.rolSeleccionado.set(rolId);
  }

  celda(moduloId: number, permisoId: number): MatrizCelda | undefined {
    return this.matriz.value()?.find((c) => c.moduloId === moduloId && c.permisoId === permisoId);
  }

  async alternar(moduloId: number, permisoId: number, otorgado: boolean) {
    const rolId = this.rolSeleccionado();
    if (!rolId) return;

    this.guardando.set(true);
    this.error.set(null);
    try {
      if (otorgado) {
        await firstValueFrom(this.api.otorgar(rolId, moduloId, permisoId));
      } else {
        await firstValueFrom(this.api.revocar(rolId, moduloId, permisoId));
      }
      this.matriz.reload();
    } catch (e: any) {
      this.error.set(e?.error?.mensaje ?? 'No se pudo guardar el cambio.');
      this.matriz.reload();
    } finally {
      this.guardando.set(false);
    }
  }
}
