# Modulo 08 - Lotes y Mermas

## Objetivo

Cubrir con pruebas de Postman/Newman los dos sub-modulos de "Trazabilidad e insumos" de ContrastIQ: el registro y trazabilidad de **lotes de agente de contraste** (`LoteController`, `/api/lotes`) y el calculo de **merma de insumos** (contraste + solucion salina) a partir del volumen programado vs. real por fase de inyeccion (`MermaController`, `/api/insumos/mermas`).

**Justificacion de la agrupacion**: la merma se calcula a partir de `inyeccion_fases`, es decir, de las fases de las inyecciones que efectivamente consumieron un lote (ver `MermaService`, que deliberadamente NO usa `inyecciones.volumen_residual_ml` para compartir la misma fuente de verdad en las 4 vistas de merma). Ambos modulos giran alrededor del mismo dato operativo (consumo real de insumo por inyeccion) y comparten la misma pantalla de "Insumos" en el frontend, por lo que se documentan y prueban juntos.

## Responsabilidades

- **Lotes**: alta de un lote de agente de contraste (numero de lote, agente, sede, caducidad, cantidad en ml) y, ante un recall del fabricante, listar de inmediato que pacientes/inyecciones usaron ese lote (`GET /api/lotes/{id}/trazabilidad`).
- **Mermas**: 4 vistas de solo lectura sobre el volumen programado vs. real (contraste y solucion salina): KPI agregado con tendencia, desglose por sede, desglose por insumo/marca, y detalle por inyeccion individual. Incluye ademas un 5to endpoint (`resumen-filtrado`) consumido por la tarjeta de merma del dashboard de "Inyecciones de contraste", con los mismos filtros que ese dashboard.

No incluye: reservar consumible ni consultar costos (confirmado ausente en el codigo real).

## URL base

- Lotes: `{{apiBaseUrl}}/api/lotes`
- Mermas: `{{apiBaseUrl}}/api/insumos/mermas`

## Dependencias

- Requiere al menos un `AgenteContraste` y una `Sede` existentes en BD (`agenteId`, `sedeId`).
- La trazabilidad de un lote requiere que existan inyecciones ya asociadas a ese lote via `InyeccionFase` (si no, la respuesta es un arreglo vacio, no un error).
- Las 4 vistas de merma (`resumen`, `por-sede`, `por-insumo`, `por-inyeccion`) dependen de que existan inyecciones con fases dentro del rango de fechas consultado.
- Correr despues de los folders de autenticacion (01) y, si se quiere trazabilidad con datos reales, despues de haber creado inyecciones (folder de Inyecciones).

## Autenticación

JWT Bearer en el header `Authorization: Bearer {{accessToken}}`. Sin token valido, todos los endpoints devuelven 401 (filtro `ResourceServerConfig`, `anyRequest().authenticated()`).

## Roles permitidos

**Hallazgo importante**: a diferencia de lo que se podria asumir, `LoteController` **no tiene ninguna anotacion `@RequierePermiso` ni `@PreAuthorize`** en ninguno de sus 3 endpoints (`buscar`, `crear`, `trazabilidad`). Cualquiera de los 5 roles autenticados (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR) puede listar y crear lotes y ver trazabilidad, siempre que tenga un JWT valido. La unica restriccion real es por **sede** (ver Reglas de negocio), no por rol/permiso de modulo.

En `MermaController` la situacion es mixta:

| Endpoint | Anotacion | Roles con `INSUMOS_MERMAS:VER` en el seed |
|---|---|---|
| `GET /resumen-filtrado` | Ninguna (deliberado, ver comentario en el codigo) | Cualquier rol autenticado |
| `GET /resumen` | `@RequierePermiso(modulo="INSUMOS_MERMAS", permiso="VER")` | ADMIN, TECNICO, BIOMEDICA, VISUALIZADOR |
| `GET /por-sede` | idem | idem |
| `GET /por-insumo` | idem | idem |
| `GET /por-inyeccion` | idem | idem |

