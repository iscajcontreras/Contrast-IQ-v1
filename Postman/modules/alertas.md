# Modulo 12 - Alertas

## Objetivo
Validar `AlertaController` (`/api/alertas`): alertas del sistema (mantenimiento de equipo, stock
bajo, falla de comunicacion, EDA fuera de rango, otros), incluyendo su listado historico, la
creacion manual y el marcado como resuelta.

## Responsabilidades
- `GET /api/alertas`: listado paginado y filtrable de alertas ya generadas.
- `POST /api/alertas`: crea una alerta y la publica en tiempo real por WebSocket
  (`/topic/alertas`).
- `PATCH /api/alertas/{id}/resolver`: marca una alerta como resuelta.

## URL base
`{{apiBaseUrl}}/api/alertas`

## Dependencias
No depende de otro modulo de esta carpeta para el listado. `POST /api/alertas` puede opcionalmente
referenciar un `inyectorId` existente (catalogo de inyectores, seed `datos_dummy_contrastiq.sql`).

## Autenticacion
Header `Authorization: Bearer {{accessToken}}`. `AlertaController` **no tiene `@RequierePermiso`
en ningun endpoint** (a diferencia de `11 - Extravasaciones`): el unico control es la
autenticacion generica (`anyRequest().authenticated()`). Cualquiera de los 5 roles puede listar,
crear y resolver alertas. Se documenta como hallazgo -- no existe un modulo `ALERTAS` en la
matriz Rol x Modulo x Permiso (a diferencia de `EXTRAVASACIONES`, que si se migro a ese modelo en
julio 2026); si el negocio espera restringir quien puede crear/resolver alertas por rol, falta
esa migracion equivalente.

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR -- los 5, sin diferenciacion, para los 3
endpoints.

## Endpoints

| Metodo | Path | Request en la coleccion |
|---|---|---|
| GET | `/api/alertas` | `GET Listar alertas - sin filtros` / `GET Listar alertas - todos los filtros` / `GET Listar alertas - severidad invalida (negativo)` / `GET Listar alertas - tipo invalido (negativo)` |
| POST | `/api/alertas` | `POST Crear alerta` / `POST Crear alerta - tipo faltante (negativo)` / `POST Crear alerta - severidad faltante (negativo)` / `POST Crear alerta - mensaje faltante (negativo)` / `POST Crear alerta - mensaje excede longitud maxima (negativo)` / `POST Crear alerta - severidad con valor no valido (negativo)` / `POST Crear alerta - inyectorId inexistente (negativo)` |
| PATCH | `/api/alertas/{id}/resolver` | `PATCH Resolver alerta` / `PATCH Resolver alerta - alerta inexistente (negativo)` |

### Filtros de `GET /api/alertas` (todos opcionales)
`resuelta` (boolean), `severidad` (`INFO|ADVERTENCIA|CRITICA`), `tipo`
(`EQUIPO_MANTENIMIENTO|STOCK_BAJO|FALLA_COMUNICACION|EDA_FUERA_DE_RANGO|OTRO`). Paginacion:
`page` (default `0`), `size` (default `20`), orden fijo `fechaHora DESC`.

### Cuerpo de `POST /api/alertas` (`CrearAlertaRequest`)
`tipo` (`@NotBlank`), `severidad` (`@NotBlank`), `inyectorId` (opcional, `Long`), `mensaje`
(`@NotBlank`, `@Size(max = 500)`).

## Variables necesarias
- `apiBaseUrl`, `accessToken`.
- `inyectorId`: ID real de un inyector del seed, usado en `POST Crear alerta` positivo.
- `alertaId`: se autocompleta via `pm.environment.set()` tanto en
  `GET Listar alertas - sin filtros` (toma el `id` de la primera fila) como en
  `POST Crear alerta` (toma el `id` de la alerta recien creada, que sobreescribe el valor
  anterior para que `PATCH Resolver alerta` opere sobre la alerta nueva).

## Datos de prueba
`POST Crear alerta` inserta una fila real en `alertas_sistema` cada vez que se corre. No hay
`DELETE` en este controller, asi que las alertas creadas por las pruebas quedan persistidas (ver
"Limpieza de datos").

## Orden recomendado
1. `GET Listar alertas - sin filtros`
2. `GET Listar alertas - todos los filtros`
3. `GET Listar alertas - severidad invalida (negativo)`
4. `GET Listar alertas - tipo invalido (negativo)`
5. `POST Crear alerta`
6. `POST Crear alerta - tipo faltante (negativo)`
7. `POST Crear alerta - severidad faltante (negativo)`
8. `POST Crear alerta - mensaje faltante (negativo)`
9. `POST Crear alerta - mensaje excede longitud maxima (negativo)`
10. `POST Crear alerta - severidad con valor no valido (negativo)`
11. `POST Crear alerta - inyectorId inexistente (negativo)`
12. `PATCH Resolver alerta`
13. `PATCH Resolver alerta - alerta inexistente (negativo)`

