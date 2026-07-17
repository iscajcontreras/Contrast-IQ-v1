# Modulo: Historial de Accesos (`/api/historial-accesos`)

## Objetivo
Cubrir con pruebas Postman/Newman la consulta del historial de intentos de login (exitosos y fallidos) del sistema, usado por la pantalla "Historial de accesos" dentro de la gestion de usuarios.

## Responsabilidades
`HistorialAccesoController` (`src/main/java/com/contrastiq/backend/controller/HistorialAccesoController.java`) expone dos lecturas paginadas sobre la tabla `historial_accesos`, poblada automaticamente por `AuthService.login()` en cada intento (exitoso o fallido) via `HistorialAccesoService.registrar(...)`. Este controller es de solo lectura: no crea, edita ni borra historial directamente (el registro ocurre como efecto secundario del login).

## URL base
`{{apiBaseUrl}}/api/historial-accesos`

## Dependencias
- Requiere que el modulo "Auth" haya corrido antes (`{{adminToken}}`, y al menos un rol no-ADMIN para los negativos).
- Requiere `{{usuarioId}}`, generado por "Crear Usuario - Positivo" del modulo "Usuarios, Roles y Permisos", para el endpoint filtrado por usuario.
- Indirectamente depende de que se hayan ejecutado logins (exitosos o fallidos) previamente en la suite, para que haya contenido real que listar — de lo contrario los tests sobre `content` seguiran pasando (arrays vacios son validos) pero con menos valor de verificacion.

## Autenticacion
Ambos endpoints exigen `Authorization: Bearer <token>` (no estan en la whitelist publica).

## Roles permitidos
Solo ADMIN, en **ambos** metodos, via `@PreAuthorize("hasRole('ADMIN')")` puesto a **nivel de metodo** (no de clase, a diferencia de `UsuarioController`/`AdministracionController`).

### Inconsistencia detectada entre el comentario del codigo y el comportamiento real
El comentario de clase de `HistorialAccesoController` dice textualmente:
> "solo ADMIN puede ver el historial de CUALQUIER usuario; ver el propio historial no requiere ese rol (por eso el metodo /me esta disponible para cualquiera autenticado)."

Sin embargo, en el codigo actual **no existe ningun endpoint `/me`** en este controller: solo hay `GET /api/historial-accesos` (`listarTodos`) y `GET /api/historial-accesos/usuario/{usuarioId}` (`listarPorUsuario`), y **ambos** llevan `@PreAuthorize("hasRole('ADMIN')")`. Es decir, hoy ni siquiera un usuario no-ADMIN puede consultar su propio historial via esta API — el comentario describe una intencion de diseño (o funcionalidad removida) que no coincide con el codigo presente. Se documenta explicitamente y se prueba en la request "Listar Todos los Accesos - Negativo - Rol no ADMIN (RADIOLOGO, 403)", cuya descripcion referencia esta discrepancia.

## Endpoints

| Metodo | Path | Query params | Response DTO | Request Postman equivalente |
|---|---|---|---|---|
| GET | /api/historial-accesos | page (default 0), size (default 20) | `Page<HistorialAccesoDTO>` | "Listar Todos los Accesos - Positivo (paginado)" |
| GET | /api/historial-accesos/usuario/{usuarioId} | page, size | `Page<HistorialAccesoDTO>` | "Listar Accesos por Usuario - Positivo" |

`HistorialAccesoDTO`: `{id, emailUsado, exitoso, metodo, ipOrigen, userAgent, fechaHora}`.

## Variables necesarias
- `adminToken` (Auth).
- `radiologoToken`, `biomedicaToken` (Auth, para los negativos de rol).
- `usuarioId` (modulo "Usuarios, Roles y Permisos", request "Crear Usuario - Positivo").

## Datos de prueba
No requiere datos de prueba propios: consume el historial generado como efecto secundario de cualquier intento de login (exitoso o fallido) ejecutado en la carpeta "01 - Auth". Cuantos mas casos de login (positivos y negativos) se hayan corrido antes, mas rico el contenido a validar.

