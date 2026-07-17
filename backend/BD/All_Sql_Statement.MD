# All_Sql_Statement.MD — Inventario de sentencias SQL por Controller

> Generado el 17 de julio de 2026 a partir del código fuente real de `BackEnd_ContrastIQ`
> (17 controllers, 22 services, 29 repositories). Para cada endpoint se documenta: qué hace,
> la cadena Controller → Service → Repository, y la sentencia SQL/JPQL real (o su equivalente
> SQL cuando es un derived query de Spring Data) para poder probarla directo en MySQL contra
> `contrast_iq` y verificar el comportamiento de los datos.
>
> **Confirmado**: no hay `JdbcTemplate`, `EntityManager.createNativeQuery` ni `createQuery` en
> ningún Service — toda la persistencia pasa por repositorios Spring Data JPA (derived methods,
> `@Query`, o `Specification`/Criteria dinámico).
>
> Las consultas marcadas **[Specification dinámica]** no son una sola sentencia fija: el
> `WHERE` se arma en tiempo de ejecución combinando solo los filtros que el usuario mandó. Se
> documenta el peor caso (todos los filtros activos) para poder probarlo tal cual.
>
> Reemplaza los `?` por valores reales de tu base al probar cada consulta en un cliente MySQL.

---

## Índice

1. [AdministracionController](#1-administracioncontroller--apiadministracion-solo-admin)
2. [AlertaController](#2-alertacontroller--apialertas)
3. [AuthController](#3-authcontroller--apiauth)
4. [CatalogoController](#4-catalogocontroller--apicatalogos)
5. [ChecklistPreInyeccionController](#5-checklistpreinyeccioncontroller--apichecklists)
6. [DashboardController](#6-dashboardcontroller--apidashboard)
7. [ExtravasacionController](#7-extravasacioncontroller--apiextravasaciones)
8. [HistorialAccesoController](#8-historialaccesocontroller--apihistorial-accesos-solo-admin)
9. [IntegracionClinicaController](#9-integracionclinicacontroller--apiintegracion-clinica)
10. [InyeccionController](#10-inyeccioncontroller--apiinyecciones)
11. [LoteController](#11-lotecontroller--apilotes)
12. [MantenimientoPredictivoController](#12-mantenimientopredictivocontroller--apimantenimiento-predictivo)
13. [MermaController](#13-mermacontroller--apiinsumosmermas)
14. [PacienteController](#14-pacientecontroller--apipacientes)
15. [PedidoReabastecimientoController](#15-pedidoreabastecimientocontroller--apipedidos-reabastecimiento)
16. [PermisosController](#16-permisoscontroller--apimepermisos)
17. [ReporteEjecutivoController](#17-reporteejecutivocontroller--apireportes)
18. [TicketSoporteController](#18-ticketsoportecontroller--apitickets-soporte)
19. [UsuarioController](#19-usuariocontroller--apiusuarios-solo-admin)
20. [Notas transversales](#20-notas-transversales)

---

## 1. AdministracionController — `/api/administracion` (solo ADMIN)

Service: `PermisoService`. Repos: `ModuloRepository`, `PermisoRepository`, `RolRepository`, `RolModuloPermisoRepository`, `UsuarioRepository`.

### GET /modulos
Lista el catálogo de módulos, para la pantalla "Roles y permisos".
```sql
SELECT * FROM modulos ORDER BY orden ASC;
```

### GET /permisos
Lista el catálogo de acciones (VER/CREAR/EDITAR/ELIMINAR/EXPORTAR).
```sql
SELECT * FROM permisos ORDER BY id ASC;
```

### GET /roles
Lista los 5 roles con el conteo de usuarios que tiene cada uno.
```sql
SELECT * FROM roles;
-- por cada rol:
SELECT COUNT(*) FROM usuarios WHERE rol_id = ?;
```

### GET /roles/{rolId}/matriz
Trae la matriz completa (módulos × permisos) marcando cuáles ya tiene ese rol.
```sql
SELECT * FROM roles WHERE id = ?;
SELECT * FROM modulos ORDER BY orden ASC;
SELECT * FROM permisos ORDER BY id ASC;
SELECT * FROM rol_modulo_permiso WHERE rol_id = ?;
```

### POST /roles/{rolId}/matriz/otorgar
Otorga un permiso puntual (rol+módulo+permiso) y registra auditoría.
```sql
SELECT COUNT(*)>0 FROM rol_modulo_permiso WHERE rol_id=? AND modulo_id=? AND permiso_id=?;
INSERT INTO rol_modulo_permiso (rol_id, modulo_id, permiso_id) VALUES (?, ?, ?);
INSERT INTO auditoria (...);
```

### DELETE /roles/{rolId}/matriz/revocar
Revoca un permiso puntual.
```sql
DELETE FROM rol_modulo_permiso WHERE rol_id=? AND modulo_id=? AND permiso_id=?;
INSERT INTO auditoria (...);
```

---

## 2. AlertaController — `/api/alertas`

Service: `AlertaService`. Repos: `AlertaSistemaRepository`, `InyectorRepository`. Tabla `alertas_sistema`.

### GET /api/alertas **[Specification dinámica]**
Lista alertas del sistema (EDA/mantenimiento/stock), paginado y ordenado por fecha descendente.
```sql
SELECT * FROM alertas_sistema
WHERE resuelta = ? AND severidad = ? AND tipo = ?
ORDER BY fecha_hora DESC LIMIT ? OFFSET ?;
```
Cada filtro (`resuelta`, `severidad`, `tipo`) es opcional e independiente — pruébalos por separado también.

### POST /api/alertas
Crea una alerta manual (además publica por WebSocket a `/topic/alertas`, sin efecto en BD).
```sql
INSERT INTO alertas_sistema (tipo, severidad, inyector_id, mensaje, fecha_hora, resuelta) VALUES (...);
```

### PATCH /{id}/resolver
Marca una alerta como resuelta.
```sql
UPDATE alertas_sistema SET resuelta = true, fecha_resolucion = ? WHERE id = ?;
```

---

## 3. AuthController — `/api/auth`

Services: `AuthService`, `HistorialAccesoService`. Repos: `UsuarioRepository`, `RolRepository`, `TokenRecuperacionPasswordRepository`, `TokenRefrescoRepository`, `HistorialAccesoRepository`.

### POST /login
Valida credenciales, registra el intento en `historial_accesos`, bloquea la cuenta tras varios fallos, y emite JWT + refresh token.
```sql
SELECT * FROM usuarios WHERE email = ?;
INSERT INTO historial_accesos (usuario_id, email_usado, exitoso, fecha_hora, ...) VALUES (...);
SELECT COUNT(*) FROM historial_accesos WHERE email_usado = ? AND exitoso = false AND fecha_hora > ?;
UPDATE usuarios SET bloqueado_hasta = ? WHERE id = ?;   -- solo si se dispara el bloqueo
INSERT INTO tokens_refresco (usuario_id, token, expira_en, revocado) VALUES (...);
```

### POST /refresh
Renueva el access token con un refresh token de un solo uso.
```sql
SELECT * FROM tokens_refresco WHERE token = ?;
UPDATE tokens_refresco SET revocado = true WHERE id = ?;
INSERT INTO tokens_refresco (...) VALUES (...);   -- el nuevo
```

### POST /logout
Revoca el refresh token activo.
```sql
SELECT * FROM tokens_refresco WHERE token = ?;
UPDATE tokens_refresco SET revocado = true WHERE id = ?;
```

### POST /registro
Alta de cuenta nueva con rol VISUALIZADOR por defecto.
```sql
SELECT * FROM usuarios WHERE email = ?;
SELECT * FROM roles WHERE nombre = 'VISUALIZADOR';
INSERT INTO usuarios (email, password_hash, nombre_completo, rol_id, activo, proveedor) VALUES (...);
```

### POST /olvidar-password
Genera token de recuperación (no revela si el correo existe).
```sql
SELECT * FROM usuarios WHERE email = ?;
INSERT INTO tokens_recuperacion_password (usuario_id, token, expira_en, usado) VALUES (...);
```

### POST /restablecer-password
Fija contraseña nueva con el token de recuperación.
```sql
SELECT * FROM tokens_recuperacion_password WHERE token = ?;
UPDATE usuarios SET password_hash = ? WHERE id = ?;
UPDATE tokens_recuperacion_password SET usado = true WHERE id = ?;
```

### GET /me
Perfil del usuario autenticado (claim `sub` del JWT = email).
```sql
SELECT * FROM usuarios WHERE email = ?;
```

---

## 4. CatalogoController — `/api/catalogos`

Service: `CatalogoService`. Repos: `SedeRepository`, `SalaRepository`, `InyectorRepository`, `ProtocoloInyeccionRepository`, `AgenteContrasteRepository`, `IdentificadorAnatomicoRepository`, `RolRepository`. Todos alimentan `<select>` del frontend.

```sql
-- GET /roles
SELECT * FROM roles;

-- GET /sedes
SELECT * FROM sedes;

-- GET /salas?sedeId=       (sin sedeId)
SELECT * FROM salas;
-- GET /salas?sedeId=X      (con sedeId)
SELECT * FROM salas WHERE sede_id = ? AND activo = true;

-- GET /inyectores?salaId=  (sin salaId)
SELECT * FROM inyectores;
-- GET /inyectores?salaId=X (con salaId)
SELECT * FROM inyectores WHERE sala_id = ?;

-- GET /protocolos?identificadorAnatomicoId=   (sin filtro)
SELECT * FROM protocolos_inyeccion WHERE activo = true;
-- GET /protocolos?identificadorAnatomicoId=X  (con filtro)
SELECT * FROM protocolos_inyeccion WHERE identificador_anatomico_id = ? AND activo = true;

-- GET /agentes-contraste
SELECT * FROM agentes_contraste WHERE activo = true;

-- GET /identificadores-anatomicos
SELECT * FROM identificadores_anatomicos;
```

---

## 5. ChecklistPreInyeccionController — `/api/checklists`

Service: `ChecklistPreInyeccionService`. Tabla `checklist_pre_inyeccion` (FKs `paciente_id`, `sala_id`, `operador_id`). Recuerda: esta tabla NO tiene FK a `inyecciones` (checklist ocurre antes de la inyección, por diseño).

### GET /paciente/{pacienteId}
Historial de checklists firmados de un paciente.
```sql
SELECT * FROM checklist_pre_inyeccion WHERE paciente_id = ? ORDER BY fecha_hora DESC;
```

### POST /api/checklists
Crea un checklist firmado (identidad/GFR/alergias/consentimiento son obligatorios a nivel DTO).
```sql
SELECT * FROM pacientes WHERE id = ?;
SELECT * FROM salas WHERE id = ?;              -- si se manda salaId
SELECT * FROM usuarios WHERE email = ?;         -- operador, resuelto del JWT
INSERT INTO checklist_pre_inyeccion (paciente_id, sala_id, operador_id, identidad_verificada,
  gfr_revisado, gfr_valor_momento, riesgo_renal_momento, alergias_revisadas, alergias_momento,
  consentimiento_firmado, observaciones, firma_nombre, firma_imagen_base64, fecha_hora) VALUES (...);
```

---

## 6. DashboardController — `/api/dashboard`

Service: `DashboardService`. Repos: `InyeccionRepository`, `InyectorRepository`, `EventoExtravasacionRepository`. Alimenta el "Panel de inyectores de contraste".

### GET /kpis
KPIs del rango de fechas: total inyecciones, volumen, inyectores activos/totales, alertas EDA.
```sql
SELECT COUNT(*) FROM inyecciones WHERE fecha_hora_inicio BETWEEN ? AND ?;
SELECT COALESCE(SUM(volumen_total_ml),0) FROM inyecciones WHERE fecha_hora_inicio BETWEEN ? AND ?;
SELECT COUNT(*) FROM inyectores WHERE estado = 'ACTIVO';
SELECT COUNT(*) FROM inyectores;
SELECT COUNT(*) FROM eventos_extravasacion WHERE estado_eda = 'FUERA_DE_RANGO' AND fecha_hora BETWEEN ? AND ?;
```

### GET /uso-contraste
Serie diaria de volumen usado, para la gráfica de uso de contraste.
```sql
SELECT DATE(fecha_hora_inicio) AS dia, COALESCE(SUM(volumen_total_ml),0) AS volumen, COUNT(*) AS total
FROM inyecciones
WHERE fecha_hora_inicio BETWEEN ? AND ?
GROUP BY DATE(fecha_hora_inicio)
ORDER BY DATE(fecha_hora_inicio);
```

### GET /distribucion-protocolo
Cuántas inyecciones hubo por región anatómica, para la gráfica de distribución.
```sql
SELECT ia.nombre, COUNT(*) AS total
FROM inyecciones i JOIN identificadores_anatomicos ia ON i.identificador_anatomico_id = ia.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?
GROUP BY ia.nombre
ORDER BY total DESC;
```

---

## 7. ExtravasacionController — `/api/extravasaciones`

Service: `ExtravasacionService`. Tabla `eventos_extravasacion` (FKs `inyeccion_id`, `revisado_por`).

### GET /api/extravasaciones **[Criteria dinámico]**
Lista eventos EDA, con filtros opcionales y restricción de sede para roles no-ADMIN.
```sql
SELECT e.* FROM eventos_extravasacion e
JOIN inyecciones i ON e.inyeccion_id = i.id
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
WHERE e.fecha_hora >= ? AND e.fecha_hora <= ?
  AND e.estado_eda = ? AND e.revisado = ? AND sa.sede_id = ?
ORDER BY e.fecha_hora DESC LIMIT ? OFFSET ?;
```

### PATCH /{id}/revisar
Marca un evento EDA como revisado por un radiólogo/técnico, con acción tomada y notas.
```sql
UPDATE eventos_extravasacion
SET revisado = true, revisado_por = ?, fecha_revision = ?, accion_tomada = ?, notas = ?
WHERE id = ?;
```

---

## 8. HistorialAccesoController — `/api/historial-accesos` (solo ADMIN)

Service: `HistorialAccesoService`. Tabla `historial_accesos`.

### GET /api/historial-accesos
Log completo de logins (exitosos y fallidos), paginado.
```sql
SELECT * FROM historial_accesos ORDER BY fecha_hora DESC LIMIT ? OFFSET ?;
```

### GET /usuario/{usuarioId}
Log de accesos de un usuario específico.
```sql
SELECT * FROM historial_accesos WHERE usuario_id = ? ORDER BY fecha_hora DESC LIMIT ? OFFSET ?;
```

---

## 9. IntegracionClinicaController — `/api/integracion-clinica`

Services: `HisIntegracionService` (simulado), `SincronizacionInyectorService`, `IntegracionClinicaService`.

### GET /his/{identificadorExterno}
Busca un paciente en el HIS simulado (en realidad consulta la propia BD local marcando `"simulado": true`).
```sql
SELECT * FROM pacientes WHERE identificador_externo = ?;
```

### POST /his/{identificadorExterno}/sincronizar
Sincroniza (crea/actualiza) el paciente local a partir del HIS simulado.
```sql
SELECT * FROM pacientes WHERE identificador_externo = ?;
INSERT INTO pacientes (...) VALUES (...);   -- o UPDATE si ya existe
```

### POST /sincronizar-inyector
Importa el CSV del inyector: crea pacientes/inyecciones/fases nuevas por cada fila. Es el ÚNICO punto del sistema que crea filas en `inyecciones` (no hay flujo manual de "crear inyección").
```sql
SELECT * FROM inyectores;                    -- match por numero_serie, filtrado en Java
SELECT * FROM pacientes WHERE identificador_externo = ?;   -- match por MRN del CSV
INSERT INTO pacientes (...) VALUES (...);     -- si no existe
SELECT * FROM protocolos_inyeccion;           -- match por nombre, filtrado en Java
SELECT * FROM agentes_contraste;              -- match por nombre, filtrado en Java
INSERT INTO inyecciones (...) VALUES (...);
INSERT INTO inyeccion_fases (...) VALUES (...);   -- una o mas por inyeccion
INSERT INTO lotes_sincronizacion (...) VALUES (...);
```

### GET /historial-sincronizacion
Historial de corridas de sincronización (manuales o automáticas cada 15 min).
```sql
SELECT * FROM lotes_sincronizacion ORDER BY fecha_hora DESC LIMIT ? OFFSET ?;
```

---

## 10. InyeccionController — `/api/inyecciones`

Service: `InyeccionService`. Tabla central `inyecciones`.

### GET /api/inyecciones **[Specification dinámica]**
Listado con filtros del dashboard operativo. `sedeId` puede ser forzado por restricción de sede del usuario autenticado (DEF-03).
```sql
SELECT DISTINCT i.* FROM inyecciones i
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
JOIN sedes se ON sa.sede_id = se.id
LEFT JOIN inyeccion_fases f ON f.inyeccion_id = i.id
LEFT JOIN eventos_extravasacion e ON e.inyeccion_id = i.id
WHERE i.fecha_hora_inicio >= ? AND i.fecha_hora_inicio <= ?
  AND se.id = ? AND sa.id = ? AND inv.id = ? AND i.protocolo_id = ?
  AND i.identificador_anatomico_id = ? AND f.agente_id = ? AND i.estado = ?
  AND e.estado_eda = 'FUERA_DE_RANGO'
ORDER BY i.fecha_hora_inicio DESC LIMIT ? OFFSET ?;
```

### GET /{id}/presion
Serie de tiempo de presión, para la gráfica de presión.
```sql
SELECT * FROM inyeccion_serie_presion WHERE inyeccion_id = ? ORDER BY tiempo_seg ASC;
```

### PATCH /{id}/dosis-radiacion
Registra CTDIvol/DLP de un estudio.
```sql
SELECT * FROM inyecciones WHERE id = ?;
UPDATE inyecciones SET ctdi_vol_mgy = ?, dlp_mgy_cm = ? WHERE id = ?;
```

### GET /{id}/detalle-completo
Pantalla de detalle de inyección (dashboard paciente v2): paciente, contraste/lote, series de presión y flujo, comparativo Planeado/Programado/Real.
```sql
SELECT * FROM inyecciones WHERE id = ?;
SELECT * FROM inyeccion_serie_presion WHERE inyeccion_id = ? ORDER BY tiempo_seg ASC;
SELECT * FROM inyeccion_serie_flujo WHERE inyeccion_id = ? ORDER BY tiempo_seg ASC;
SELECT * FROM protocolo_fases WHERE protocolo_id = ? ORDER BY numero_fase ASC;
SELECT * FROM inyeccion_fases_programadas WHERE inyeccion_id = ? ORDER BY orden_fase ASC;
SELECT * FROM inyeccion_fases WHERE inyeccion_id = ?;
```

---

## 11. LoteController — `/api/lotes`

Service: `LoteService`. Tabla `lotes_agente_contraste`.

### GET /api/lotes **[Specification dinámica]**
Listado de lotes con trazabilidad e indicador visual de "ya tiene inyecciones".
```sql
SELECT * FROM lotes_agente_contraste
WHERE sede_id = ? AND agente_id = ? AND fecha_caducidad >= CURDATE()
  AND fecha_caducidad BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
ORDER BY fecha_caducidad ASC LIMIT ? OFFSET ?;
-- indicador "tieneInyecciones" (batch, una sola query por pagina):
SELECT DISTINCT lote_agente_id FROM inyeccion_fases WHERE lote_agente_id IN (?, ?, ...);
```

### POST /api/lotes
Da de alta un lote nuevo (valida duplicado por número de lote + agente + sede).
```sql
SELECT * FROM agentes_contraste WHERE id = ?;
SELECT * FROM sedes WHERE id = ?;
SELECT * FROM lotes_agente_contraste WHERE numero_lote = ? AND agente_id = ? AND sede_id = ?;
INSERT INTO lotes_agente_contraste (...) VALUES (...);
INSERT INTO auditoria (...);
```

### GET /{id}/trazabilidad
Trazabilidad de un lote hasta el paciente — la consulta clave ante un recall.
```sql
SELECT f.* FROM inyeccion_fases f JOIN inyecciones i ON f.inyeccion_id = i.id
WHERE f.lote_agente_id = ? ORDER BY i.fecha_hora_inicio DESC;
```

---

## 12. MantenimientoPredictivoController — `/api/mantenimiento-predictivo`

Service: `MantenimientoPredictivoService`. Repos: `InyectorRepository`, `InyeccionRepository`, `MantenimientoInyectorRepository`.

### GET /predicciones
Predicción de falla por umbral de ciclos (constante `600` en código, no en BD).
```sql
SELECT * FROM inyectores;   -- filtrado a ACTIVO en Java
-- por cada inyector:
SELECT COUNT(*) FROM inyecciones WHERE inyector_id = ? AND fecha_hora_inicio > ?;
```

### GET /calendario-calibracion
Última calibración por inyector (intervalo `365` días en código).
```sql
SELECT * FROM inyectores;
-- por cada inyector:
SELECT * FROM mantenimientos_inyector WHERE inyector_id = ? AND tipo = 'CALIBRACION'
ORDER BY fecha_inicio DESC LIMIT 1;
```

---

## 13. MermaController — `/api/insumos/mermas`

Service: `MermaService`. Repo: `InyeccionFaseRepository`. Todas las consultas de merma parten de `inyeccion_fases` (volumen_programado_ml − volumen_real_ml), no de `inyecciones.volumen_residual_ml`, para que los 4 números (KPI, por sede, por insumo, por inyección) sean consistentes entre sí.

### GET /resumen-filtrado **[query con filtros opcionales]**
Merma total replicando TODOS los filtros de la barra del dashboard (fecha, sede, sala, agente, región anatómica, estado, solo-con-alerta-EDA).
```sql
SELECT COALESCE(SUM(f.volumen_programado_ml),0), COALESCE(SUM(f.volumen_real_ml),0)
FROM inyeccion_fases f
JOIN inyecciones i ON f.inyeccion_id = i.id
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
JOIN sedes se ON sa.sede_id = se.id
WHERE (? IS NULL OR i.fecha_hora_inicio >= ?)
  AND (? IS NULL OR i.fecha_hora_inicio <= ?)
  AND (? IS NULL OR se.id = ?)
  AND (? IS NULL OR sa.id = ?)
  AND (? IS NULL OR f.agente_id = ?)
  AND (? IS NULL OR i.identificador_anatomico_id = ?)
  AND (? IS NULL OR i.estado = ?)
  AND (? = false OR EXISTS (
        SELECT 1 FROM eventos_extravasacion e WHERE e.inyeccion_id = i.id AND e.estado_eda = 'FUERA_DE_RANGO'));
```

### GET /resumen
Merma total del periodo completo (solo fecha), llamada dos veces para calcular tendencia (periodo actual vs. anterior).
```sql
SELECT COALESCE(SUM(f.volumen_programado_ml),0), COALESCE(SUM(f.volumen_real_ml),0)
FROM inyeccion_fases f JOIN inyecciones i ON f.inyeccion_id = i.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?;
```

### GET /por-sede
Desglose de merma por sede.
```sql
SELECT s.id, s.nombre, COALESCE(SUM(f.volumen_programado_ml),0), COALESCE(SUM(f.volumen_real_ml),0)
FROM inyeccion_fases f
JOIN inyecciones i ON f.inyeccion_id = i.id
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
JOIN sedes s ON sa.sede_id = s.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?
GROUP BY s.id, s.nombre ORDER BY s.nombre;
```

### GET /por-insumo
Desglose de merma por agente de contraste/insumo.
```sql
SELECT ag.id, ag.nombre_comercial, ag.tipo, ag.fabricante,
       COALESCE(SUM(f.volumen_programado_ml),0), COALESCE(SUM(f.volumen_real_ml),0)
FROM inyeccion_fases f
JOIN inyecciones i ON f.inyeccion_id = i.id
JOIN agentes_contraste ag ON f.agente_id = ag.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?
GROUP BY ag.id, ag.nombre_comercial, ag.tipo, ag.fabricante
ORDER BY ag.nombre_comercial;
```

### GET /por-inyeccion
Tabla "Detalle por inyección", paginada, ordenada por fecha más reciente primero (julio 2026: antes era por merma descendente).
```sql
SELECT i.id, i.fecha_hora_inicio, p.nombre_completo, p.numero_expediente, s.nombre, sa.nombre,
       i.estado, i.motivo_aborto, COALESCE(SUM(f.volumen_programado_ml),0), COALESCE(SUM(f.volumen_real_ml),0)
FROM inyeccion_fases f
JOIN inyecciones i ON f.inyeccion_id = i.id
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
JOIN sedes s ON sa.sede_id = s.id
LEFT JOIN pacientes p ON i.paciente_id = p.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?
GROUP BY i.id, i.fecha_hora_inicio, p.nombre_completo, p.numero_expediente, s.nombre, sa.nombre, i.estado, i.motivo_aborto
ORDER BY i.fecha_hora_inicio DESC
LIMIT ? OFFSET ?;
```

---

## 14. PacienteController — `/api/pacientes`

Service: `PacienteService`. Repos: `PacienteRepository`, `InyeccionRepository`, `EventoExtravasacionRepository`.

### GET /api/pacientes **[Specification dinámica]**
Búsqueda por identificador externo o nombre, con restricción de sede vía `EXISTS` correlacionado.
```sql
SELECT * FROM pacientes p
WHERE (LOWER(p.identificador_externo) LIKE ? OR LOWER(p.nombre_completo) LIKE ?)
  AND EXISTS (
    SELECT 1 FROM inyecciones i JOIN inyectores inv ON i.inyector_id = inv.id
    JOIN salas sa ON inv.sala_id = sa.id
    WHERE i.paciente_id = p.id AND sa.sede_id = ?)
ORDER BY p.nombre_completo ASC LIMIT ? OFFSET ?;
```

### GET /{id}
Ficha clínica completa con métricas agregadas (usadas en la tarjeta de resumen del dashboard de paciente).
```sql
-- chequeo de acceso por sede (si aplica):
SELECT EXISTS(SELECT 1 FROM inyecciones i JOIN inyectores inv ON i.inyector_id=inv.id
  JOIN salas sa ON inv.sala_id=sa.id WHERE i.paciente_id=? AND sa.sede_id=?);
SELECT * FROM pacientes WHERE id = ?;
SELECT COUNT(*) FROM inyecciones WHERE paciente_id = ?;
SELECT COALESCE(SUM(volumen_total_ml),0) FROM inyecciones WHERE paciente_id = ?;
SELECT COUNT(*) FROM inyecciones WHERE paciente_id = ? AND estado IN ('ABORTADA','ERROR');
SELECT COUNT(*) FROM eventos_extravasacion e JOIN inyecciones i ON e.inyeccion_id=i.id
  WHERE i.paciente_id = ? AND e.estado_eda = 'FUERA_DE_RANGO';
SELECT COALESCE(SUM(dlp_mgy_cm),0) FROM inyecciones WHERE paciente_id = ?;
SELECT MAX(fecha_hora_inicio) FROM inyecciones WHERE paciente_id = ?;
```

### GET /{id}/inyecciones
Historial de inyecciones del paciente (con número de inyección, fecha, etc.).
```sql
SELECT * FROM inyecciones WHERE paciente_id = ? ORDER BY fecha_hora_inicio DESC;
```

### GET /{id}/reacciones
Historial de eventos EDA/reacciones del paciente.
```sql
SELECT e.* FROM eventos_extravasacion e JOIN inyecciones i ON e.inyeccion_id = i.id
WHERE i.paciente_id = ? ORDER BY e.fecha_hora DESC;
```

---

## 15. PedidoReabastecimientoController — `/api/pedidos-reabastecimiento`

Service: `PedidoReabastecimientoService`. Tabla `pedidos_reabastecimiento`.

### GET /api/pedidos-reabastecimiento
Lista todos los pedidos (paginación es client-side en el frontend; el backend regresa todo).
```sql
SELECT * FROM pedidos_reabastecimiento;   -- ordenado por fecha_solicitud DESC en Java
```

### PATCH /{id}
Cambia el estado de un pedido (PENDIENTE/ENVIADO/RECIBIDO/CANCELADO).
```sql
SELECT * FROM pedidos_reabastecimiento WHERE id = ?;
UPDATE pedidos_reabastecimiento SET estado = ?, fecha_envio = ?, fecha_recepcion = ? WHERE id = ?;
```

> Nota: `existsBySede_IdAndAgente_IdAndEstadoIn` lo usa el scheduler automático de reorden
> (`PedidoReabastecimientoService.generarAutomatico()`), no este controller directamente.

---

## 16. PermisosController — `/api/me/permisos`

Service: `PermisoService`. Repos: `UsuarioRepository`, `RolModuloPermisoRepository`.

### GET /api/me/permisos
Trae los permisos efectivos del usuario autenticado, para gatear el menú/rutas de Angular.
```sql
SELECT u.*, r.* FROM usuarios u JOIN roles r ON u.rol_id = r.id WHERE u.email = ?;
SELECT m.codigo, p.codigo
FROM rol_modulo_permiso rmp
JOIN roles r ON rmp.rol_id = r.id
JOIN modulos m ON rmp.modulo_id = m.id
JOIN permisos p ON rmp.permiso_id = p.id
WHERE r.nombre = ?;
```

> Relacionado (no expuesto por controller, pero corre en CADA endpoint con `@RequierePermiso`,
> vía `PermisoAspect`):
> ```sql
> SELECT COUNT(*)>0 FROM rol_modulo_permiso rmp
> JOIN roles r ON rmp.rol_id=r.id JOIN modulos m ON rmp.modulo_id=m.id JOIN permisos p ON rmp.permiso_id=p.id
> WHERE r.nombre=? AND m.codigo=? AND p.codigo=?;
> ```

---

## 17. ReporteEjecutivoController — `/api/reportes`

Service: `ReporteEjecutivoService`. Repo: `InyeccionRepository`.

### GET /comparativa-sedes
Comparativa entre sedes: total de inyecciones, volumen, y fallidas (ABORTADA/ERROR).
```sql
SELECT s.id, s.nombre, COUNT(*) AS totalInyecciones, COALESCE(SUM(i.volumen_total_ml),0) AS volumenTotal,
       SUM(CASE WHEN i.estado IN ('ABORTADA','ERROR') THEN 1 ELSE 0 END) AS fallidas
FROM inyecciones i
JOIN inyectores inv ON i.inyector_id = inv.id
JOIN salas sa ON inv.sala_id = sa.id
JOIN sedes s ON sa.sede_id = s.id
WHERE i.fecha_hora_inicio BETWEEN ? AND ?
GROUP BY s.id, s.nombre ORDER BY s.nombre;
```

### GET /comparativa-sedes/excel
Misma consulta de arriba; el `.xlsx` se arma en memoria (Apache POI), sin tocar disco ni BD adicional.

---

## 18. TicketSoporteController — `/api/tickets-soporte`

Service: `TicketSoporteService`. Tabla `tickets_soporte`.

```sql
-- GET /api/tickets-soporte
SELECT * FROM tickets_soporte;   -- ordenado por fecha_creacion DESC en Java

-- GET /inyector/{inyectorId}
SELECT * FROM tickets_soporte WHERE inyector_id = ? ORDER BY fecha_creacion DESC;

-- POST /api/tickets-soporte
SELECT * FROM inyectores WHERE id = ?;
SELECT * FROM usuarios WHERE email = ?;
INSERT INTO tickets_soporte (inyector_id, creado_por, estado, prioridad, descripcion, fecha_creacion, ...) VALUES (...);

-- PATCH /{id}
SELECT * FROM tickets_soporte WHERE id = ?;
UPDATE tickets_soporte SET estado=?, numero_ticket_fabricante=?, respuesta_fabricante=?, fecha_cierre=? WHERE id=?;
```

---

## 19. UsuarioController — `/api/usuarios` (solo ADMIN)

Service: `UsuarioService`. Tabla `usuarios`.

### GET /api/usuarios **[Specification dinámica]**
Listado de usuarios con filtro por sede/rol/activo/búsqueda, más 2 sub-consultas por fila (sesión activa, último acceso).
```sql
SELECT * FROM usuarios
WHERE sede_id = ? AND rol_id = ? AND activo = ?
  AND (LOWER(nombre_completo) LIKE ? OR LOWER(email) LIKE ?)
ORDER BY nombre_completo ASC LIMIT ? OFFSET ?;
-- por cada fila:
SELECT EXISTS(SELECT 1 FROM tokens_refresco WHERE usuario_id=? AND revocado=false AND expira_en > ?);
SELECT * FROM historial_accesos WHERE usuario_id=? AND exitoso=true ORDER BY fecha_hora DESC LIMIT 1;
```

### GET /{id}
Detalle de un usuario (misma fórmula de sesión activa + último acceso).
```sql
SELECT * FROM usuarios WHERE id = ?;
SELECT EXISTS(SELECT 1 FROM tokens_refresco WHERE usuario_id=? AND revocado=false AND expira_en > ?);
SELECT * FROM historial_accesos WHERE usuario_id=? AND exitoso=true ORDER BY fecha_hora DESC LIMIT 1;
```

### POST /api/usuarios
Alta de usuario por un ADMIN.
```sql
SELECT * FROM usuarios WHERE email = ?;
SELECT * FROM roles WHERE id = ?;
SELECT * FROM sedes WHERE id = ?;
INSERT INTO usuarios (...) VALUES (...);
INSERT INTO auditoria (...);
```

### PUT /{id}
Edición de nombre/rol/sede.
```sql
SELECT * FROM usuarios WHERE id = ?;
SELECT * FROM roles WHERE id = ?;
SELECT * FROM sedes WHERE id = ?;
UPDATE usuarios SET nombre_completo=?, rol_id=?, sede_id=? WHERE id=?;
INSERT INTO auditoria (...);
```

### PATCH /{id}/estado
Activa/desactiva una cuenta.
```sql
SELECT * FROM usuarios WHERE id = ?;
UPDATE usuarios SET activo = ? WHERE id = ?;
INSERT INTO auditoria (...);
```

---

## 20. Notas transversales

- **Sin SQL nativo/JDBC**: confirmado por grep en todo `service/` — cero `JdbcTemplate`,
  `createNativeQuery`, `createQuery` sueltos. Todo pasa por repositorios Spring Data.

- **Specifications dinámicas** (no son una sola sentencia fija — el `WHERE` depende de qué
  filtros mande el usuario): `InyeccionSpecification` (InyeccionController), `LoteAgenteContrasteSpecification`
  (LoteController), `PacienteSpecification` (PacienteController), `UsuarioSpecification`
  (UsuarioController), `AlertaSistemaSpecification` (AlertaController). Más 2 Criteria inline
  (no en clase aparte): `ExtravasacionService.buscar()` y `DashboardService.kpis()`.

- **Jobs automáticos que tocan la BD pero NO los dispara ningún controller directamente**
  (útiles de conocer si estás probando comportamiento de datos en vivo, no solo por endpoint):
  - `AlertasAutomaticasScheduler` — cada minuto revisa EDA fuera de rango sin alerta generada;
    diario revisa mantenimiento vencido y stock bajo (además corre una vez al arrancar).
    ```sql
    SELECT * FROM eventos_extravasacion WHERE estado_eda = 'FUERA_DE_RANGO' AND alerta_generada = false;
    SELECT * FROM inventario_agentes WHERE cantidad_actual <= umbral_minimo;   -- via conStockBajo()
    SELECT * FROM inyectores WHERE ...;                                        -- via conMantenimientoVencido()
    ```
  - `SincronizacionInyectorService.sincronizarProgramado()` — cada 15 min, mismas consultas que
    `POST /sincronizar-inyector` (sección 9), cuando `app.sincronizacion.habilitada=true`
    (deshabilitado por defecto).

- **Tabla sin consultas propias**: `auditoria` — solo `INSERT` vía `AuditoriaService.registrar()`
  (invocado desde `PermisoService`, `LoteService`, `UsuarioService`), sin ningún `SELECT`
  expuesto por controller todavía.

- **Posible tabla/entidad muerta**: `EtiquetaImpresa` (tabla `etiquetas_impresas`) no tiene
  métodos de consulta propios más allá del `JpaRepository` base, y ningún service/controller
  la referencia en el código revisado — parece no usarse actualmente. No se tocó ni se
  eliminó; queda señalado aquí para que lo confirmes.

- **Restricción de sede (DEF-03)**: varios endpoints (`InyeccionController`, `PacienteController`,
  `ExtravasacionController`, `LoteController`, `UsuarioController` según filtros) fuerzan o
  agregan `sede_id = :sedeRestriccion` server-side para roles no-ADMIN, vía
  `UsuarioAutenticadoService.sedeIdRestriccion()` — si pruebas estas consultas manualmente como
  si fueras un usuario restringido, agrega ese filtro a mano.