## Casos positivos
- Listado sin filtros y con todos los filtros combinados (`resuelta`, `severidad`, `tipo`).
- Creacion de una alerta valida de tipo `STOCK_BAJO`/severidad `ADVERTENCIA` referenciando un
  `inyectorId` real -> 201.
- Resolucion de la alerta recien creada -> 200 con `resuelta=true`.

## Casos negativos
- `severidad=NO_EXISTE` o `tipo=NO_EXISTE` en el listado -> `Severidad.valueOf()` /
  `TipoAlerta.valueOf()` lanzan `IllegalArgumentException` -> **400**.
- `POST` sin `tipo`, sin `severidad` o sin `mensaje` -> **400** por Bean Validation
  (`@NotBlank`), con el campo correspondiente en `errores`.
- `POST` con `mensaje` de mas de 500 caracteres -> **400** por `@Size(max = 500)`.
- `POST` con `severidad` no vacia pero fuera del enum (ej. `"URGENTISIMA"`) -> pasa la validacion
  `@NotBlank` (no esta en blanco) pero `Severidad.valueOf()` en `AlertaService.crear()` lanza
  `IllegalArgumentException` -> **400**. Se distingue del caso anterior porque el mecanismo es
  distinto (`MethodArgumentNotValidException` vs. `IllegalArgumentException`), aunque el status
  HTTP final sea el mismo.
- `POST` con `inyectorId` que no existe en la tabla `inyectores` -> `IllegalArgumentException("El
  inyector indicado no existe")` -> **400**.
- `PATCH .../resolver` sobre un `id` inexistente -> `IllegalArgumentException("La alerta no
  existe")` -> **400** (no 404, mismo patron que los otros 3 modulos de este grupo).

## Reglas de negocio
- `AlertaService.crear()` publica cada alerta nueva en el topic STOMP `/topic/alertas` via
  `SimpMessagingTemplate.convertAndSend()`, ademas de persistirla. Postman/Newman **no valida
  este push en tiempo real** (ver "Limitaciones").
- `severidad` tiene un valor por defecto `INFO` a nivel de entidad (`@Builder.Default`), pero
  como el DTO de entrada exige `@NotBlank`, en la practica siempre hay que enviarlo explicitamente
  desde la API (el default de la entidad solo aplicaria si se construyera `AlertaSistema` por otro
  camino que no pase por este DTO).
- `resolver()` no vuelve a permitir "reabrir" una alerta: no existe un endpoint inverso a
  `/resolver`. Volver a llamar `/resolver` sobre una alerta ya resuelta no falla (no hay chequeo
  de estado previo), simplemente vuelve a setear `fechaResolucion` a la hora actual -- igual que
  el caso de doble revision en `11 - Extravasaciones`, por eso no se agrega como negativo.
- Este controller **no aplica DEF-03** (restriccion de sede): no hay ningun filtro por sede en
  `AlertaService`, ni en el listado ni en la creacion/resolucion. Documentado como hallazgo, no
  como comportamiento verificado.

## Codigos HTTP esperados
| Escenario | Status |
|---|---|
| Listado exitoso | 200 |
| Creacion exitosa | 201 |
| Resolucion exitosa | 200 |
| `severidad`/`tipo` invalido en filtro o en body | 400 |
| Campo obligatorio faltante o fuera de longitud | 400 |
| `inyectorId` inexistente | 400 |
| `id` de alerta inexistente en `/resolver` | 400 |
| Sin token / token expirado | 401 |

## Ejemplos de request/response reales

`POST {{apiBaseUrl}}/api/alertas`
```json
{
  "tipo": "STOCK_BAJO",
  "severidad": "ADVERTENCIA",
  "inyectorId": 12,
  "mensaje": "Quedan menos de 3 jeringas precargadas disponibles en la sala 2"
}
```
Respuesta 201:
```json
{
  "id": 5017,
  "tipo": "STOCK_BAJO",
  "severidad": "ADVERTENCIA",
  "inyector": "IRIS-0012",
  "sala": "Sala TAC 1",
  "mensaje": "Quedan menos de 3 jeringas precargadas disponibles en la sala 2",
  "fechaHora": "2026-07-17T10:15:32",
  "resuelta": false
}
```

`PATCH {{apiBaseUrl}}/api/alertas/5017/resolver` -> 200:
```json
{
  "id": 5017,
  "tipo": "STOCK_BAJO",
  "severidad": "ADVERTENCIA",
  "inyector": "IRIS-0012",
  "sala": "Sala TAC 1",
  "mensaje": "Quedan menos de 3 jeringas precargadas disponibles en la sala 2",
  "fechaHora": "2026-07-17T10:15:32",
  "resuelta": true
}
```