**RADIOLOGO no tiene `INSUMOS_MERMAS:VER`** en la matriz de permisos sembrada (`migration_catalogo_modulos_permisos.sql`), por lo que recibe 403 en `resumen`, `por-sede`, `por-insumo` y `por-inyeccion`, pero SI puede usar `resumen-filtrado` (por eso ese endpoint esta deliberadamente sin `@RequierePermiso`: lo consume el dashboard de inyecciones, al que RADIOLOGO si tiene acceso).

## Endpoints

| Metodo | Path | Nombre en groupD.json | Body/Query clave |
|---|---|---|---|
| GET | `/api/lotes` | Listar lotes (paginado, sin filtros) | `page`, `size` |
| GET | `/api/lotes` | Listar lotes filtrados por sede y proximos a caducar | `sedeId`, `proximosACaducar`, `soloVigentes` |
| POST | `/api/lotes` | Crear lote de agente de contraste | `agenteId`, `sedeId`, `numeroLote`, `fechaCaducidad`, `cantidadMl` |
| GET | `/api/lotes/{id}/trazabilidad` | Trazabilidad de un lote (recall) | path `id` |
| GET | `/api/insumos/mermas/resumen-filtrado` | Resumen de merma filtrado (tarjeta del dashboard de inyecciones, sin permiso de modulo) | `fechaInicio`, `fechaFin`, `sedeId`, `salaId`, `agenteId`, `identificadorAnatomicoId`, `estado`, `soloConAlertaEda` |
| GET | `/api/insumos/mermas/resumen` | Resumen de merma (KPI agregado con tendencia) | `desde`, `hasta` (LocalDate) |
| GET | `/api/insumos/mermas/por-sede` | Merma por sede | `desde`, `hasta` |
| GET | `/api/insumos/mermas/por-insumo` | Merma por insumo (agente/marca) | `desde`, `hasta` |
| GET | `/api/insumos/mermas/por-inyeccion` | Merma por inyeccion (detalle paginado) | `desde`, `hasta`, `page`, `size` |

## Variables necesarias

- `apiBaseUrl` (ej. `http://localhost:8080`)
- `accessToken`, `radiologoToken`, `tecnicoToken` (tokens de sesiones ya iniciadas por rol)
- `sedeId`, `agenteId` (semillas conocidas de la BD)
- `loteId` (capturado por "Listar lotes"), `loteIdNuevo`, `loteNumeroCreado` (capturados por "Crear lote")
- `loteIdOtraSede` (debe fijarse manualmente a un lote de una sede distinta a la del usuario `tecnicoToken`, para la prueba de acceso cruzado)

## Datos de prueba

- `agenteId` / `sedeId` deben existir previamente (dato semilla, ver `datos_dummy_contrastiq.sql`).
- `numeroLote` se genera con `{{$timestamp}}` para evitar colisiones entre corridas, salvo en el caso negativo de duplicado, donde se reutiliza a proposito `loteNumeroCreado`.
- Los rangos `desde`/`hasta` de merma usan fechas dentro del entorno de pruebas (2026-07-01 a 2026-07-17) — ajustar si la base de pruebas no tiene inyecciones en ese rango, o las 4 vistas devolveran ceros (no error).

## Orden recomendado

1. Listar lotes (paginado, sin filtros) — captura `loteId`.
2. Listar lotes filtrados por sede y proximos a caducar.
3. Crear lote de agente de contraste — captura `loteIdNuevo`, `loteNumeroCreado`.
4. Negativos de creacion de lote (duplicado, fecha pasada, cantidad invalida, campo faltante, agente inexistente, sin token).
5. Trazabilidad de un lote (recall) — usa `loteId`.
6. Negativos de trazabilidad (lote inexistente, acceso cruzado de sede).
7. Merma: resumen-filtrado, resumen, por-sede, por-insumo, por-inyeccion (y sus negativos).

## Casos positivos

- Listar lotes con y sin filtros (sede, vigencia, proximidad a caducidad).
- Crear un lote valido y confirmar `tieneInyecciones=false` (un lote recien creado nunca tiene inyecciones).
- Consultar trazabilidad de un lote existente.
- Consultar las 4 vistas de merma y `resumen-filtrado` con rangos de fecha validos.

## Casos negativos

