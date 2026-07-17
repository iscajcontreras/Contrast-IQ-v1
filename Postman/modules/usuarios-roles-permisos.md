# Modulo: Usuarios, Roles y Permisos (`/api/usuarios`, `/api/me/permisos`, `/api/administracion`)

## Objetivo
Cubrir con pruebas Postman/Newman la gestion administrativa de identidad y acceso de ContrastIQ: alta/baja/edicion de usuarios, consulta de los propios permisos, y la matriz Rol x Modulo x Permiso que controla que puede ver/hacer cada rol.

## Por que se agrupan estos 3 controllers
`UsuarioController`, `PermisosController` y `AdministracionController` giran todos alrededor de la misma pantalla conceptual de "Administracion de usuarios" en el frontend Angular: gestionar CUENTAS (`UsuarioController`), consultar los PERMISOS EFECTIVOS de la sesion actual para armar el menu/guards (`PermisosController`), y administrar la MATRIZ de permisos por rol que alimenta a `PermisosController` (`AdministracionController`). Los tres dependen del mismo modelo de datos (`Usuario`, `Rol`, `Modulo`, `Permiso`) y, salvo `PermisosController`, estan restringidos a ADMIN.

## Responsabilidades
- `UsuarioController` (`/api/usuarios`): CRUD (sin DELETE fisico, solo `PATCH /estado` para activar/desactivar) de cuentas de usuario. Delega en `UsuarioService`.
- `PermisosController` (`/api/me/permisos`): expone los permisos efectivos (`moduloCodigo:permisoCodigo`) del usuario autenticado, sin importar su rol. Alimenta `PermisosService`/`permisoGuard` en Angular.
- `AdministracionController` (`/api/administracion`): catalogos (`modulos`, `permisos`, `roles`) y la matriz de permisos por rol (`GET .../matriz`, `POST .../matriz/otorgar`, `DELETE .../matriz/revocar`).

## URL base
- `{{apiBaseUrl}}/api/usuarios`
- `{{apiBaseUrl}}/api/me/permisos`
- `{{apiBaseUrl}}/api/administracion`

## Dependencias
- Requiere que el modulo "Auth" haya corrido antes (necesita `{{adminToken}}`, `{{tecnicoToken}}`, `{{radiologoToken}}`, `{{biomedicaToken}}`, `{{visualizadorToken}}`).
- Dentro del propio modulo: "Listar Roles - Positivo" (Administracion) debe correr antes que "Crear Usuario"/"Actualizar Usuario" (Usuarios), porque estas ultimas necesitan `{{rolIdTecnico}}` (u otro `rolId*`) capturado de ahi. "Listar Modulos - Positivo" y "Listar Permisos - Positivo" deben correr antes de "Otorgar Permiso"/"Revocar Permiso".
- "Historial de Accesos" (modulo separado) depende de `{{usuarioId}}`, generado aqui por "Crear Usuario - Positivo".

## Autenticacion
Todos los endpoints de los 3 controllers exigen `Authorization: Bearer <token>` (ninguno esta en la whitelist publica de `ResourceServerConfig`).

## Roles permitidos

| Controller | Restriccion | Nivel de la anotacion |
|---|---|---|
| `UsuarioController` | Solo ADMIN, en TODOS los metodos (incluida lectura) | `@PreAuthorize("hasRole('ADMIN')")` a nivel de **clase** |
| `PermisosController` | Cualquier rol autenticado (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR) | **Sin** `@PreAuthorize` — solo exige autenticacion |
| `AdministracionController` | Solo ADMIN, en TODOS los metodos | `@PreAuthorize("hasRole('ADMIN')")` a nivel de **clase**; ademas `otorgar`/`revocar` llevan `@RequierePermiso(modulo="ADMINISTRACION", permiso="EDITAR")` como capa adicional |

## Endpoints

### Usuarios (`/api/usuarios`)

| Metodo | Path | Request DTO | Response DTO | Request Postman equivalente |
|---|---|---|---|---|
| GET | /api/usuarios | query: sedeId, rolId, activo, busqueda, page, size | `Page<UsuarioResumenDTO>` | "Listar Usuarios - Positivo (paginado, default)", "Listar Usuarios - Positivo - Filtros (rolId, activo, busqueda)" |
| GET | /api/usuarios/{id} | - | `UsuarioResumenDTO` | "Obtener Usuario por ID - Positivo" |
| POST | /api/usuarios | `CrearUsuarioRequest{nombreCompleto,email,password,rolId,sedeId?}` | `UsuarioResumenDTO` (201) | "Crear Usuario - Positivo (201, guarda usuarioId)" |
| PUT | /api/usuarios/{id} | `ActualizarUsuarioRequest{nombreCompleto,rolId,sedeId?}` | `UsuarioResumenDTO` | "Actualizar Usuario - Positivo (200)" |
| PATCH | /api/usuarios/{id}/estado | `CambiarEstadoUsuarioRequest{activo}` | `UsuarioResumenDTO` | "Cambiar Estado Usuario - Positivo - Desactivar (200)" |