## Como ejecutar en Postman/Newman
1. Cargar la coleccion y el environment con `apiBaseUrl`, `accessToken` y un `inyectorId` real.
2. Ejecutar `12 - Alertas` en orden (Postman Runner) o
   `newman run coleccion.json -e entorno.json --folder "12 - Alertas"`.
3. `POST Crear alerta` debe correr antes que `PATCH Resolver alerta` para que
   `{{alertaId}}` apunte a una alerta fresca creada por la propia corrida (evita depender de que
   el seed tenga alertas sin resolver).

## Pruebas de seguridad
- Confirmar 401 sin `Authorization`.
- Confirmar que los 5 roles pueden crear y resolver alertas (no hay 403 esperado en este
  modulo). Si el negocio decide restringir esto por rol en el futuro, actualizar este documento y
  agregar el modulo `ALERTAS` a la matriz Rol x Modulo x Permiso, siguiendo el mismo patron que
  `migration_modulo_extravasaciones.sql`.

## Pruebas de integracion
- Cruzar `AlertaDTO.inyector`/`sala` de una alerta creada con `inyectorId` real contra el
  catalogo de inyectores (modulo de administracion de inyectores) para confirmar que
  `numeroSerie`/`nombre de sala` coinciden.
- Confirmar que `GET Listar alertas - todos los filtros` con `resuelta=false` no incluye la
  alerta recien resuelta por `PATCH Resolver alerta` en una corrida posterior.

## Limpieza de datos
No hay `DELETE /api/alertas/{id}`. Las alertas creadas por `POST Crear alerta` (y las variantes
negativas que llegan a persistir, ninguna en este caso porque todas fallan validacion) quedan en
la base. Si se necesita un estado limpio entre corridas, borrar manualmente las filas de prueba
de `alertas_sistema` a nivel de base de datos (no hay endpoint de limpieza en la API).

## Resultados esperados
Los casos positivos devuelven 200/201 con la forma documentada; los negativos devuelven 400.
Ningun caso de este modulo debe devolver 403 ni 500.

## Problemas frecuentes
- **400 en vez de 404** al resolver una alerta inexistente: comportamiento real, no error de la
  coleccion.
- **Alertas de prueba acumulandose**: al no existir `DELETE`, correr la carpeta muchas veces deja
  muchas filas de prueba en `alertas_sistema`; considerar limpiarlas periodicamente vía SQL en el
  entorno de pruebas.
- **`severidad` invalida como 400 "silencioso"**: como no hay `@Pattern`/enum de Bean Validation
  sobre `severidad`/`tipo` en `CrearAlertaRequest` (solo `@NotBlank`), el error 400 real viene del
  `IllegalArgumentException` de `Severidad.valueOf()`/`TipoAlerta.valueOf()` dentro del servicio,
  no de la fase de validacion del DTO -- el cuerpo de la respuesta en ese caso **no** tiene la
  forma `{mensaje, errores: {campo: ...}}` de `MethodArgumentNotValidException`, sino la forma mas
  simple `{timestamp, mensaje}` de `manejarArgumentoInvalido()`.

## Limitaciones
- **WebSocket no probado por esta suite**: cada `POST /api/alertas` exitoso publica la alerta en
  el topic STOMP `/topic/alertas` (broker nativo configurado en `WebSocketConfig`, endpoint
  `/ws`) para que cualquier cliente conectado la reciba en tiempo real. Postman/Newman no tiene
  soporte nativo para conectarse a un broker STOMP/WebSocket y verificar mensajes recibidos, asi
  que este modulo **solo valida el efecto HTTP** (la alerta se creo y quedo en la base) y **no
  valida** que el push por WebSocket efectivamente haya llegado. Si se necesita cubrir esa parte,
  usar una herramienta aparte (ej. un cliente STOMP de prueba en Node/Python, o un test end-to-end
  con el frontend Angular real) fuera de esta coleccion de Postman.
- No hay endpoint para reabrir una alerta resuelta ni para eliminarla.

## Evidencias recomendadas
- Captura de la respuesta 201 de `POST Crear alerta` con el `id` generado.
- Captura de la respuesta 200 de `PATCH Resolver alerta` mostrando `resuelta=true`.
- Captura de los 400 obtenidos en los 6 casos negativos de `POST Crear alerta`, agrupados, como
  evidencia de cobertura de validacion.
- Nota explicita en el reporte de ejecucion de que el push WebSocket de `/topic/alertas` quedo
  fuera del alcance de esta corrida de Postman/Newman.
