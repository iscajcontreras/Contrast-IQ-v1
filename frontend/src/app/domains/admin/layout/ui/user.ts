import { Component, computed, inject } from '@angular/core';
import { MatPseudoCheckbox } from '@angular/material/core';
import { MatIcon } from '@angular/material/icon';
import { MatDivider } from '@angular/material/list';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { Router } from '@angular/router';
import { Scheme, Theming } from '@/app/core/theming';
import { AuthService } from '@/app/core/auth/auth.service';

@Component({
  selector: 'user',
  imports: [
    MatDivider,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatPseudoCheckbox,
    MatMenuTrigger,
  ],
  template: `
    <button
      class="flex w-full cursor-pointer items-center gap-x-3 rounded-xl p-2 text-left hover:bg-neutral-700/10 dark:hover:bg-neutral-300/10"
      [matMenuTriggerFor]="userMenu"
    >
      <div
        class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-600 text-sm font-medium text-white select-none"
      >
        {{ iniciales() }}
      </div>
      <div class="flex min-w-0 flex-auto flex-col select-none">
        <div class="truncate font-medium">{{ nombre() }}</div>
        <div class="text-on-surface-variant truncate text-sm">
          {{ correo() }}
        </div>
      </div>
      <mat-icon
        class="size-4"
        svgIcon="ellipsis-vertical"
      />
    </button>

    <mat-menu
      class="min-w-60"
      xPosition="before"
      yPosition="above"
      #userMenu="matMenu"
    >
      <button class="py-2 [&>span]:flex [&>span]:items-center">
        <div
          class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary-600 text-sm font-medium text-white select-none"
        >
          {{ iniciales() }}
        </div>
        <div class="ml-3 flex min-w-0 flex-auto flex-col select-none">
          <div class="truncate font-medium">{{ nombre() }}</div>
          <div class="text-on-surface-variant truncate text-xs">
            {{ correo() }}
          </div>
        </div>
      </button>
      <mat-divider />
      <button
        mat-menu-item
        [matMenuTriggerFor]="appearanceMenu"
      >
        <mat-icon svgIcon="sun-moon" />
        Appearance
      </button>
      <mat-divider />
      <button
        mat-menu-item
        (click)="cerrarSesion()"
      >
        <mat-icon svgIcon="log-out" />
        Sign out
      </button>
    </mat-menu>

    <mat-menu #appearanceMenu="matMenu">
      @for (item of schemes; track item.value) {
        <button
          mat-menu-item
          (click)="updateScheme(item.value)"
        >
          <mat-pseudo-checkbox
            appearance="minimal"
            class="mr-2"
            [state]="scheme() === item.value ? 'checked' : 'unchecked'"
          />
          <span>{{ item.label }}</span>
        </button>
      }
    </mat-menu>
  `,
})
export class User {
  // Dependencies
  private theming = inject(Theming);
  private auth = inject(AuthService);
  private router = inject(Router);

  // State
  protected scheme = computed(() => this.theming.scheme());
  protected schemes: { label: string; value: Scheme }[] = [
    { label: 'Light', value: 'light' },
    { label: 'Dark', value: 'dark' },
    { label: 'System', value: 'system' },
  ];

  // Nombre y correo reales de quien inicio sesion, obtenidos de
  // GET /api/auth/me -- ver AuthService.cargarPerfil().
  protected nombre = computed(() => this.auth.perfil()?.nombreCompleto ?? 'Usuario');
  protected correo = computed(() => this.auth.perfil()?.email ?? '');
  protected iniciales = computed(() => {
    const nombre = this.nombre();
    const partes = nombre.trim().split(/\s+/);
    const primeras = partes.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '');
    return primeras.join('') || 'U';
  });

  updateScheme(scheme: Scheme) {
    this.theming.scheme.set(scheme);
  }

  cerrarSesion() {
    this.auth.cerrarSesion();
    this.router.navigateByUrl('/auth/sign-in');
  }
}
