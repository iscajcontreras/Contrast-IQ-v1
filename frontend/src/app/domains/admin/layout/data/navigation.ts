import { IsActiveMatchOptions } from '@angular/router';

export type NavigationItem = {
  id: string;
  label: string;
  description?: string;
  route?: string;
  icon?: string;
  badge?: string;
  children?: NavigationItem[];
  disabled?: boolean;
  expanded?: boolean;
  activeOptions?: { exact: boolean } | IsActiveMatchOptions;
  // Si se define, el item (y sus hijos) solo se muestra a usuarios cuyo
  // rol este en esta lista. Sin esta propiedad, el item es visible para
  // cualquier usuario autenticado.
  rolesPermitidos?: string[];
  // Codigo del modulo (tabla `modulos`, ver migration_roles_permisos.sql)
  // que controla este item via la matriz Rol x Modulo x Permiso. Se
  // evalua ADEMAS de rolesPermitidos (ambos deben pasar) -- rolesPermitidos
  // queda como candado de codigo fijo, moduloCodigo es lo que un ADMIN
  // puede reconfigurar en vivo desde "Roles y permisos".
  moduloCodigo?: string;
};

export const NAVIGATION: NavigationItem[] = [
  {
    id: 'dashboards',
    label: 'Dashboards',
    description: 'Overview of key metrics',
    children: [
      {
        id: 'dashboards/contrast-injector',
        label: 'Inyectores de contraste',
        icon: 'syringe',
        route: '/admin/dashboards/contrast-injector',
        moduloCodigo: 'DASHBOARD_INYECTORES',
      },
    ],
  },
  {
    id: 'paciente',
    label: 'Paciente',
    description: 'Historial clinico-operativo por paciente',
    children: [
      {
        id: 'paciente/dashboard',
        label: 'Dashboard de paciente',
        icon: 'user-round-search',
        route: '/admin/paciente',
        moduloCodigo: 'DASHBOARD_PACIENTE',
      },
    ],
  },
  {
    id: 'insumos',
    label: 'Insumos',
    description: 'Trazabilidad de lotes de contraste',
    children: [
      {
        id: 'insumos/lotes',
        label: 'Lotes de contraste',
        icon: 'package',
        route: '/admin/insumos/lotes',
        moduloCodigo: 'INSUMOS_LOTES',
      },
      {
        id: 'insumos/pedidos',
        label: 'Pedidos de reabastecimiento',
        icon: 'shopping-cart',
        route: '/admin/insumos/pedidos',
        moduloCodigo: 'INSUMOS_PEDIDOS',
      },
    ],
  },
  {
    id: 'mantenimiento',
    label: 'Mantenimiento',
    description: 'Prediccion de falla, calibracion y soporte',
    children: [
      {
        id: 'mantenimiento/predictivo',
        label: 'Mantenimiento predictivo',
        icon: 'wrench',
        route: '/admin/mantenimiento',
        moduloCodigo: 'MANTENIMIENTO',
      },
    ],
  },
  {
    id: 'reportes',
    label: 'Reportes',
    description: 'Comparativas ejecutivas entre sedes',
    children: [
      {
        id: 'reportes/ejecutivos',
        label: 'Reportes ejecutivos',
        icon: 'chart-bar',
        route: '/admin/reportes',
        moduloCodigo: 'REPORTES',
      },
    ],
  },
  {
    id: 'integracion-clinica',
    label: 'Integracion clinica',
    description: 'HIS y sincronizacion con el inyector',
    rolesPermitidos: ['ADMIN', 'BIOMEDICA'],
    children: [
      {
        id: 'integracion-clinica/panel',
        label: 'HIS y sincronizacion',
        icon: 'plug-zap',
        route: '/admin/integracion-clinica',
        moduloCodigo: 'INTEGRACION_CLINICA',
      },
    ],
  },
  {
    id: 'administracion',
    label: 'Administracion',
    description: 'Gestion de usuarios y accesos',
    rolesPermitidos: ['ADMIN'],
    children: [
      {
        id: 'administracion/usuarios',
        label: 'Usuarios',
        icon: 'users',
        route: '/admin/administracion/usuarios',
        moduloCodigo: 'ADMINISTRACION',
      },
      {
        id: 'administracion/roles-permisos',
        label: 'Roles y permisos',
        icon: 'shield-check',
        route: '/admin/administracion/roles-permisos',
        moduloCodigo: 'ADMINISTRACION',
      },
    ],
  },
];
