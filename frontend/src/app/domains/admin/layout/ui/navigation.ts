import { Tree, TreeItem, TreeItemGroup } from '@angular/aria/tree';
import { CdkMonitorFocus } from '@angular/cdk/a11y';
import { NgTemplateOutlet } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIcon } from '@angular/material/icon';
import {
  isActive,
  IsActiveMatchOptions,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
} from '@angular/router';
import { filter, take } from 'rxjs';
import { AuthService } from '@/app/core/auth/auth.service';
import { PermisosService } from '@/app/core/auth/permisos.service';
import {
  NAVIGATION,
  NavigationItem,
} from '@/app/domains/admin/layout/data/navigation';

@Component({
  selector: 'navigation',
  imports: [
    MatIcon,
    NgTemplateOutlet,
    RouterLinkActive,
    Tree,
    TreeItem,
    TreeItemGroup,
    RouterLink,
    CdkMonitorFocus,
  ],
  template: `
    <div class="flex flex-col gap-y-4">
      @for (section of navigation(); track section.id) {
        <div class="flex flex-col px-4">
          <!-- Section title -->
          <div class="px-2.5 py-1.5 text-sm font-semibold text-blue-400">
            {{ section.label }}

            <!-- Section description -->
            @if (section.description) {
              <div class="text-xs font-medium text-neutral-400">
                {{ section.description }}
              </div>
            }
          </div>

          <!-- Section content -->
          <ul
            ngTree
            class="mt-1 flex flex-col gap-y-1"
            [nav]="true"
            #tree="ngTree"
          >
            <ng-template
              [ngTemplateOutlet]="treeNodes"
              [ngTemplateOutletContext]="{
                nodes: section.children,
                parent: tree,
              }"
            />
          </ul>

          <!-- Menu item -->
          <ng-template
            let-nodes="nodes"
            let-parent="parent"
            #treeNodes
          >
            @for (node of nodes; track node.id) {
              <a
                cdkMonitorElementFocus
                ngTreeItem
                routerLinkActive="bg-neutral-700/10 dark:bg-neutral-300/10"
                class="navigation-item flex cursor-pointer items-center gap-x-2 rounded-lg px-2.5 py-2 select-none hover:bg-neutral-700/10 dark:hover:bg-neutral-300/10"
                [parent]="parent"
                [value]="node.id"
                [label]="node.label"
                [disabled]="node.disabled"
                [selectable]="!node.children"
                [(expanded)]="node.expanded"
                [routerLink]="node.route"
                [routerLinkActiveOptions]="
                  node.activeOptions ?? { exact: true }
                "
                (click)="$event.preventDefault()"
                #rla="routerLinkActive"
                #treeItem="ngTreeItem"
              >
                <!-- Icon -->
                @if (node.icon) {
                  <mat-icon
                    class="pointer-events-none size-4"
                    [svgIcon]="node.icon"
                  />
                }

                <!-- Label -->
                <div class="flex flex-auto flex-col font-medium">
                  {{ node.label }}

                  <!-- Description -->
                  @if (node.description) {
                    <div class="text-xs">
                      {{ node.description }}
                    </div>
                  }
                </div>

                <!-- Badge -->
                @if (node.badge) {
                  <div
                    class="rounded bg-pink-400 px-1.5 py-0.5 text-xs font-semibold dark:bg-pink-700"
                  >
                    {{ node.badge }}
                  </div>
                }

                <!-- Expand icon -->
                @if (node.children && node.children.length > 0) {
                  <mat-icon
                    svgIcon="chevron-right"
                    class="pointer-events-none size-4 transition-[rotate]"
                    [class.rotate-90]="node.expanded"
                  />
                }
              </a>

              <!-- Children -->
              @if (node.children && node.children.length > 0) {
                <ul
                  class="flex flex-col gap-y-1 [&_ul>.navigation-item]:pl-14.5 [&>.navigation-item]:pl-8.5"
                  [class.hidden]="!node.expanded"
                  [class.mt-1]="node.expanded"
                  role="group"
                >
                  <ng-template
                    ngTreeItemGroup
                    [ownedBy]="treeItem"
                    #group="ngTreeItemGroup"
                  >
                    <ng-template
                      [ngTemplateOutlet]="treeNodes"
                      [ngTemplateOutletContext]="{
                        nodes: node.children,
                        parent: group,
                      }"
                    />
                  </ng-template>
                </ul>
              }
            }
          </ng-template>
        </div>
      }
    </div>
  `,
})
export class Navigation {
  // Dependencies
  private router = inject(Router);
  private auth = inject(AuthService);
  private permisos = inject(PermisosService);

