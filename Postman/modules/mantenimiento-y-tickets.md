# Modulo 10 - Mantenimiento y Tickets

## Objetivo

Cubrir con pruebas de Postman/Newman los dos sub-modulos que giran en torno al ciclo de vida del inyector como activo fisico: **mantenimiento predictivo** (`MantenimientoPredictivoController`, `/api/mantenimiento-predictivo`, solo lectura, calculado en Java) y **tickets de soporte con el fabricante** (`TicketSoporteController`, `/api/tickets-soporte`).

**Justificacion de la agrupacion**: ambos consumen el mismo activo (`Inyector`) y su historial de mantenimientos (`MantenimientoInyector`); las predicciones le dicen a biomedica *cuando* un inyector necesita atencion, y los tickets son el mecanismo para *escalar* con el fabricante (Bracco/ACIST) cuando el equipo interno no puede resolverlo. Se documentan juntos porque comparten `inyectorId` como clave de correlacion y porque en el frontend viven en la misma seccion de "Mantenimiento".

## Responsabilidades

- **Mantenimiento predictivo**: predicciones de falla por ciclos de uso desde el ultimo mantenimiento (umbral fijo de 600 ciclos) y calendario de calibracion (intervalo estandar de 365 dias desde la ultima calibracion registrada). **Es de solo lectura** — no hay CRUD de planes preventivos ni de refacciones (confirmado ausente en el codigo, ver Limitaciones).
- **Tickets de soporte**: alta, listado (general y por inyector) y actualizacion de tickets de incidencia tecnica abiertos con el fabricante.

## URL base

- Mantenimiento predictivo: `{{apiBaseUrl}}/api/mantenimiento-predictivo`
- Tickets de soporte: `{{apiBaseUrl}}/api/tickets-soporte`

## Dependencias

- Requiere al menos un `Inyector` con `estado=ACTIVO` para que aparezca en `predicciones` y `calendario-calibracion` (los inactivos se filtran explicitamente en el service).
- `CrearTicketRequest` requiere un `inyectorId` valido.
- Recomendable ejecutar "Predicciones de falla" primero para capturar un `inyectorId` real antes de crear tickets.

## Autenticación

JWT Bearer en `Authorization: Bearer {{accessToken}}`. Sin token, 401.

## Roles permitidos

**Hallazgo importante**: **ni `MantenimientoPredictivoController` ni `TicketSoporteController` tienen ninguna anotacion `@RequierePermiso` ni `@PreAuthorize`** en ninguno de sus endpoints. Cualquiera de los 5 roles con JWT valido puede ver predicciones, calendario de calibracion, listar/crear/actualizar tickets — pese a que el modulo `MANTENIMIENTO` existe en la matriz de permisos sembrada (TECNICO y BIOMEDICA tienen `VER/CREAR/EDITAR`; RADIOLOGO y VISUALIZADOR no estan en el seed de ese modulo). Igual que en Pedidos de Reabastecimiento, el modulo esta definido en el catalogo Rol x Modulo x Permiso pero **el controller nunca lo consulta**, por lo que en la practica el control de acceso real es "cualquier usuario autenticado".

## Endpoints

| Metodo | Path | Nombre en groupD.json | Body/Query clave |
|---|---|---|---|
| GET | `/api/mantenimiento-predictivo/predicciones` | Predicciones de falla por ciclos de uso | — |
| GET | `/api/mantenimiento-predictivo/calendario-calibracion` | Calendario de calibracion | — |
| GET | `/api/tickets-soporte` | Listar todos los tickets de soporte | — |
| GET | `/api/tickets-soporte/inyector/{inyectorId}` | Listar tickets por inyector | path `inyectorId` |
| POST | `/api/tickets-soporte` | Crear ticket de soporte | `inyectorId`, `titulo`, `descripcion`, `prioridad` |
| PATCH | `/api/tickets-soporte/{id}` | Actualizar ticket a EN_PROCESO | `estado`, `numeroTicketFabricante` |
| PATCH | `/api/tickets-soporte/{id}` | Cerrar ticket (estado CERRADO, fija fechaCierre) | `estado`, `respuestaFabricante` |

## Variables necesarias

- `apiBaseUrl`, `accessToken`
- `inyectorId` (capturado por "Predicciones de falla por ciclos de uso", tomando el primer inyector de la lista)
- `ticketId` (capturado por "Crear ticket de soporte")

## Datos de prueba