### Permisos (me) (`/api/me/permisos`)

| Metodo | Path | Response DTO | Request Postman equivalente |
|---|---|---|---|
| GET | /api/me/permisos | `List<PermisoModuloDTO>{moduloCodigo,permisoCodigo}` | "Mis Permisos - Positivo (ADMIN)", "Mis Permisos - Positivo (TECNICO, lista distinta a ADMIN)" |

### Administracion (`/api/administracion`)

| Metodo | Path | Request DTO | Response DTO | Request Postman equivalente |
|---|---|---|---|---|
| GET | /api/administracion/modulos | - | `List<ModuloDTO>{id,codigo,nombre,descripcion}` | "Listar Modulos - Positivo" |
| GET | /api/administracion/permisos | - | `List<PermisoDTO>{id,codigo,nombre,descripcion}` | "Listar Permisos - Positivo" |
| GET | /api/administracion/roles | - | `List<RolDTO>{id,nombre,cantidadUsuarios}` | "Listar Roles - Positivo (guarda rolId de cada uno de los 5 roles)" |
| GET | /api/administracion/roles/{rolId}/matriz | - | `List<MatrizCeldaDTO>{moduloId,moduloCodigo,moduloNombre,permisoId,permisoCodigo,permisoNombre,otorgado}` | "Obtener Matriz de Rol - Positivo" |
| POST | /api/administracion/roles/{rolId}/matriz/otorgar | `OtorgarPermisoRequest{moduloId,permisoId}` | vacio (204) | "Otorgar Permiso - Positivo (204)" |
| DELETE | /api/administracion/roles/{rolId}/matriz/revocar | query: moduloId, permisoId | vacio (204) | "Revocar Permiso - Positivo (204)" |

## Variables necesarias
- `adminToken`, `tecnicoToken`, `radiologoToken`, `biomedicaToken`, `visualizadorToken` (del modulo Auth).
- `rolIdAdmin`, `rolIdTecnico`, `rolIdRadiologo`, `rolIdBiomedica`, `rolIdVisualizador`: los setea "Listar Roles - Positivo".
- `moduloIdEjemplo`, `moduloCodigoEjemplo`: los setea "Listar Modulos - Positivo".
- `permisoIdEjemplo`, `permisoCodigoEjemplo`: los setea "Listar Permisos - Positivo".
- `usuarioId`: lo setea "Crear Usuario - Positivo"; lo reutilizan "Obtener/Actualizar/Cambiar Estado Usuario" y el modulo "Historial de Accesos".
- `randomEmail`: reutilizada del modulo Auth para "Crear Usuario - Positivo".

## Datos de prueba
Mismos 5 usuarios semilla que el modulo Auth (ver ese documento). Los roles reales (enum `NombreRol`) son exactamente: ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR — no existen otros.

## Orden recomendado de ejecucion
1. (Modulo Auth ya corrido: `adminToken`, `tecnicoToken`, `radiologoToken`, `biomedicaToken`, `visualizadorToken` disponibles.)
2. Administracion: "Listar Modulos - Positivo", "Listar Permisos - Positivo", "Listar Roles - Positivo" (en cualquier orden entre si, pero todas antes que Usuarios/Otorgar/Revocar).
3. Usuarios: "Listar Usuarios - *" (no dependen de nada mas), "Crear Usuario - Positivo" (deja `usuarioId`), luego "Obtener/Actualizar/Cambiar Estado Usuario por ID" (dependen de `usuarioId`).
4. Administracion: "Obtener Matriz de Rol - Positivo", "Otorgar Permiso - Positivo", "Revocar Permiso - Positivo" (revierte el otorgamiento, dejando la matriz como estaba).
5. Permisos (me): en cualquier momento despues de tener tokens (no depende de nada del resto del modulo).
6. Negativos de cada sub-carpeta: pueden correrse en cualquier momento despues de tener los tokens de rol necesarios.

