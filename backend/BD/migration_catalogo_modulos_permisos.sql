-- =====================================================================
-- migration_catalogo_modulos_permisos.sql
-- Correr DESPUES de schema_contrastiq_completo.sql (y, si se quiere,
-- despues de datos_dummy_contrastiq.sql -- el orden entre estos dos no
-- importa, esta migracion no toca tablas operativas).
--
-- Consolida en UN solo archivo, en orden, las 3 migraciones reales que
-- siembran el catalogo Rol x Modulo x Permiso -- antes vivian como 3
-- archivos separados (migration_roles_permisos.sql,
-- migration_modulo_mermas.sql, migration_modulo_extravasaciones.sql).
-- Se consolidan aqui porque `schema_contrastiq_completo.sql` y
-- `datos_dummy_contrastiq.sql` deliberadamente NO incluyen este catalogo
-- (es dato estructural, no dato de prueba -- ver cabecera de
-- datos_dummy_contrastiq.sql), asi que sin este archivo una BD nueva
-- levantada solo con esos dos scripts queda con `modulos`, `permisos` y
-- `rol_modulo_permiso` VACIAS y el sistema de permisos por modulo no
-- funciona (todo el mundo sin acceso a ningun modulo).
--
-- Las CREATE TABLE de modulos/permisos/rol_modulo_permiso NO se repiten
-- aqui -- ya estan en schema_contrastiq_completo.sql. Este archivo es
-- solo el INSERT del catalogo, en el mismo orden en que se aplicaron
-- originalmente en la BD real:
--   1. migration_roles_permisos.sql -- 8 modulos originales + 5 permisos
--      + matriz completa de los 5 roles.
--   2. migration_modulo_mermas.sql -- 9o modulo, INSUMOS_MERMAS.
--   3. migration_modulo_extravasaciones.sql -- 10o modulo, EXTRAVASACIONES.
--
-- Seguro de re-ejecutar: todos los INSERT de modulos usan
-- ON DUPLICATE KEY UPDATE (o son la primera siembra) y los de
-- rol_modulo_permiso usan ON DUPLICATE KEY UPDATE / dependen de que la
-- fila del modulo ya exista -- no falla si se corre dos veces.
-- =====================================================================

USE contrast_iq;

-- =====================================================================
-- PARTE 1 (migration_roles_permisos.sql): 8 modulos originales
-- =====================================================================

INSERT INTO modulos (codigo, nombre, descripcion, orden) VALUES
    ('DASHBOARD_INYECTORES',  'Panel de inyectores de contraste', 'Dashboard operativo de inyectores', 1),
    ('DASHBOARD_PACIENTE',    'Dashboard de paciente',            'Historial e inyecciones por paciente', 2),
    ('INSUMOS_LOTES',         'Lotes de insumos',                 'Control de lotes de medio de contraste', 3),
    ('INSUMOS_PEDIDOS',       'Pedidos de reabastecimiento',      'Ordenes de compra de insumos', 4),
    ('MANTENIMIENTO',         'Mantenimiento predictivo',         'Tickets y prediccion de fallas de inyectores', 5),
    ('REPORTES',              'Reportes ejecutivos',              'Comparativas entre sedes, exportables', 6),
    ('INTEGRACION_CLINICA',   'Integracion clinica',              'Sincronizacion con HIS/RIS', 7),
    ('ADMINISTRACION',        'Administracion de usuarios',       'Alta/edicion de usuarios, roles y permisos', 8)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion), orden = VALUES(orden);

INSERT INTO permisos (codigo, nombre, descripcion) VALUES
    ('VER',       'Ver',       'Consultar informacion del modulo'),
    ('CREAR',     'Crear',     'Dar de alta registros nuevos'),
    ('EDITAR',    'Editar',    'Modificar registros existentes'),
    ('ELIMINAR',  'Eliminar',  'Eliminar o desactivar registros'),
    ('EXPORTAR',  'Exportar',  'Descargar/exportar informacion (ej. Excel)')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

