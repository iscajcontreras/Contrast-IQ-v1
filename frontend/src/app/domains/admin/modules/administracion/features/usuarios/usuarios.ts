import {
  Component,
  PLATFORM_ID,
  TemplateRef,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormField, required, email, submit, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelect } from '@angular/material/select';
import { MatOption } from '@angular/material/autocomplete';
import { firstValueFrom, of } from 'rxjs';
import { ssrSeguro } from '@/app/core/ssr/ssr-seguro';
import {
  UsuariosApiService,
  UsuarioResumen,
} from '@/app/domains/admin/modules/administracion/data/usuarios-api.service';

@Component({
  selector: 'gestion-usuarios',
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
            Gestion de usuarios
          </div>
          <div class="text-neutral-500">
            Alta, edicion y permisos de acceso al sistema.
          </div>
        </div>
        <div class="flex-auto"></div>
        <button
          matButton="filled"
          (click)="abrirCrear()"
        >
          <mat-icon svgIcon="plus" />
          Nuevo usuario
        </button>
      </div>

      <!-- Filtros -->
      <mat-card
        class="flex flex-wrap items-end gap-3 p-4"
        appearance="outlined"
      >
        <mat-form-field class="w-64">
          <mat-label>Buscar por nombre o correo</mat-label>
          <input
            matInput
            [value]="busqueda()"
            (input)="busqueda.set($any($event.target).value)"
          />
        </mat-form-field>

        <mat-form-field class="w-48">
          <mat-select [(value)]="sedeId" placeholder="Todas las sedes">
            <mat-option [value]="undefined">Todas las sedes</mat-option>
            @for (sede of sedes.value(); track sede.id) {
              <mat-option [value]="sede.id">{{ sede.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-48">
          <mat-select [(value)]="rolId" placeholder="Todos los roles">
            <mat-option [value]="undefined">Todos los roles</mat-option>
            @for (rol of roles.value(); track rol.id) {
              <mat-option [value]="rol.id">{{ rol.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-chip-listbox class="ml-auto">
          <mat-chip-option
            [selected]="soloActivos()"
            (click)="soloActivos.set(!soloActivos())"
          >
            Solo activos
          </mat-chip-option>
        </mat-chip-listbox>
      </mat-card>

      <!-- Tabla -->
      <mat-card appearance="outlined">
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="px-6 py-2 font-normal">Nombre</th>
                <th class="px-6 py-2 font-normal">Correo</th>
                <th class="px-6 py-2 font-normal">Sede</th>
                <th class="px-6 py-2 font-normal">Rol</th>
                <th class="px-6 py-2 font-normal">Origen</th>
                <th class="px-6 py-2 font-normal">Estado</th>
                <th class="px-6 py-2 font-normal">En linea</th>
                <th class="px-6 py-2 font-normal">Ultimo login</th>
                <th class="px-6 py-2 font-normal"></th>
              </tr>
            </thead>
            <tbody>
              @for (u of usuarios.value()?.content ?? []; track u.id) {
                <tr class="border-t border-neutral-100">
                  <td class="px-6 py-2 font-medium">{{ u.nombreCompleto }}</td>
                  <td class="px-6 py-2">{{ u.email }}</td>
                  <td class="px-6 py-2">{{ u.sede ?? '—' }}</td>
                  <td class="px-6 py-2">{{ u.rol }}</td>
                  <td class="px-6 py-2">Local</td>
                  <td class="px-6 py-2">
                    @if (u.activo) {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Activo</span>
                    } @else {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Inactivo</span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    @if (u.online) {
                      <span class="inline-flex items-center gap-x-1.5 rounded-full bg-green-50 px-2 py-0.5 text-green-700">
                        <span class="size-1.5 rounded-full bg-green-500"></span>
                        En linea
                      </span>
                    } @else {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Desconectado</span>
                    }
                  </td>
                  <td class="px-6 py-2">
                    {{ u.ultimoLogin ? (u.ultimoLogin | date: 'dd-MM-yyyy HH:mm') : 'Nunca' }}
                  </td>
                  <td class="px-6 py-2 text-right whitespace-nowrap">
                    <button matIconButton (click)="abrirEditar(u)" title="Editar">
                      <mat-icon svgIcon="pencil" />
                    </button>
                    <button matIconButton (click)="abrirHistorialAccesos(u)" title="Historial de accesos">
                      <mat-icon svgIcon="history" />
                    </button>
                    <button
                      matIconButton
                      (click)="cambiarEstado(u)"
                      [title]="u.activo ? 'Desactivar' : 'Activar'"
                    >
                      <mat-icon [svgIcon]="u.activo ? 'user-x' : 'user-check'" />
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator
          [length]="usuarios.value()?.totalElements ?? 0"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPagina($event)"
        ></mat-paginator>
      </mat-card>
    </div>

    <!-- Dialog de alta / edicion -->
    <ng-template #dialogUsuario>
      <div class="flex w-full max-w-md flex-col gap-y-4 p-6">
        <div class="text-lg font-semibold">
          {{ editando() ? 'Editar usuario' : 'Nuevo usuario' }}
        </div>

        <mat-form-field class="w-full">
          <mat-label>Nombre completo</mat-label>
          <input matInput [formField]="usuarioForm.nombreCompleto" />
        </mat-form-field>

        @if (!editando()) {
          <mat-form-field class="w-full">
            <mat-label>Correo</mat-label>
            <input matInput [formField]="usuarioForm.email" />
          </mat-form-field>
          <mat-form-field class="w-full">
            <mat-label>Contrasena temporal</mat-label>
            <input matInput type="password" [formField]="usuarioForm.password" />
          </mat-form-field>
        }

        <mat-form-field class="w-full">
          <mat-label>Rol</mat-label>
          <mat-select [formField]="usuarioForm.rolId">
            @for (rol of roles.value(); track rol.id) {
              <mat-option [value]="rol.id">{{ rol.etiqueta }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field class="w-full">
          <mat-label>Sede (opcional)</mat-label>
          <mat-select [formField]="usuarioForm.sedeId">
            <mat-option [value]="0">Sin sede especifica</mat-option>
            @for (sede of sedes.value(); track sede.id) {
              <mat-option [value]="sede.id">{{ sede.etiqueta }}</mat-option>
            }
          </mat-select>
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

    <!-- Dialog: historial de accesos -->
    <ng-template #dialogHistorial>
      <div class="flex w-full max-w-lg flex-col gap-y-3 p-6">
        <div class="text-lg font-semibold">
          Historial de accesos — {{ usuarioHistorial()?.nombreCompleto }}
        </div>
        <div class="max-h-96 overflow-y-auto">
          <table class="w-full text-sm">
            <thead class="text-left text-neutral-500">
              <tr>
                <th class="py-2 pr-4 font-normal">Fecha</th>
                <th class="py-2 pr-4 font-normal">Resultado</th>
                <th class="py-2 pr-4 font-normal">Metodo</th>
                <th class="py-2 pr-4 font-normal">IP</th>
              </tr>
            </thead>
            <tbody>
              @for (h of historialAccesos.value()?.content ?? []; track h.id) {
                <tr class="border-t border-neutral-100">
                  <td class="py-2 pr-4">{{ h.fechaHora | date: 'dd-MM-yyyy HH:mm' }}</td>
                  <td class="py-2 pr-4">
                    @if (h.exitoso) {
                      <span class="rounded-full bg-neutral-100 px-2 py-0.5 text-neutral-700">Exitoso</span>
                    } @else {
                      <span class="rounded-full bg-red-50 px-2 py-0.5 text-red-600">Fallido</span>
                    }
                  </td>
                  <td class="py-2 pr-4">Local</td>
                  <td class="py-2 pr-4">{{ h.ipOrigen ?? '—' }}</td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="4" class="py-4 text-center text-neutral-500">
                    Sin accesos registrados todavia.
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
export default class GestionUsuarios {
  private api = inject(UsuariosApiService);
  private esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private matDialog = inject(MatDialog);
  dialogRef: MatDialogRef<unknown> | null = null;

  private readonly dialogTpl = viewChild.required<TemplateRef<unknown>>('dialogUsuario');
  private readonly dialogHistorialTpl = viewChild.required<TemplateRef<unknown>>('dialogHistorial');

  // --- Filtros ---
  busqueda = signal('');
  sedeId = signal<number | undefined>(undefined);
  rolId = signal<number | undefined>(undefined);
  soloActivos = signal(false);
  pageIndex = signal(0);
  pageSize = signal(20);

  // --- Catalogos ---
  sedes = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getSedes(), []) });
  roles = rxResource({ stream: () => ssrSeguro(this.esNavegador, () => this.api.getRoles(), []) });

  // --- Listado ---
  usuarios = rxResource({
    params: () => ({
      busqueda: this.busqueda(),
      sedeId: this.sedeId(),
      rolId: this.rolId(),
      activo: this.soloActivos() ? true : undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
    }),
    stream: ({ params }) =>
      ssrSeguro(
        this.esNavegador,
        () => this.api.buscar(params),
        { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }
      ),
  });

  // --- Dialog de alta/edicion ---
  protected editando = signal<UsuarioResumen | null>(null);
  protected error = signal<string | null>(null);

  private usuarioFormModel = signal({
    nombreCompleto: '',
    email: '',
    password: '',
    rolId: 0,
    sedeId: 0,
  });
  protected usuarioForm = form(this.usuarioFormModel, (f) => {
    required(f.nombreCompleto, { message: 'El nombre es obligatorio' });
    if (!this.editando()) {
      required(f.email, { message: 'El correo es obligatorio' });
      email(f.email, { message: 'El correo no es valido' });
      required(f.password, { message: 'La contrasena es obligatoria' });
    }
    // 0 es el valor centinela de "sin seleccionar" (ningun rol real usa
    // ese id) -- required() no detecta un numero en 0 como vacio, por
    // eso se valida explicitamente en vez de usar required() aqui.
    validate(f.rolId, (ctx) => (ctx.value() > 0 ? null : { kind: 'required', message: 'El rol es obligatorio' }));
  });

  abrirCrear() {
    this.editando.set(null);
    this.error.set(null);
    this.usuarioFormModel.set({ nombreCompleto: '', email: '', password: '', rolId: 0, sedeId: 0 });
    this.dialogRef = this.matDialog.open(this.dialogTpl(), { panelClass: 'w-full max-w-md'.split(' ') });
  }

  abrirEditar(usuario: UsuarioResumen) {
    this.editando.set(usuario);
    this.error.set(null);
    this.usuarioFormModel.set({
      nombreCompleto: usuario.nombreCompleto,
      email: usuario.email,
      password: '',
      rolId: this.roles.value()?.find((r) => r.etiqueta === usuario.rol)?.id ?? 0,
      sedeId: usuario.sedeId ?? 0,
    });
    this.dialogRef = this.matDialog.open(this.dialogTpl(), { panelClass: 'w-full max-w-md'.split(' ') });
  }

  async guardar() {
    this.error.set(null);
    submit(this.usuarioForm, async () => {
      const datos = this.usuarioFormModel();
      const sedeId = datos.sedeId > 0 ? datos.sedeId : null;
      try {
        if (this.editando()) {
          await firstValueFrom(
            this.api.actualizar(this.editando()!.id, {
              nombreCompleto: datos.nombreCompleto,
              rolId: datos.rolId,
              sedeId,
            })
          );
        } else {
          await firstValueFrom(
            this.api.crear({
              nombreCompleto: datos.nombreCompleto,
              email: datos.email,
              password: datos.password,
              rolId: datos.rolId,
              sedeId,
            })
          );
        }
        this.dialogRef?.close();
        this.usuarios.reload();
      } catch (e: any) {
        this.error.set(e?.error?.mensaje ?? 'No se pudo guardar el usuario.');
      }
    });
  }

  cambiarEstado(usuario: UsuarioResumen) {
    this.api.cambiarEstado(usuario.id, !usuario.activo).subscribe(() => this.usuarios.reload());
  }

  onPagina(evento: PageEvent) {
    this.pageIndex.set(evento.pageIndex);
    this.pageSize.set(evento.pageSize);
  }

  // --- Historial de accesos ---
  protected usuarioHistorial = signal<UsuarioResumen | null>(null);
  protected historialAccesos = rxResource({
    params: () => this.usuarioHistorial()?.id ?? null,
    stream: ({ params }) =>
      params
        ? ssrSeguro(this.esNavegador, () => this.api.historialAccesos(params), { content: [], totalElements: 0 })
        : of({ content: [], totalElements: 0 }),
  });

  abrirHistorialAccesos(usuario: UsuarioResumen) {
    this.usuarioHistorial.set(usuario);
    this.dialogRef = this.matDialog.open(this.dialogHistorialTpl(), { panelClass: 'w-full max-w-lg'.split(' ') });
  }
}
