# Modulo 11 - Extravasaciones

## Objetivo
Validar `ExtravasacionController` (`/api/extravasaciones`), que soporta la pantalla "Alertas de
extravasacion" (revision clinica de eventos EDA -- deteccion de extravasacion -- fuera de rango)
y el filtro "Estado: Solo alertas EDA" del dashboard.

## Responsabilidades
- `GET /api/extravasaciones`: listado paginado y filtrable de eventos de extravasacion.
- `PATCH /api/extravasaciones/{id}/revisar`: cierra el ciclo de una alerta EDA, dejando
  constancia de la accion clinica tomada.

## URL base
`{{apiBaseUrl}}/api/extravasaciones`

## Dependencias
Depende de que existan eventos en `eventos_extravasacion`, generados por el proceso que analiza
la serie de presion de cada inyeccion (fuera del alcance de este modulo). El seed
`datos_dummy_contrastiq.sql` ya carga eventos de ejemplo asociados a inyecciones existentes.

## Autenticacion
Header `Authorization: Bearer {{token}}`. A diferencia de `06 - Inyecciones` y
`07 - Dashboard Operativo`, este controller **si** usa la matriz Rol x Modulo x Permiso via
`@RequierePermiso`:
- `GET /api/extravasaciones` -> `@RequierePermiso(modulo = "EXTRAVASACIONES", permiso = "VER")`
- `PATCH /api/extravasaciones/{id}/revisar` -> `@RequierePermiso(modulo = "EXTRAVASACIONES",
  permiso = "EDITAR")`

Nota historica (julio 2026, ver comentario en el codigo y
`migration_modulo_extravasaciones.sql`): antes de esa migracion el controller no tenia ningun
`@RequierePermiso` -- cualquier usuario autenticado podia listar y revisar cualquier evento. Se
gateo al construirse la pantalla dedicada.

## Roles permitidos
Segun `migration_modulo_extravasaciones.sql` (matriz `rol_modulo_permiso` para el modulo
`EXTRAVASACIONES`):

| Rol | VER | EDITAR |
|---|---|---|
| ADMIN | si | si |
| RADIOLOGO | si | si (da el criterio clinico de revision) |
| TECNICO | si | si (opera el inyector, detecta la alerta primero) |
| BIOMEDICA | si | no (solo seguimiento del equipo) |
| VISUALIZADOR | si | no (solo lectura) |

Es decir: **los 5 roles pueden listar**, pero solo ADMIN, RADIOLOGO y TECNICO pueden marcar un
evento como revisado.

## Endpoints

| Metodo | Path | Request en la coleccion |
|---|---|---|
| GET | `/api/extravasaciones` | `GET Listar eventos de extravasacion - sin filtros` / `GET Listar eventos de extravasacion - todos los filtros` / `GET Listar eventos de extravasacion - estadoEda invalido (negativo)` |
| PATCH | `/api/extravasaciones/{id}/revisar` | `PATCH Revisar evento de extravasacion` / `PATCH Revisar evento - evento inexistente (negativo)` / `PATCH Revisar evento - accionTomada excede longitud maxima (negativo)` / `PATCH Revisar evento - notas excede longitud maxima (negativo)` / `PATCH Revisar evento - sin permiso EDITAR (negativo)` |

### Filtros de `GET /api/extravasaciones` (todos opcionales)
`desde`, `hasta` (`LocalDateTime`, ISO), `estadoEda`
(`SIN_REFERENCIA|EN_RANGO|FUERA_DE_RANGO`), `revisado` (boolean). Paginacion: `page`
(default `0`), `size` (default `20`), orden fijo `fechaHora DESC`.

### Cuerpo de `PATCH .../revisar` (`RevisarExtravasacionRequest`)
`accionTomada` (opcional, `@Size(max = 255)`), `notas` (opcional, `@Size(max = 2000)`). Ningun
campo es obligatorio (no hay `@NotBlank`/`@NotNull`): se puede revisar un evento sin escribir
nada, aunque no es el flujo clinico esperado.

## Variables necesarias
- `apiBaseUrl`, `radiologoToken` (o `tecnicoToken`/`adminToken`) para revisar.
- `biomedicaToken` (o `visualizadorToken`) para el caso negativo de permiso EDITAR.
- `eventoExtravasacionId`: se autocompleta via `pm.environment.set()` en
  `GET Listar eventos de extravasacion - sin filtros` (toma el `id` de la primera fila).