-- ADMIN recibe automaticamente TODOS los permisos en TODOS los modulos
-- (CROSS JOIN). Los demas roles se configuran a mano desde la pantalla
-- "Roles y permisos".
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- TECNICO: operacion diaria (dashboards, insumos, mantenimiento).
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'TECNICO'
  AND m.codigo IN ('DASHBOARD_INYECTORES', 'DASHBOARD_PACIENTE', 'INSUMOS_LOTES', 'INSUMOS_PEDIDOS', 'MANTENIMIENTO')
  AND p.codigo IN ('VER', 'CREAR', 'EDITAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- RADIOLOGO: enfocado en pacientes/inyecciones, solo lectura+exportar en reportes.
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'RADIOLOGO'
  AND m.codigo IN ('DASHBOARD_INYECTORES', 'DASHBOARD_PACIENTE')
  AND p.codigo IN ('VER', 'CREAR', 'EDITAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'RADIOLOGO'
  AND m.codigo = 'REPORTES'
  AND p.codigo IN ('VER', 'EXPORTAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- BIOMEDICA: mantenimiento + integracion clinica + insumos.
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'BIOMEDICA'
  AND m.codigo IN ('DASHBOARD_INYECTORES', 'MANTENIMIENTO', 'INTEGRACION_CLINICA', 'INSUMOS_LOTES', 'INSUMOS_PEDIDOS')
  AND p.codigo IN ('VER', 'CREAR', 'EDITAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- VISUALIZADOR: solo lectura en todo (rol por defecto de auto-registro).
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'VISUALIZADOR'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- =====================================================================
-- PARTE 2 (migration_modulo_mermas.sql): 9o modulo, INSUMOS_MERMAS
-- =====================================================================

INSERT INTO modulos (codigo, nombre, descripcion, orden) VALUES
    ('INSUMOS_MERMAS', 'Merma de insumos', 'Volumen programado vs. real de contraste y solucion salina', 4)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN'
  AND m.codigo = 'INSUMOS_MERMAS'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'TECNICO'
  AND m.codigo = 'INSUMOS_MERMAS'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'BIOMEDICA'
  AND m.codigo = 'INSUMOS_MERMAS'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'VISUALIZADOR'
  AND m.codigo = 'INSUMOS_MERMAS'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- =====================================================================
-- PARTE 3 (migration_modulo_extravasaciones.sql): 10o modulo, EXTRAVASACIONES
-- =====================================================================

INSERT INTO modulos (codigo, nombre, descripcion, orden) VALUES
    ('EXTRAVASACIONES', 'Alertas de extravasacion', 'Revision clinica de eventos EDA (deteccion de extravasacion) fuera de rango', 5)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN'
  AND m.codigo = 'EXTRAVASACIONES'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'RADIOLOGO'
  AND m.codigo = 'EXTRAVASACIONES'
  AND p.codigo IN ('VER', 'EDITAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'TECNICO'
  AND m.codigo = 'EXTRAVASACIONES'
  AND p.codigo IN ('VER', 'EDITAR')
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'BIOMEDICA'
  AND m.codigo = 'EXTRAVASACIONES'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id)
SELECT r.id, m.id, p.id
FROM roles r
CROSS JOIN modulos m
CROSS JOIN permisos p
WHERE r.nombre = 'VISUALIZADOR'
  AND m.codigo = 'EXTRAVASACIONES'
  AND p.codigo = 'VER'
ON DUPLICATE KEY UPDATE rol_id = rol_id;

-- Verificacion rapida: deberia mostrar 10 modulos con sus permisos por rol.
-- SELECT r.nombre AS rol, m.codigo AS modulo, GROUP_CONCAT(p.codigo ORDER BY p.codigo) AS permisos
-- FROM rol_modulo_permiso rmp
-- JOIN roles r ON r.id = rmp.rol_id
-- JOIN modulos m ON m.id = rmp.modulo_id
-- JOIN permisos p ON p.id = rmp.permiso_id
-- GROUP BY r.nombre, m.codigo
-- ORDER BY r.nombre, m.codigo;