## Orden recomendado de ejecucion
1. Correr "01 - Auth" completo primero (genera filas reales en `historial_accesos`, incluyendo casos fallidos).
2. Correr "02 - Usuarios, Roles y Permisos" → "Usuarios" → "Crear Usuario - Positivo" (genera `{{usuarioId}}`).
3. Correr "15 - Historial de Accesos": "Listar Todos los Accesos - Positivo" primero, luego "Listar Accesos por Usuario - Positivo", luego los negativos y el caso edge de usuarioId inexistente (estos ultimos no dependen de orden entre si).

## Casos positivos
- Listado paginado de todo el historial (`page=0, size=20` por defecto), validando forma de `Page<HistorialAccesoDTO>` y de cada elemento.
- Listado paginado filtrado por `usuarioId` real (el creado en el modulo de Usuarios).

## Casos negativos
- Sin token: 401 en ambos endpoints.
- Rol distinto de ADMIN (probado con RADIOLOGO en `listarTodos` y BIOMEDICA en `listarPorUsuario`): 403 en ambos endpoints — confirmando la restriccion real (no la descrita en el comentario del codigo, ver seccion de Inconsistencia arriba).

## Caso "edge" (no es error, es comportamiento de Spring Data)
- `usuarioId` inexistente en `GET /api/historial-accesos/usuario/{usuarioId}`: responde **200 con una `Page` vacia** (`content: [], totalElements: 0`), NO un 404 ni un 500 — porque `listarPorUsuario` hace un query filtrado que simplemente no matchea ninguna fila cuando el `usuarioId` no existe. Es un contraste util frente a `UsuarioController.obtener(id)` (modulo Usuarios), donde un id inexistente probablemente si produce un error (comportamiento no confirmado en ese modulo, ver su documento).

## Reglas de negocio relevantes
- El historial se llena automaticamente en `AuthService.login()` para TODO intento, exitoso o fallido, antes de decidir si lanzar `BadCredentialsException` — por eso incluso los logins fallidos de la carpeta Auth dejan rastro aqui.
- `usuario` en la entidad `HistorialAcceso` es nulo si el email ni siquiera corresponde a una cuenta existente (comentario en el modelo) — es decir, `listarPorUsuario` solo puede devolver filas de intentos donde el email SI correspondia a una cuenta real en el momento del intento.
- `metodo` refleja `ProveedorAutenticacion` (hoy solo `LOCAL` tiene uso activo; `GOOGLE` esta reservado sin implementar, ver comentario en `Usuario.java`).
- Es un modulo puramente de lectura desde la API: no hay POST/PUT/DELETE expuestos.

## Codigos HTTP esperados

| Escenario | Status |
|---|---|
| Listar todos / listar por usuario, exitoso (incluye usuarioId inexistente → pagina vacia) | 200 |
| Sin token | 401 |
| Token valido, rol distinto de ADMIN | 403 |

## Ejemplos de request/response reales

**GET /api/historial-accesos?page=0&size=20** (response 200, ejemplo)
```json
{
    "content": [
        {
            "id": 134,
            "emailUsado": "teresa.hernandez@contrastiq-demo.mx",
            "exitoso": true,
            "metodo": "LOCAL",
            "ipOrigen": "127.0.0.1",
            "userAgent": "PostmanRuntime/7.36.0",
            "fechaHora": "2026-07-17T10:05:12.331"
        },
        {
            "id": 133,
            "emailUsado": "teresa.hernandez@contrastiq-demo.mx",
            "exitoso": false,
            "metodo": "LOCAL",
            "ipOrigen": "127.0.0.1",
            "userAgent": "PostmanRuntime/7.36.0",
            "fechaHora": "2026-07-17T10:04:58.902"
        }
    ],
    "totalElements": 134,
    "totalPages": 7,
    "number": 0,
    "size": 20
}
```

**GET /api/historial-accesos/usuario/999999999** (response 200, pagina vacia)
```json
{
    "content": [],
    "totalElements": 0,
    "totalPages": 0,
    "number": 0,
    "size": 20
}
```

**Error 403 (rol no ADMIN)**
```json
{
    "timestamp": "2026-07-17T10:22:00.789",
    "mensaje": "No tienes permiso para realizar esta accion"
}
```