## Datos de prueba
No se crean eventos via Postman (no hay `POST` en este controller). Se depende del seed. La
accion `PATCH .../revisar` es idempotente en el sentido de que el servicio **no verifica** si el
evento ya estaba revisado -- volver a llamarla sobre el mismo evento simplemente sobreescribe
`accionTomada`/`notas`/`fechaRevision`/`revisadoPor` (ver "Casos negativos" para la aclaracion de
por que NO se incluyo "revisar un evento ya revisado" como negativo).

## Orden recomendado
1. `GET Listar eventos de extravasacion - sin filtros`
2. `GET Listar eventos de extravasacion - todos los filtros`
3. `GET Listar eventos de extravasacion - estadoEda invalido (negativo)`
4. `PATCH Revisar evento de extravasacion`
5. `PATCH Revisar evento - evento inexistente (negativo)`
6. `PATCH Revisar evento - accionTomada excede longitud maxima (negativo)`
7. `PATCH Revisar evento - notas excede longitud maxima (negativo)`
8. `PATCH Revisar evento - sin permiso EDITAR (negativo)`

## Casos positivos
- Listado sin filtros y con todos los filtros combinados (`desde`, `hasta`, `estadoEda`,
  `revisado`).
- Revision de un evento real con `radiologoToken`, confirmando `revisado=true` y
  `accionTomada`/`notas` guardadas.

## Casos negativos
- `estadoEda=NO_EXISTE` -> `EstadoEda.valueOf()` lanza `IllegalArgumentException` -> **400**.
- `PATCH .../revisar` sobre un `id` inexistente -> `IllegalArgumentException("El evento no
  existe")` -> **400** (no 404, mismo patron que `06 - Inyecciones`).
- `accionTomada` de mas de 255 caracteres -> **400** por `MethodArgumentNotValidException`
  (`@Size(max = 255)`).
- `notas` de mas de 2000 caracteres -> **400** por `@Size(max = 2000)`.
- `PATCH .../revisar` con `biomedicaToken` (rol sin permiso `EDITAR` en `EXTRAVASACIONES`) ->
  **403** via `PermisoAspect`.

> No se incluye "revisar un evento ya revisado" como caso negativo: `ExtravasacionService.revisar()`
> no comprueba el valor actual de `revisado` antes de sobreescribir -- volver a revisar un evento
> ya revisado responde **200** igual que la primera vez, simplemente reemplaza los datos de la
> revision anterior. Documentarlo como negativo hubiera sido inventar una regla que el codigo no
> implementa.

## Reglas de negocio
- **DEF-03 (aislamiento por sede)**, igual patron que `06 - Inyecciones`:
  - En el listado, si el usuario esta restringido a una sede, se agrega automaticamente el
    predicado `inyeccion.inyector.sala.sede.id = <sede del usuario>` (no es un query param
    expuesto al frontend, se calcula server-side).
  - En `PATCH .../revisar`, si la sede del evento no coincide con la del usuario restringido,
    `AccessDeniedException` -> 403 (mismo mecanismo que el detalle de inyeccion).
- `revisar()` registra automaticamente `revisadoPor` (usuario del JWT) y `fechaRevision`
  (`LocalDateTime.now()`); no se pueden enviar esos dos campos desde el body porque
  `RevisarExtravasacionRequest` no los expone.

## Codigos HTTP esperados
| Escenario | Status |
|---|---|
| Listado / revision exitosos | 200 |
| `estadoEda` invalido | 400 |
| Evento inexistente | 400 |
| `accionTomada`/`notas` fuera de longitud | 400 |
| Rol sin permiso EDITAR | 403 |
| Rol sin permiso VER (teorico; no hay rol real sin VER en este modulo) | 403 |
| Sin token / token expirado | 401 |

## Ejemplos de request/response reales

`GET {{apiBaseUrl}}/api/extravasaciones?estadoEda=FUERA_DE_RANGO&revisado=false&page=0&size=20`
```json
{
  "content": [
    {
      "id": 341,
      "inyeccionId": 88231,
      "fechaHora": "2026-07-15T09:26:12",
      "estadoEda": "FUERA_DE_RANGO",
      "revisado": false,
      "sala": "Sala TAC 2",
      "inyector": "IRIS-0245",
      "accionTomada": null
    }
  ],
  "totalElements": 37,
  "totalPages": 2,
  "number": 0,
  "size": 20
}
```

