# Modulo 06 - Inyecciones

## Objetivo
Validar `InyeccionController` (`/api/inyecciones`), el endpoint principal detras de la tabla
"Inyecciones recientes" del dashboard y de la pantalla de detalle de una inyeccion. Cubre
busqueda paginada con filtros, la grafica de presion, la actualizacion manual de dosis de
radiacion (CTDIvol/DLP) y el detalle completo (paciente + contraste + metadatos + graficas +
comparativo de fases Planeado/Programado/Real).

## Responsabilidades
- `GET /api/inyecciones`: listado paginado y filtrable (barra de filtros del dashboard).
- `GET /api/inyecciones/{id}/presion`: serie de tiempo de presion (PSI) para el boton
  "Ver presion".
- `PATCH /api/inyecciones/{id}/dosis-radiacion`: registro manual de CTDIvol/DLP del estudio de
  TAC asociado (dato que llega del equipo de TAC, no del inyector, por eso se actualiza aparte).
- `GET /api/inyecciones/{id}/detalle-completo`: pantalla de detalle completo de una inyeccion.

## URL base
`{{apiBaseUrl}}/api/inyecciones` (en el entorno local, `apiBaseUrl = http://localhost:8080/api`,
por eso las URLs completas de las requests se escriben como `{{apiBaseUrl}}/api/inyecciones...`
siguiendo la convencion usada en el resto de la coleccion).

## Dependencias
- Las inyecciones **NO se crean manualmente por API**. La unica via de creacion es la
  sincronizacion automatica desde CSV que corre otro modulo del backend (fuera del alcance de
  esta carpeta). Para poder ejecutar cualquier request de este modulo hace falta que ya existan
  filas en la tabla `inyecciones`.
- El seed `datos_dummy_contrastiq.sql` ya carga ~90,000 inyecciones distribuidas en 2 anios, con
  sus series de presion/flujo, fases y fases programadas. Si se corre ese script no hace falta
  ejecutar ninguna sincronizacion adicional antes de usar esta carpeta.
- Si el entorno se levanta desde cero sin ese seed, hay que correr primero el proceso de
  sincronizacion CSV documentado en el modulo correspondiente (fuera de este documento) para
  generar al menos una inyeccion antes de probar `06 - Inyecciones`.

## Autenticacion
Header `Authorization: Bearer {{accessToken}}` (o el token de rol especifico que aplique, ver
abajo). `InyeccionController` **no tiene `@RequierePermiso` en ningun endpoint**: el unico
control de acceso a nivel de controller es el generico de `ResourceServerConfig`
(`anyRequest().authenticated()`), es decir, cualquier usuario autenticado con cualquiera de los
5 roles puede leer y actualizar inyecciones. El control fino no es por modulo/permiso sino por
**sede** (ver "Reglas de negocio").

## Roles permitidos
Los 5 roles (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR) pueden llamar los 4 endpoints;
no hay diferenciacion de permisos por rol en este controller. Lo que si cambia por rol/usuario es
**a que sede queda restringido cada uno** (ver DEF-03 abajo).

## Endpoints

| Metodo | Path | Request en la coleccion |
|---|---|---|
| GET | `/api/inyecciones` | `GET Listar inyecciones - sin filtros` / `GET Listar inyecciones - todos los filtros` / `GET Listar inyecciones - paginacion personalizada` / `GET Listar inyecciones - estado invalido (negativo)` |
| GET | `/api/inyecciones/{id}/presion` | `GET Serie de presion de una inyeccion` / `GET Serie de presion - inyeccion inexistente (negativo)` |
| PATCH | `/api/inyecciones/{id}/dosis-radiacion` | `PATCH Actualizar dosis de radiacion` / `PATCH Actualizar dosis de radiacion - inyeccion inexistente (negativo)` |
| GET | `/api/inyecciones/{id}/detalle-completo` | `GET Detalle completo de una inyeccion` / `GET Detalle completo - inyeccion inexistente (negativo)` / `GET Detalle completo - inyeccion de otra sede (negativo - aislamiento DEF-03)` |

