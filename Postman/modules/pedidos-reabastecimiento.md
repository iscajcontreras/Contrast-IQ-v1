# Modulo 09 - Pedidos de Reabastecimiento

## Objetivo

Cubrir con pruebas de Postman/Newman el ciclo de vida de las ordenes de reabastecimiento de insumos (agente de contraste) expuesto por `PedidoReabastecimientoController` (`/api/pedidos-reabastecimiento`): listar los pedidos existentes y actualizar su estado a lo largo del flujo PENDIENTE → ENVIADO → RECIBIDO (o CANCELADO).

## Responsabilidades

- Listar todos los pedidos de reabastecimiento, ordenados por fecha de solicitud descendente.
- Actualizar el estado de un pedido (`PATCH`), lo que ademas fija automaticamente `fechaEnvio` (al pasar a ENVIADO) o `fechaRecepcion` (al pasar a RECIBIDO).

**Confirmado en el codigo**: el controller **no expone un endpoint de creacion manual** (`POST`). La unica forma de generar un pedido es el metodo `PedidoReabastecimientoService.generarAutomatico(...)`, invocado internamente por `AlertasAutomaticasScheduler` cuando el stock cae por debajo del minimo — no hay un `@PostMapping` en `PedidoReabastecimientoController`. Esto contradice la idea de "creado manualmente por farmacia" que sugiere el comentario del servicio: ese flujo, si existe, no esta expuesto via API REST en este controller. Se documenta como confirmado ausente, no como una omision de esta tarea.

## URL base

`{{apiBaseUrl}}/api/pedidos-reabastecimiento`

## Dependencias

- Requiere que existan pedidos previos en BD (generados por el scheduler de alertas o precargados en datos dummy) — sin al menos un pedido `PENDIENTE`, el caso positivo de "Actualizar pedido" no tiene sobre que actuar.
- No depende de otros folders de este grupo, pero logicamente esta relacionado con Lotes (ambos son parte de "Trazabilidad de insumos").

## Autenticación

JWT Bearer en `Authorization: Bearer {{accessToken}}`. Sin token, 401.

## Roles permitidos

**Hallazgo importante**: `PedidoReabastecimientoController` **no tiene ninguna anotacion `@RequierePermiso` ni `@PreAuthorize`** en `listar()` ni en `actualizar()`. Cualquiera de los 5 roles (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR) con un JWT valido puede listar y actualizar pedidos, incluyendo VISUALIZADOR (rol de solo lectura por diseño en el resto del sistema, pero aqui puede hacer PATCH porque no hay gate a nivel de controller). Esto es una diferencia real respecto a lo que el nombre del modulo (`INSUMOS_PEDIDOS`, sembrado en el catalogo para TECNICO/BIOMEDICA) sugeriria — el modulo existe en la matriz de permisos pero el controller no lo consulta.

## Endpoints

| Metodo | Path | Nombre en groupD.json | Body/Query clave |
|---|---|---|---|
| GET | `/api/pedidos-reabastecimiento` | Listar pedidos de reabastecimiento | — |
| PATCH | `/api/pedidos-reabastecimiento/{id}` | Actualizar pedido a ENVIADO | `estado` |
| PATCH | `/api/pedidos-reabastecimiento/{id}` | Actualizar pedido a RECIBIDO | `estado` |

## Variables necesarias

- `apiBaseUrl`, `accessToken`
- `pedidoId` (capturado automaticamente por "Listar pedidos de reabastecimiento": toma el primer pedido en estado `PENDIENTE`, o el primero de la lista si no hay ninguno pendiente)

## Datos de prueba

- No se requieren datos de entrada complejos: el unico campo del body es `estado`, un string que debe coincidir con un valor del enum `EstadoPedido` (`PENDIENTE`, `ENVIADO`, `RECIBIDO`, `CANCELADO`).

## Orden recomendado

1. Listar pedidos de reabastecimiento — captura `pedidoId`.
2. Actualizar pedido a ENVIADO.
3. Negativos (enum invalido, campo vacio, pedido inexistente, sin token).
4. Actualizar pedido a RECIBIDO.

## Casos positivos

- Listar pedidos y confirmar el orden descendente por `fechaSolicitud`.
- Actualizar un pedido a `ENVIADO` y confirmar que `fechaEnvio` queda seteada.
- Actualizar un pedido a `RECIBIDO` y confirmar que `fechaRecepcion` queda seteada.

## Casos negativos