`PATCH {{apiBaseUrl}}/api/extravasaciones/341/revisar`
```json
{
  "accionTomada": "Se aplico compresa fria y se elevo la extremidad",
  "notas": "Paciente refiere disminucion del dolor a los 10 minutos. Se notifico al medico tratante."
}
```
Respuesta 200:
```json
{
  "id": 341,
  "inyeccionId": 88231,
  "fechaHora": "2026-07-15T09:26:12",
  "estadoEda": "FUERA_DE_RANGO",
  "revisado": true,
  "sala": "Sala TAC 2",
  "inyector": "IRIS-0245",
  "accionTomada": "Se aplico compresa fria y se elevo la extremidad"
}
```

## Como ejecutar en Postman/Newman
1. Cargar la coleccion y el environment con `apiBaseUrl` y los tokens de rol
   (`radiologoToken`, `tecnicoToken`, `biomedicaToken`, `adminToken`).
2. Ejecutar `11 - Extravasaciones` en orden (Postman Runner) o
   `newman run coleccion.json -e entorno.json --folder "11 - Extravasaciones"`.
3. Correr `GET Listar eventos de extravasacion - sin filtros` primero para poblar
   `{{eventoExtravasacionId}}`.

## Pruebas de seguridad
- Confirmar 401 sin `Authorization`.
- Confirmar 403 en `PATCH .../revisar` con `biomedicaToken` y `visualizadorToken`.
- Confirmar que los 5 roles pueden ejecutar `GET` sin recibir 403.
- Confirmar DEF-03: con `tecnicoToken`, el listado no debe traer eventos de otra sede aunque el
  seed los tenga.

## Pruebas de integracion
- Tomar `inyeccionId` de un evento devuelto por este modulo y cruzarlo contra
  `GET /api/inyecciones/{id}/detalle-completo` (modulo `06`) para confirmar que
  `tieneAlertaEda=true` en el resumen de esa inyeccion cuando el evento esta `FUERA_DE_RANGO`.
- Confirmar que revisar un evento no cambia su `estadoEda` (el estado EDA es un dato clinico
  calculado, no editable via este endpoint).

## Limpieza de datos
No hay `DELETE`. Si se quiere dejar un evento como "sin revisar" tras las pruebas, no es posible
revertirlo via API (`revisado` no tiene un endpoint para volver a `false`); documentar en el
reporte de ejecucion que las pruebas dejan eventos marcados como revisados de forma permanente
sobre los datos de seed usados.

## Resultados esperados
Los casos positivos devuelven 200 con la forma documentada; los negativos devuelven 400 o 403
segun corresponda, nunca 500.

## Problemas frecuentes
- **400 en vez de 404** para evento inexistente: comportamiento real, no error de la coleccion.
- **No hay proteccion contra doble revision**: si se corre la carpeta completa dos veces seguidas
  sin resetear el seed, el segundo `PATCH Revisar evento de extravasacion` sigue devolviendo 200
  (sobreescribe la revision anterior), no falla.
- **`eventoExtravasacionId` vacio**: si el seed no tiene eventos de extravasacion o no se corrio
  primero el listado, las requests de revision fallan por variable vacia.

## Limitaciones
- No hay endpoint para crear eventos de extravasacion via API (se generan por el analisis de la
  serie de presion, fuera de este modulo) ni para revertir una revision ya hecha.
- No se puede filtrar por sala/inyector directamente (solo por fecha, `estadoEda` y `revisado`);
  para cruzar por sala hay que combinar con datos de `06 - Inyecciones`.

## Evidencias recomendadas
- Captura del listado filtrado por `estadoEda=FUERA_DE_RANGO&revisado=false` antes de revisar.
- Captura de la respuesta 200 de `PATCH .../revisar` mostrando `revisado=true`.
- Captura del 403 obtenido con `biomedicaToken` en `PATCH .../revisar`, como evidencia de que la
  matriz de permisos de `migration_modulo_extravasaciones.sql` esta vigente.