### Filtros de `GET /api/inyecciones` (todos opcionales, `FiltroInyeccionDTO`)
`fechaInicio`, `fechaFin` (ISO `LocalDateTime`, ej. `2026-07-01T00:00:00`), `sedeId`, `salaId`,
`inyectorId`, `protocoloId`, `identificadorAnatomicoId`, `agenteId`, `estado`
(`COMPLETADA|ABORTADA|ERROR`), `soloConAlertaEda` (boolean). Paginacion: `page` (default `0`),
`size` (default `20`), orden fijo por `fechaHoraInicio DESC` (no configurable por query param).

## Variables necesarias
- `apiBaseUrl`, `accessToken` (o el token del rol con el que se quiera probar).
- `tecnicoToken` / `radiologoToken` / `biomedicaToken` / `visualizadorToken` / `adminToken`: para
  los casos de aislamiento por sede.
- `inyeccionId`: se autocompleta con `pm.environment.set()` en `GET Listar inyecciones - sin
  filtros` (toma el `id` de la primera fila devuelta). Tambien puede fijarse a mano con un ID
  conocido del seed.
- `inyeccionIdOtraSede`: variable manual, no se autocompleta. Debe apuntar a una inyeccion cuya
  sala/sede sea **distinta** a la del usuario dueno de `tecnicoToken`, para poder ejercitar el
  caso negativo de aislamiento por sede.
- `sedeId`, `salaId`, `inyectorId`, `protocoloId`, `identificadorAnatomicoId`, `agenteId`: IDs de
  catalogo usados en el caso "todos los filtros" (llenar con datos reales del seed o de los
  modulos de catalogo).

## Datos de prueba
No aplica creacion propia: se depende 100% del seed `datos_dummy_contrastiq.sql` (o de haber
corrido la sincronizacion CSV al menos una vez). No se requiere limpieza de datos de este modulo
porque no se insertan filas de `inyecciones` via Postman (solo se actualiza `ctdi_vol_mgy` /
`dlp_mgy_cm` de filas existentes).

## Orden recomendado
1. `GET Listar inyecciones - sin filtros` (agarra un `inyeccionId` valido).
2. `GET Listar inyecciones - todos los filtros`
3. `GET Listar inyecciones - paginacion personalizada`
4. `GET Listar inyecciones - estado invalido (negativo)`
5. `GET Serie de presion de una inyeccion`
6. `GET Serie de presion - inyeccion inexistente (negativo)`
7. `PATCH Actualizar dosis de radiacion`
8. `PATCH Actualizar dosis de radiacion - inyeccion inexistente (negativo)`
9. `GET Detalle completo de una inyeccion`
10. `GET Detalle completo - inyeccion inexistente (negativo)`
11. `GET Detalle completo - inyeccion de otra sede (negativo - aislamiento DEF-03)`

## Casos positivos
- Listado sin filtros, con todos los filtros combinados y con paginacion custom (`page=0&size=5`).
- Serie de presion de una inyeccion real (`tieneSeriePresion=true` en el resumen).
- Actualizacion de `ctdiVolMgy`/`dlpMgyCm` sobre una inyeccion existente.
- Detalle completo de una inyeccion real (valida las 6 secciones del DTO).

## Casos negativos
- `estado=NO_EXISTE` en el listado -> `EstadoInyeccion.valueOf()` lanza `IllegalArgumentException`
  -> **400** (via `ManejadorGlobalExcepciones`).
- Serie de presion de un `id` inexistente -> **NO hay validacion de existencia** en
  `InyeccionService.obtenerSeriePresion()`: la respuesta real es **200 con arreglo vacio**, no un
  404. Se documenta el comportamiento tal cual esta implementado.