  // State
  protected navigation = signal<NavigationItem[]>(
    this.filtrarPorRol(NAVIGATION)
  );
  protected navigationEnd = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      take(1)
    )
  );

  constructor() {
    // El perfil (y por lo tanto el rol) se carga de forma asincrona
    // despues del login o al recargar la pagina; en cuanto llega, se
    // vuelve a filtrar el menu para mostrar/ocultar secciones como
    // "Administracion" segun corresponda.
    effect(() => {
      this.auth.perfil();
      this.navigation.set(this.filtrarPorRol(NAVIGATION));
    });

    // Los permisos (matriz Rol x Modulo x Permiso) se cargan de forma
    // asincrona via /api/me/permisos -- en cuanto llegan, se vuelve a
    // filtrar el menu para ocultar items cuyo moduloCodigo no tenga VER.
    this.permisos.asegurarCargado().subscribe(() => {
      this.navigation.set(this.filtrarPorRol(NAVIGATION));
    });

    // Expand active route on initial load
    effect(() => {
      const navigationEnd = this.navigationEnd();
      if (!navigationEnd) {
        return;
      }

      this.navigation.set(this.expandActiveRoute(this.navigation()));
    });
  }

  /**
   * Oculta los items (y sus hijos) cuyo rolesPermitidos no incluya el
   * rol del usuario autenticado, Y (si tienen moduloCodigo) cuya matriz
   * Rol x Modulo x Permiso no le otorgue VER en ese modulo. Un item sin
   * ninguna de las dos propiedades es visible para cualquier usuario
   * autenticado. Un grupo padre que se queda sin hijos tras filtrar
   * (ej. "Administracion" completo) tambien se oculta.
   */
  private filtrarPorRol(items: NavigationItem[]): NavigationItem[] {
    const rol = this.auth.perfil()?.rol;

    return items
      .filter((item) => !item.rolesPermitidos || (!!rol && item.rolesPermitidos.includes(rol)))
      .filter((item) => !item.moduloCodigo || this.permisos.tienePermisoSync(item.moduloCodigo, 'VER'))
      .map((item) =>
        item.children
          ? { ...item, children: this.filtrarPorRol(item.children) }
          : item
      )
      .filter((item) => !item.children || item.children.length > 0);
  }

  /**
   * Expand all parent routes of the active route.
   * @param items
   */
  expandActiveRoute(items: NavigationItem[]): NavigationItem[] {
    for (const item of items) {
      if (item.children?.length) {
        item.children = this.expandActiveRoute(item.children);

        if (item.children.some((child) => child.expanded)) {
          item.expanded = true;
        }
      }

      if (
        item.route &&
        isActive(
          item.route,
          this.router,
          this.isActiveOption(item.activeOptions ?? { exact: true })
        )()
      ) {
        item.expanded = true;
      }
    }
    return items;
  }

  /**
   * Convert simple exact option to full IsActiveMatchOptions.
   * @param options
   */
  isActiveOption(
    options: { exact: boolean } | IsActiveMatchOptions
  ): IsActiveMatchOptions {
    if ('exact' in options) {
      return options.exact
        ? {
            paths: 'exact',
            queryParams: 'exact',
            fragment: 'ignored',
            matrixParams: 'ignored',
          }
        : {
            paths: 'subset',
            queryParams: 'subset',
            fragment: 'ignored',
            matrixParams: 'ignored',
          };
    }

    return options;
  }
}