- **Lote duplicado**: mismo `numeroLote` + `agenteId` + `sedeId` → 400 "Ese lote ya esta registrado para este agente y sede" (constraint unico real en `LoteAgenteContraste` + chequeo explicito en `LoteService.crear`).
- **`fechaCaducidad` en el pasado** → 400, viola `@Future` en `CrearLoteRequest`.
- **`cantidadMl` <= 0** → 400, viola `@Positive`.
- **Falta `agenteId`** → 400, viola `@NotNull`.
- **`agenteId`/`sedeId` inexistentes** → 400, `IllegalArgumentException` desde el service ("El agente de contraste no existe" / "La sede no existe").
- **Lote inexistente en trazabilidad** → 400 ("El lote no existe").
- **Acceso cruzado de sede en trazabilidad**: un usuario restringido a una sede (`sedeIdRestriccion() != null`) pidiendo la trazabilidad de un lote de otra sede → 403 (`AccessDeniedException`, fix DEF-03).
- **Sin token** → 401.
- **RADIOLOGO** en `resumen`/`por-sede`/`por-insumo`/`por-inyeccion` de merma → 403 (no tiene `INSUMOS_MERMAS:VER`).
- **Rango de fechas invertido** (`desde` posterior a `hasta`) en cualquier endpoint de merma que reciba `desde`/`hasta` → 400 (`ValidadorRangoFechas`, fix DEF-04).
- **Falta parametro obligatorio** `desde`/`hasta` en `resumen`, `por-sede`, `por-insumo`, `por-inyeccion` → 400 (son `@RequestParam` sin `required=false`).

## Reglas de negocio

- Constraint unico real en BD: `(numero_lote, agente_id, sede_id)` en `lotes_agente_contraste` — reforzado tambien a nivel de aplicacion antes de tocar la BD.
- Un usuario restringido a una sede (`UsuarioAutenticadoService.sedeIdRestriccion()`) no puede pedir lotes ni trazabilidad de otra sede: en `buscar()` el `sedeId` del query param se ignora y se fuerza la sede del usuario; en `trazabilidad()` se valida explicitamente y se lanza 403 si no coincide.
- La merma se calcula siempre como `volumenProgramadoMl - volumenRealMl` sobre `inyeccion_fases`, nunca sobre `inyecciones.volumen_residual_ml`, para que las 4 vistas compartan la misma fuente de verdad.
- `resumen-filtrado` no calcula tendencia (`volumenMermaPeriodoAnteriorMl`/`variacionPorcentual` quedan `null`) porque con filtros arbitrarios de sala/agente/estado ya no hay un "periodo inmediato anterior" comparable de forma clara.
- `resumen` (sin filtros extra) SI calcula tendencia contra el periodo inmediato anterior de la misma duracion.

## Códigos HTTP esperados

| Escenario | Codigo |
|---|---|
| Listar/consultar OK | 200 |
| Crear lote OK | 201 |
| Validacion de campos (`@NotNull`, `@Future`, `@Positive`, `@NotBlank`) | 400 |
| Regla de negocio violada (duplicado, entidad referenciada inexistente, rango de fechas invertido) | 400 |
| Sin token / token invalido | 401 |
| Sin permiso de modulo (`INSUMOS_MERMAS:VER` para RADIOLOGO) o acceso cruzado de sede | 403 |

## Ejemplos de request/response reales

**POST `/api/lotes`** (request):
```json
{
  "agenteId": 1,
  "sedeId": 1,
  "numeroLote": "LOTE-1737120000",
  "fechaCaducidad": "2027-06-30",
  "cantidadMl": 500.00
}
```

**Response 201**:
```json
{
  "id": 42,
  "numeroLote": "LOTE-1737120000",
  "agente": "Omnipaque 350",
  "sede": "Hospital Central",
  "fechaCaducidad": "2027-06-30",
  "cantidadMl": 500.00,
  "recibidoEn": "2026-07-17T10:15:00",
  "activo": true,
  "vencido": false,
  "diasParaCaducar": 348,
  "tieneInyecciones": false
}
```

**GET `/api/insumos/mermas/resumen?desde=2026-07-01&hasta=2026-07-17`** (response):
```json
{
  "volumenProgramadoMl": 12500.00,
  "volumenRealMl": 11800.00,
  "volumenMermaMl": 700.00,
  "porcentajeMerma": 5.6,
  "volumenMermaPeriodoAnteriorMl": 850.00,
  "variacionPorcentual": -17.6
}
```