- `prioridad` debe ser uno de `BAJA`, `MEDIA`, `ALTA`, `CRITICA` (enum `PrioridadTicket`).
- `estado` en `ActualizarTicketRequest` debe ser uno de `ABIERTO`, `EN_PROCESO`, `ESPERANDO_FABRICANTE`, `CERRADO` (enum `EstadoTicket`).
- `titulo` maximo 200 caracteres, `descripcion` maximo 2000 caracteres (`@Size`).

## Orden recomendado

1. Predicciones de falla por ciclos de uso — captura `inyectorId`.
2. Calendario de calibracion.
3. Listar todos los tickets de soporte.
4. Crear ticket de soporte — captura `ticketId`; correr negativos (falta `inyectorId`, titulo vacio, prioridad invalida, inyector inexistente, descripcion > 2000, sin token).
5. Listar tickets por inyector.
6. Actualizar ticket a EN_PROCESO.
7. Cerrar ticket (estado CERRADO) — y sus negativos (enum invalido, ticket inexistente).

## Casos positivos

- Consultar predicciones y calendario de calibracion sin datos de entrada (son GET calculados).
- Crear un ticket con datos validos, confirmando `estado=ABIERTO` por defecto.
- Actualizar el ticket a `EN_PROCESO` con `numeroTicketFabricante`.
- Cerrarlo con `estado=CERRADO`, confirmando que `fechaCierre` se fija automaticamente.

## Casos negativos

- **Falta `inyectorId`** al crear ticket → 400, viola `@NotNull`.
- **`titulo` vacio** → 400, viola `@NotBlank`.
- **`prioridad` con valor fuera del enum** (ej. `"URGENTISIMA"`) → 400, `PrioridadTicket.valueOf(...)` lanza `IllegalArgumentException`.
- **`inyectorId` inexistente** → 400, `IllegalArgumentException` ("El inyector no existe").
- **`descripcion` que excede 2000 caracteres** → 400, viola `@Size(max=2000)`.
- **`estado` con valor fuera del enum** al actualizar ticket → 400, `EstadoTicket.valueOf(...)` lanza `IllegalArgumentException`.
- **Ticket inexistente** al actualizar → 400 ("El ticket no existe").
- **Sin token** → 401 en cualquier endpoint.

## Reglas de negocio

- **Prediccion de falla**: umbral fijo `UMBRAL_CICLOS_RIESGO = 600` ciclos (inyecciones) desde el ultimo mantenimiento; `riesgoFalla = ciclos >= 600`. Solo se consideran inyectores con `estado=ACTIVO`. Si un inyector nunca tuvo mantenimiento, se usa `LocalDate.now().minusYears(5)` como fecha base para contar ciclos.
- **Calendario de calibracion**: intervalo fijo `INTERVALO_CALIBRACION_DIAS = 365`; `proximaCalibracion = ultimaCalibracion + 365 dias` (o `hoy + 365` si nunca hubo calibracion registrada); `vencida = diasRestantes < 0`.
- **Creacion de ticket**: `estado` siempre se fuerza a `ABIERTO` en el servidor (el campo no viene en `CrearTicketRequest`, solo en `ActualizarTicketRequest`); `creadoPor` se resuelve del JWT (`Authentication` → email → `Usuario`), no se puede suplantar por body.
- **Actualizacion de ticket**: pasar `estado=CERRADO` fija `fechaCierre` automaticamente. No hay validacion de transicion de estados (se puede pasar de `ABIERTO` directo a `CERRADO`, o de `CERRADO` de vuelta a `ABIERTO` — confirmado ausente).
- No existe "crear plan preventivo" ni "registrar refacciones" (confirmado ausente, ni controller ni service lo exponen).

## Códigos HTTP esperados

| Escenario | Codigo |
|---|---|
| GET de predicciones/calendario/tickets | 200 |
| Crear ticket OK | 201 |
| Actualizar ticket OK | 200 |
| Validacion de campos o enum invalido | 400 |
| Entidad referenciada inexistente (inyector, ticket) | 400 |
| Sin token | 401 |

## Ejemplos de request/response reales

**GET `/api/mantenimiento-predictivo/predicciones`** (response, un elemento):
```json
{
  "inyectorId": 3,
  "numeroSerie": "SN-2024-0031",
  "sala": "Sala 2",
  "sede": "Hospital Central",
  "estado": "ACTIVO",
  "ciclosDesdeMantenimiento": 612,
  "umbralCiclos": 600,
  "porcentajeUso": 100,
  "riesgoFalla": true,
  "fechaUltimoMantenimiento": "2026-01-15",
  "diasDesdeMantenimiento": 183
}
```