## Como ejecutar en Postman/Newman
1. Correr "01 - Auth" (genera historial real) y "02 - Usuarios, Roles y Permisos" → "Usuarios" → "Crear Usuario - Positivo" (genera `usuarioId`) antes que este modulo.
2. Ejecutar la carpeta "15 - Historial de Accesos" completa.
3. Con Newman: `newman run coleccion.json -e environment.json --folder "15 - Historial de Accesos"` (asegurando que el environment ya tenga `adminToken`, `radiologoToken`, `biomedicaToken` y `usuarioId` de corridas previas dentro de la misma ejecucion, o encadenando las 3 carpetas en una sola corrida de Newman sin `--folder`).

## Pruebas de seguridad
- Confirmar que AMBOS endpoints (no solo uno) exigen rol ADMIN, dado que la anotacion esta duplicada a nivel de metodo (mas fragil ante un descuido futuro que ponerla a nivel de clase como en `UsuarioController`).
- Usar esta carpeta para regresionar especificamente la inconsistencia documentada: si en el futuro se agrega un endpoint `/me` real (como sugiere el comentario del codigo), esta suite deberia actualizarse para probarlo con reglas de autorizacion mas laxas (cualquier rol autenticado viendo SU PROPIO historial).

## Pruebas de integracion
- Login exitoso (Auth) → "Listar Todos los Accesos - Positivo" → confirmar que aparece una fila con `emailUsado` igual al usuario que acaba de loguearse y `exitoso: true`.
- Login fallido (password incorrecto, Auth) → "Listar Todos los Accesos - Positivo" → confirmar fila con `exitoso: false` para el mismo email.
- Crear Usuario (Usuarios) → login con ese usuario (si se agrega a la suite) → "Listar Accesos por Usuario - Positivo" con su `usuarioId` → confirmar que aparece su propio intento.

## Limpieza de datos
- No aplica limpieza propia: este modulo no crea datos, solo lee lo que otros modulos generan como efecto secundario (logins). La tabla `historial_accesos` crecera con cada corrida de la suite completa; en entornos de CI de larga duracion considerar una limpieza periodica de la tabla (fuera del alcance de esta API).

## Resultados esperados
Todos los tests en verde siempre que el modulo "01 - Auth" se haya corrido antes en la misma ejecucion (para tener contenido real) y "Usuarios" haya generado `usuarioId`. El caso "usuarioId inexistente" debe devolver SIEMPRE 200 con pagina vacia, nunca un error — si llegara a fallar con 4xx/5xx, es una regresion real a reportar.

## Problemas frecuentes
- **`content` vacio en "Listar Todos los Accesos - Positivo" en una corrida aislada:** normal si esta carpeta se corre sola, sin haber ejecutado antes ningun login de la carpeta Auth en la misma BD/corrida. No es un fallo del test (el test no asume `content.length > 0` de forma estricta), pero reduce el valor de la verificacion.
- **403 en "Listar Accesos por Usuario - Positivo":** confirmar que se esta usando `{{adminToken}}` y no otro rol.
- **`usuarioId` vacio (path `/usuario/`):** falta correr "Crear Usuario - Positivo" del modulo de Usuarios antes.

## Limitaciones
- No se confirmo el comportamiento con `page`/`size` fuera de rango (ej. `page` negativo o `size=0`) — no incluido en esta version de la suite; se recomienda agregarlo si se detectan bugs de paginacion en produccion.
- El comentario de clase del controller sugiere una funcionalidad ("ver el propio historial") que no existe en el codigo actual — ver seccion "Inconsistencia detectada" arriba. Cualquier prueba de "usuario ve su propio historial sin ser ADMIN" fallaria hoy con 403, lo cual es el comportamiento REAL a validar, no un bug de la suite.

## Evidencias recomendadas
- Captura de "Listar Todos los Accesos - Positivo" mostrando al menos un intento exitoso y uno fallido, correlacionados con las requests de la carpeta Auth.
- Captura del caso edge "usuarioId inexistente" mostrando 200 + pagina vacia (para dejar constancia de que es comportamiento esperado, no un bug).
- Log de Newman de la corrida completa como evidencia de que la restriccion ADMIN-only sigue vigente en ambos endpoints.