- **`estado` con valor que no existe en `EstadoPedido`** (ej. `"NO_EXISTE"`) → 400, `EstadoPedido.valueOf(...)` lanza `IllegalArgumentException`, capturada por el manejador global.
- **`estado` vacio** → 400, viola `@NotBlank` en `ActualizarPedidoRequest`.
- **Pedido inexistente** (`id=999999`) → 400, `IllegalArgumentException` ("El pedido no existe").
- **Sin token** → 401.

## Reglas de negocio

- `estado == ENVIADO` → fija `fechaEnvio = LocalDateTime.now()` automaticamente (no se puede pasar por body).
- `estado == RECIBIDO` → fija `fechaRecepcion = LocalDateTime.now()` automaticamente.
- No hay validacion de transicion de estado (por ejemplo, no se impide pasar de `RECIBIDO` de vuelta a `PENDIENTE`, ni de `CANCELADO` a `ENVIADO`) — **confirmado ausente**: `actualizar()` simplemente sobreescribe `estado` sin revisar el valor anterior.
- Generacion automatica (`generarAutomatico`): cantidad solicitada = `2 * stockMinimo - stockActual`, o `stockMinimo` si ese calculo da <= 0. Esto no es probable via API (es interno al scheduler), se documenta solo como contexto de negocio.

## Códigos HTTP esperados

| Escenario | Codigo |
|---|---|
| Listar OK | 200 |
| Actualizar OK | 200 |
| `estado` invalido (enum o `@NotBlank`) | 400 |
| Pedido inexistente | 400 |
| Sin token | 401 |

## Ejemplos de request/response reales

**PATCH `/api/pedidos-reabastecimiento/7`** (request):
```json
{
  "estado": "ENVIADO"
}
```

**Response 200**:
```json
{
  "id": 7,
  "sede": "Hospital Central",
  "agente": "Omnipaque 350",
  "cantidadSolicitadaMl": 3000.00,
  "estado": "ENVIADO",
  "generadoAutomaticamente": true,
  "fechaSolicitud": "2026-07-10T08:00:00",
  "fechaEnvio": "2026-07-17T10:20:00",
  "fechaRecepcion": null,
  "notas": "Generado automaticamente: stock (450.00 ml) por debajo del minimo (500.00 ml)"
}
```

## Cómo ejecutar en Postman/Newman

1. Ejecutar el folder de autenticacion primero.
2. Ejecutar "09 - Pedidos de Reabastecimiento" en orden.
3. Newman: `newman run ContrastIQ.postman_collection.json -e ContrastIQ.postman_environment.json --folder "09 - Pedidos de Reabastecimiento"`.

## Pruebas de seguridad

- Confirmar 401 sin token.
- Confirmar que **cualquier rol** (incluido VISUALIZADOR) puede hacer `PATCH` — es el hallazgo de seguridad mas relevante de este modulo: no hay control de permiso a nivel de controller pese a existir el modulo `INSUMOS_PEDIDOS` en la matriz de roles.

## Pruebas de integración

- Confirmar que un pedido generado automaticamente por el scheduler de alertas (`generadoAutomaticamente=true`) aparece correctamente en el listado y puede actualizarse igual que uno cualquiera.

## Limpieza de datos

- No hay endpoint DELETE. Los pedidos actualizados a `ENVIADO`/`RECIBIDO` en pruebas quedan en ese estado permanentemente salvo que se revierta manualmente con otro PATCH (no hay validacion que lo impida, ver Reglas de negocio).

## Resultados esperados

200 en listar/actualizar validos, 400 en violaciones de validacion o entidad inexistente, 401 sin token.

## Problemas frecuentes

- Intentar correr "Actualizar pedido a RECIBIDO" sin haber corrido antes "Listar" en la misma sesion de Postman deja `pedidoId` sin valor.
- Suponer que existe un endpoint POST para crear pedidos manualmente — no existe en este controller.

## Limitaciones

- No hay POST para creacion manual de pedidos vía API — confirmado ausente en `PedidoReabastecimientoController`.
- No hay validacion de transiciones de estado (cualquier estado puede pasar a cualquier otro).
- No hay control de acceso por rol/permiso — cualquier usuario autenticado puede actualizar cualquier pedido.
- No hay endpoint para consultar un pedido individual por id (solo listado completo).

## Evidencias recomendadas

- Captura de la respuesta 200 con `fechaEnvio`/`fechaRecepcion` seteadas automaticamente.
- Captura del 400 por `estado` invalido, mostrando el mensaje de error real del backend.
- Captura de una llamada exitosa con `{{visualizadorToken}}` como evidencia del hallazgo de falta de control de acceso por rol.