**POST `/api/tickets-soporte`** (request):
```json
{
  "inyectorId": 3,
  "titulo": "Inyector no completa la fase de flujo",
  "descripcion": "Al iniciar el protocolo estandar el inyector se detiene en la fase 2 con error de presion.",
  "prioridad": "ALTA"
}
```

**Response 201**:
```json
{
  "id": 15,
  "inyectorId": 3,
  "inyectorNumeroSerie": "SN-2024-0031",
  "sala": "Sala 2",
  "creadoPor": "Ana Ramirez",
  "titulo": "Inyector no completa la fase de flujo",
  "descripcion": "Al iniciar el protocolo estandar el inyector se detiene en la fase 2 con error de presion.",
  "prioridad": "ALTA",
  "estado": "ABIERTO",
  "numeroTicketFabricante": null,
  "respuestaFabricante": null,
  "fechaCreacion": "2026-07-17T10:30:00",
  "fechaCierre": null
}
```

## Cómo ejecutar en Postman/Newman

1. Ejecutar el folder de autenticacion primero.
2. Ejecutar "10 - Mantenimiento y Tickets" completo, en orden (los sub-folders "Mantenimiento Predictivo" y "Tickets de Soporte" corren en secuencia).
3. Newman: `newman run ContrastIQ.postman_collection.json -e ContrastIQ.postman_environment.json --folder "10 - Mantenimiento y Tickets"`.

## Pruebas de seguridad

- Confirmar 401 sin token en todos los endpoints.
- Confirmar que **cualquier rol** (incluidos RADIOLOGO y VISUALIZADOR, que no estan en el seed de `MANTENIMIENTO`) puede consultar predicciones/calendario y crear/actualizar tickets — hallazgo de seguridad a documentar: el modulo `MANTENIMIENTO` del catalogo de permisos no se aplica en estos dos controllers.

## Pruebas de integración

- Confirmar que un ticket creado para un `inyectorId` dado aparece correctamente en `GET /api/tickets-soporte/inyector/{inyectorId}`.
- Confirmar que `riesgoFalla=true` en predicciones corresponde a inyectores que efectivamente tienen >= 600 inyecciones desde su ultimo mantenimiento (validacion cruzada contra datos de inyecciones).

## Limpieza de datos

- No hay endpoint DELETE para tickets. Los tickets creados en pruebas quedan en la BD; para limpieza real hay que hacerlo directamente en BD.

## Resultados esperados

200/201 en escenarios validos; 400 en validaciones de negocio/campo; 401 sin token. Ninguna prueba deberia devolver 403 en este modulo (no hay gate de rol/permiso implementado).

## Problemas frecuentes

- Confundir "no hay inyectores ACTIVOS" (respuesta 200 con arreglo vacio) con un error — `predicciones` y `calendario-calibracion` filtran por `estado=ACTIVO` silenciosamente.
- Copiar `estado` de `EstadoPedido` (modulo de pedidos) al actualizar un ticket — son enums distintos (`EstadoTicket` vs `EstadoPedido`), un valor valido en uno no lo es en el otro.
- Reutilizar `descripcion` corta en la prueba de "excede 2000 caracteres": el request usa variables dinamicas de Postman (`$randomLoremParagraphs`) repetidas; si el generador produce menos de 2000 caracteres la prueba deja pasar tanto 201 como 400 (documentado explicitamente en el script de esa prueba).

## Limitaciones

- Ambos controllers carecen de control de acceso por rol/permiso, pese a existir el modulo `MANTENIMIENTO` en el catalogo.
- Mantenimiento predictivo es completamente de solo lectura: no hay forma de registrar manualmente un mantenimiento/calibracion via estos dos endpoints (esa escritura, si existe, vive en otro controller no cubierto aqui).
- No hay "crear plan preventivo" ni "registrar refacciones" — confirmado ausente.
- No hay endpoint DELETE para tickets.
- No hay validacion de transicion de estado de ticket.

## Evidencias recomendadas

- Captura de un inyector con `riesgoFalla=true` y `porcentajeUso=100`.
- Captura del ciclo completo de un ticket: creado (`ABIERTO`) → `EN_PROCESO` → `CERRADO` con `fechaCierre` poblada.
- Captura del 400 por `prioridad`/`estado` con valor de enum invalido.