- `PATCH dosis-radiacion` y `GET detalle-completo` sobre un `id` inexistente -> ambos lanzan
  `IllegalArgumentException("La inyeccion no existe")` en el servicio, que
  `ManejadorGlobalExcepciones` mapea a **400 Bad Request**, no a 404 (este backend no distingue
  "recurso no encontrado" de "solicitud invalida": no existe ningun `@ExceptionHandler` de
  `NotFoundException` ni uso de `ResponseStatusException(HttpStatus.NOT_FOUND)`).
- `GET detalle-completo` de una inyeccion de otra sede con un token no-ADMIN -> **403**
  (`AccessDeniedException` desde `verificarAccesoASede()`).

> No se prueba "dosis de radiacion con valor negativo" como caso negativo: `ActualizarDosisRadiacionRequest`
> no tiene ninguna anotacion de Bean Validation (`@Positive`, `@Min`, etc.) sobre `ctdiVolMgy` ni
> `dlpMgyCm`, asi que un valor negativo se acepta tal cual (200) y se persiste sin validar. Esto
> se documenta como hallazgo en la seccion de Problemas frecuentes, no se inventa un negativo que
> el codigo no soporta.

## Reglas de negocio
- **DEF-03 (aislamiento por sede)**: para cualquier usuario no-ADMIN,
  `UsuarioAutenticadoService.sedeIdRestriccion()` devuelve el `sede_id` del usuario autenticado.
  - En `GET /api/inyecciones`, ese valor **sobreescribe** cualquier `sedeId` que mande el query
    param (el frontend no puede "pedir prestada" la sede de otro usuario).
  - En `PATCH dosis-radiacion` y `GET detalle-completo` (acceso por ID directo), se compara la
    sede de la inyeccion contra la del usuario; si no coincide, `AccessDeniedException` -> 403.
  - Un ADMIN, o un usuario sin `sede_id` asignado (columna nullable, interpretado como usuario
    corporativo), no tiene restriccion de sede.
- El comparativo de fases (`comparativoFases`) une por `numeroFase`/`ordenFase` tres fuentes
  independientes (`protocolo_fases`, `inyeccion_fases_programadas`, `inyeccion_fases|`); una fase
  que solo exista en una de las tres tablas aparece igual, con los otros dos lados en `null`.
- `agentePrincipal` en el resumen se calcula tomando la fase marcada `esFaseContraste=true`; si
  la inyeccion no tiene ninguna fase de contraste, el valor es el literal `"—"`.

## Codigos HTTP esperados
| Escenario | Status |
|---|---|
| Listado / detalle / presion exitosos | 200 |
| Actualizar dosis exitosa | 200 |
| Filtro `estado` invalido | 400 |
| `id` inexistente (dosis-radiacion, detalle-completo) | 400 |
| `id` inexistente (serie de presion) | 200 (arreglo vacio) |
| Acceso a inyeccion de otra sede (no-ADMIN) | 403 |
| Sin token / token expirado | 401 |

## Ejemplos de request/response reales

`GET {{apiBaseUrl}}/api/inyecciones?estado=COMPLETADA&page=0&size=20`
```json
{
  "content": [
    {
      "id": 88231,
      "fechaHoraInicio": "2026-07-15T09:24:00",
      "sala": "Sala TAC 2",
      "inyector": "IRIS-0245",
      "protocolo": "Torax contrastado estandar",
      "identificadorAnatomico": "Torax",
      "agentePrincipal": "Omnipaque 350",
      "volumenCargadoMl": 115.0,
      "volumenTotalMl": 90.0,
      "volumenResidualMl": 25.0,
      "presionMaximaPsi": 210.5,
      "presionPromedioPsi": 180.2,
      "presionLimitePsi": 325.0,
      "edaHabilitado": true,
      "ctdiVolMgy": null,
      "dlpMgyCm": null,
      "estado": "COMPLETADA",
      "tieneAlertaEda": false,
      "tieneSeriePresion": true
    }
  ],
  "totalElements": 90000,
  "totalPages": 4500,
  "number": 0,
  "size": 20
}
```