## Casos positivos
- Listado paginado y filtrado de usuarios, orden ascendente por `nombreCompleto`.
- Obtencion de un usuario por id.
- Alta de usuario (rol asignado explicito via `rolId`, no el VISUALIZADOR fijo de `/auth/registro`).
- Actualizacion de nombre/rol/sede.
- Cambio de estado activo/inactivo.
- Consulta de permisos propios para ADMIN y para TECNICO (listas distintas segun rol).
- Catalogos de modulos, permisos y roles con `cantidadUsuarios` por rol.
- Matriz Rol x Modulo x Permiso de un rol especifico.
- Otorgar y revocar un permiso puntual en la matriz.

## Casos negativos
- Todos los endpoints de `UsuarioController` y `AdministracionController`: sin token (401) y con token de rol distinto de ADMIN (403) — se prueba con distintos roles no-ADMIN en distintos endpoints (TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR) para evidenciar que la restriccion es por rol, no por endpoint especifico.
- Crear Usuario: email duplicado, password <8 caracteres, `rolId` faltante (`@NotNull`), email con formato invalido.
- Actualizar Usuario: `nombreCompleto` vacio (`@NotBlank`), `rolId` nulo (`@NotNull`).
- Cambiar Estado Usuario: `activo` nulo (`@NotNull`).
- Otorgar Permiso: `moduloId` faltante (`@NotNull`).
- Obtener Usuario / Obtener Matriz de Rol / Revocar Permiso con ids inexistentes (comportamiento exacto no confirmado en el analisis de codigo — ver Limitaciones).
- `PermisosController`: sin token (401). **No existe** caso negativo de "rol incorrecto" para este endpoint porque no filtra por rol.

## Reglas de negocio relevantes
- `UsuarioController` y `AdministracionController` restringen TODO su contenido a ADMIN a nivel de clase — no hay ninguna operacion de lectura "abierta" en estos dos controllers, a diferencia de `PermisosController`.
- `PermisosController` es deliberadamente el unico endpoint de este grupo accesible a cualquier rol: es la fuente de verdad que usa el propio frontend para decidir que mostrar en el menu de CADA usuario, sin importar su rol.
- `AdministracionController.otorgar()`/`revocar()` llevan una segunda capa de control (`@RequierePermiso`) ademas del `@PreAuthorize` de clase — documentado en el codigo como preparacion para un futuro donde la pantalla se abra en modo "solo lectura" a otro rol; hoy, con `hasRole('ADMIN')` a nivel de clase, un no-ADMIN nunca llega a evaluar la segunda capa.
- `CrearUsuarioRequest.sedeId` y `ActualizarUsuarioRequest.sedeId` son opcionales: un ADMIN o VISUALIZADOR puede no pertenecer a una sede especifica (comentario explicito en el DTO).
- El campo `online` de `UsuarioResumenDTO` no es presencia en tiempo real: significa "tiene al menos un refresh token vigente" (ver comentario en el DTO).
- No existe borrado fisico de usuarios expuesto por la API; la unica forma de "dar de baja" es `PATCH /estado` con `activo=false`.

## Codigos HTTP esperados

| Escenario | Status |
|---|---|
| Lecturas/escrituras exitosas en Usuarios/Administracion | 200 |
| Crear Usuario exitoso | 201 |
| Otorgar/Revocar permiso exitoso | 204 |
| Mis Permisos exitoso | 200 |
| Validacion de campos (`@NotBlank`, `@NotNull`, `@Email`, `@Size`) | 400 |
| Sin token en cualquier endpoint de este modulo | 401 |
| Token valido pero rol distinto de ADMIN, contra UsuarioController/AdministracionController | 403 |

## Ejemplos de request/response reales

**POST /api/usuarios** (request)
```json
{
    "nombreCompleto": "Usuario QA Postman",
    "email": "qa.1737000000000@contrastiq-demo.mx",
    "password": "PasswordSeguro123",
    "rolId": 2
}
```
Response 201:
```json
{
    "id": 57,
    "nombreCompleto": "Usuario QA Postman",
    "email": "qa.1737000000000@contrastiq-demo.mx",
    "rol": "TECNICO",
    "sede": null,
    "sedeId": null,
    "activo": true,
    "proveedor": "LOCAL",
    "online": false,
    "ultimoLogin": null
}
```

**GET /api/me/permisos** (response 200, ejemplo)
```json
[
    { "moduloCodigo": "ADMINISTRACION", "permisoCodigo": "VER" },
    { "moduloCodigo": "ADMINISTRACION", "permisoCodigo": "EDITAR" }
]
```

**GET /api/administracion/roles/2/matriz** (response 200, ejemplo)
```json
[
    {
        "moduloId": 3,
        "moduloCodigo": "PACIENTES",
        "moduloNombre": "Pacientes",
        "permisoId": 1,
        "permisoCodigo": "VER",
        "permisoNombre": "Ver",
        "otorgado": true
    }
]
```