## Cómo ejecutar en Postman/Newman

1. Importar la coleccion completa (que incluye este fragmento `groupD.json` combinado con el resto de folders) y el environment con `apiBaseUrl`, `accessToken`, `radiologoToken`, `tecnicoToken`, `sedeId`, `agenteId`.
2. Ejecutar primero el folder de autenticacion para poblar los tokens.
3. Ejecutar "08 - Lotes y Mermas" completo, en orden (Postman respeta el orden del folder).
4. Con Newman: `newman run ContrastIQ.postman_collection.json -e ContrastIQ.postman_environment.json --folder "08 - Lotes y Mermas"`.

## Pruebas de seguridad

- Confirmar 401 en cualquier endpoint sin header `Authorization`.
- Confirmar 403 en `resumen`/`por-sede`/`por-insumo`/`por-inyeccion` de merma con `{{radiologoToken}}`.
- Confirmar 403 en trazabilidad de lote cuando el usuario (`{{tecnicoToken}}`, restringido a una sede) pide un lote de otra sede.
- Confirmar que `resumen-filtrado` SI responde 200 con `{{radiologoToken}}` (a diferencia de los otros 4 endpoints de merma) — es la prueba que documenta la excepcion deliberada del codigo.

## Pruebas de integración

- Crear un lote, luego crear una inyeccion que lo consuma (fuera del alcance de este folder, ver folder de Inyecciones), y confirmar que `tieneInyecciones=true` aparece en el listado y que la trazabilidad ya no esta vacia.
- Confirmar que la merma reportada en `por-inyeccion` para esa inyeccion coincide con `volumenProgramadoMl - volumenRealMl` de sus fases.

## Limpieza de datos

- Los lotes creados en pruebas (`LOTE-{{$timestamp}}`) quedan en la BD; no hay endpoint DELETE expuesto en `LoteController`. Si se requiere limpieza, debe hacerse directamente en BD o marcando `activo=false` manualmente (no hay endpoint para esto tampoco — es una limitacion del alcance actual del controller).

## Resultados esperados

Todas las pruebas positivas devuelven 200/201 con los campos documentados; las negativas devuelven 400/401/403 segun el escenario, con mensajes de error legibles en `mensaje` (y `errores` para fallos de `@Valid`).

## Problemas frecuentes

- Usar `desde`/`hasta` fuera del rango con datos reales en la BD de pruebas produce respuestas 200 "vacias" (ceros), no errores — no confundir con un fallo de la prueba.
- `resumen-filtrado` espera `fechaInicio`/`fechaFin` en formato `LocalDateTime` ISO (`yyyy-MM-ddTHH:mm:ss`), mientras que `resumen`/`por-sede`/`por-insumo`/`por-inyeccion` esperan `desde`/`hasta` en formato `LocalDate` (`yyyy-MM-dd`) — son formatos distintos, revisar antes de copiar parametros entre requests.
- Confundir el 403 de permiso de modulo (`RequierePermiso`/`PermisoAspect`) con el 403 de acceso cruzado de sede (`AccessDeniedException` lanzado directamente en `LoteService`) — ambos devuelven el mismo cuerpo de error generico, hay que revisar el escenario, no solo el codigo HTTP.

## Limitaciones

- `LoteController` no tiene control de acceso por rol/permiso de modulo, solo por sede — cualquier rol autenticado puede crear lotes.
- No existe endpoint para editar o desactivar un lote una vez creado.
- No existe endpoint de "reservar consumible" ni de "consultar costos" (confirmado ausente).
- La trazabilidad no pagina — si un lote tiene muchas inyecciones asociadas, la respuesta puede ser grande.

## Evidencias recomendadas

- Captura de la respuesta 201 al crear un lote.
- Captura del 400 por lote duplicado (evidencia de la regla de negocio real, no inventada).
- Captura del 403 en merma con `radiologoToken` vs. el 200 en `resumen-filtrado` con el mismo token — evidencia clave de la excepcion documentada en el codigo.
- Captura del archivo/consola de Newman con el resumen de la corrida del folder completo.