`PATCH {{apiBaseUrl}}/api/inyecciones/88231/dosis-radiacion`
```json
{ "ctdiVolMgy": 12.5, "dlpMgyCm": 450.75 }
```
Respuesta 200: `InyeccionResumenDTO` con `ctdiVolMgy: 12.5, dlpMgyCm: 450.75`.

## Como ejecutar en Postman/Newman
1. Importar la coleccion completa (que incluye este fragmento) y el environment con
   `apiBaseUrl`, `accessToken`, y los tokens de rol.
2. Correr primero `01 - Auth` (u otra carpeta que obtenga los tokens) para poblar
   `{{accessToken}}` / `{{tecnicoToken}}` / etc.
3. Ejecutar `06 - Inyecciones` en orden (Postman Runner respeta el orden de la carpeta).
4. Con Newman: `newman run coleccion.json -e entorno.json --folder "06 - Inyecciones"`.

## Pruebas de seguridad
- Confirmar que sin header `Authorization` cualquier endpoint responde 401.
- Confirmar DEF-03: con `{{tecnicoToken}}`, `GET /api/inyecciones?sedeId=<otra_sede>` debe
  devolver solo resultados de la sede real del tecnico (el filtro del query param se ignora).
- Confirmar `GET Detalle completo - inyeccion de otra sede (negativo - aislamiento DEF-03)` ->
  403 con un token no-ADMIN y una inyeccion de otra sede.

## Pruebas de integracion
- Encadenar `GET Listar inyecciones` -> tomar `id` -> `GET presion` -> `GET detalle-completo` ->
  `PATCH dosis-radiacion` -> volver a pedir `GET detalle-completo` y confirmar que
  `metadatos.ctdiVolMgy`/`dlpMgyCm` reflejan el cambio.

## Limpieza de datos
No aplica borrado: este modulo solo lee y actualiza dos columnas (`ctdi_vol_mgy`, `dlp_mgy_cm`)
de filas ya existentes del seed. Si se quiere dejar el dato como estaba, volver a hacer
`PATCH dosis-radiacion` con los valores originales (o `null`).

## Resultados esperados
Todas las requests marcadas como positivas devuelven 200 con la forma de DTO documentada; las
negativas devuelven el status indicado arriba (la mayoria 400, una 403, una sigue siendo 200 con
arreglo vacio). Ningun test de esta carpeta debe fallar por timeout ni por 500 no esperado.

## Problemas frecuentes
- **`inyeccionId` vacio**: si nunca se corrio `GET Listar inyecciones - sin filtros` (o el seed
  esta vacio), las requests que dependen de `{{inyeccionId}}` fallan. Correr primero esa request
  o fijar la variable a mano.
- **`inyeccionIdOtraSede` sin configurar**: el caso DEF-03 requiere setear manualmente esta
  variable con un ID real de otra sede; si queda vacio, la URL queda mal formada.
- **400 en vez del 404 esperado**: es comportamiento real del backend (no hay manejo especifico
  de "no encontrado"), no un error de la coleccion.
- **`ActualizarDosisRadiacionRequest` sin validacion**: enviar valores negativos o absurdamente
  grandes de `ctdiVolMgy`/`dlpMgyCm` no falla; se persisten tal cual. Si se agrega validacion en
  el backend a futuro, actualizar este modulo con el caso negativo correspondiente.

## Limitaciones
- No hay forma de probar la creacion de inyecciones via Postman: es un proceso de sincronizacion
  CSV automatico, fuera del alcance de esta suite (otro modulo lo cubre).
- El orden de resultados de `GET /api/inyecciones` es fijo (`fechaHoraInicio DESC`); no se puede
  probar ordenamiento custom porque el controller no expone ese parametro.

## Evidencias recomendadas
- Captura de la corrida de Newman de `06 - Inyecciones` con el resumen de tests (positivos y
  negativos) en verde.
- Captura del cuerpo de respuesta de `GET Detalle completo de una inyeccion` mostrando las 6
  secciones pobladas.
- Captura del 403 obtenido en el caso de aislamiento por sede (DEF-03), como evidencia de que el
  fix sigue vigente.