**Error 403 (rol no ADMIN)**
```json
{
    "timestamp": "2026-07-17T10:20:00.456",
    "mensaje": "No tienes permiso para realizar esta accion"
}
```

## Como ejecutar en Postman/Newman
1. Correr primero la carpeta "01 - Auth" completa (o al menos los 5 logins por rol).
2. Correr "02 - Usuarios, Roles y Permisos" → sub-carpeta "Administracion (Roles y Permisos)" primero (para poblar `rolId*`, `moduloIdEjemplo`, `permisoIdEjemplo`), luego "Usuarios", luego "Permisos (me)".
3. Con Newman: `newman run coleccion.json -e environment.json --folder "02 - Usuarios, Roles y Permisos"` (Postman preserva subcarpetas; validar con `--folder` que ejecute en el orden esperado o dividir en 3 corridas por subcarpeta si es necesario).

## Pruebas de seguridad
- Confirmar que CADA metodo de `UsuarioController` y `AdministracionController` (no solo uno) rechaza tokens de rol no-ADMIN con 403 — se cubre con distintos roles distintos en distintos endpoints para evitar sesgo de "solo probamos un endpoint".
- Confirmar que `PermisosController` NO exige rol ADMIN (contraste deliberado con los otros dos controllers del modulo).
- Confirmar que la matriz de permisos (otorgar/revocar) requiere tanto rol como, en teoria, el permiso `ADMINISTRACION:EDITAR` (aunque hoy ADMIN los tiene todos por definicion del rol).

## Pruebas de integracion
- Crear Usuario → Obtener Usuario por ID → Actualizar Usuario → Cambiar Estado Usuario: ciclo completo sobre el mismo `usuarioId`.
- Listar Roles → Otorgar Permiso → Obtener Matriz de Rol (confirmar `otorgado:true` en la celda correspondiente) → Revocar Permiso → Obtener Matriz de Rol de nuevo (confirmar `otorgado:false`).
- Login con el usuario recien creado (rol TECNICO) → GET /api/me/permisos, para confirmar que los permisos efectivos coinciden con la matriz del rol TECNICO.

## Limpieza de datos
- El usuario creado en "Crear Usuario - Positivo" queda persistido; no hay DELETE fisico expuesto. Recomendado: dejarlo con `activo=false` al final de la corrida (ya cubierto por "Cambiar Estado Usuario - Positivo - Desactivar") o limpiar la tabla `usuarios` en BD de pruebas entre corridas de CI.
- "Revocar Permiso - Positivo" debe correr siempre despues de "Otorgar Permiso - Positivo" en la misma corrida para dejar la matriz del rol TECNICO en su estado original.

## Resultados esperados
Todos los tests en verde en una BD semilla intacta con el backend recien levantado, siguiendo el orden recomendado. Fallos esperables si se corre "Usuarios" antes que "Administracion" (por falta de `rolIdTecnico`), o si se corre "Historial de Accesos" antes que "Crear Usuario - Positivo" (por falta de `usuarioId`).

## Problemas frecuentes
- **400 "rolId" al crear/actualizar usuario:** casi siempre porque no se corrio antes "Listar Roles - Positivo" (variable `rolIdTecnico` vacia se serializa como `null` en el body).
- **403 en toda la sub-carpeta Usuarios:** revisar que el token usado sea `{{adminToken}}` y no otro rol.
- **La matriz no refleja el otorgamiento reciente:** confirmar que "Otorgar Permiso" respondio 204 antes de leer la matriz (orden de ejecucion).

## Limitaciones
- No se confirmo en este analisis el comportamiento exacto de `UsuarioService.obtener(id)`, `UsuarioService.crear()` (ante email duplicado) ni `PermisoService.obtenerMatrizDeRol()`/`revocar()` ante ids inexistentes — los tests correspondientes en la coleccion usan aserciones flexibles (`oneOf([...])`) y comentarios explicitos senalando la incertidumbre. Se recomienda revisar `UsuarioService.java` y `PermisoService.java` para afinar estos tests a un status unico.
- No se investigo el modelo `Sede` (referenciado por `sedeId`) en profundidad; los ejemplos de esta suite usan `sedeId: null` para simplificar.

## Evidencias recomendadas
- Captura de la matriz de un rol antes y despues de "Otorgar Permiso"/"Revocar Permiso".
- Captura de `GET /api/me/permisos` para ADMIN vs. VISUALIZADOR mostrando listas distintas.
- Log de Newman de la corrida completa del modulo como evidencia de regresion de autorizacion por rol.
