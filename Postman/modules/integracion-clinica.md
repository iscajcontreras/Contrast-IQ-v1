# Modulo 13 - Integracion Clinica

## Objetivo
Validar los endpoints de integracion con dos sistemas externos simulados/desacoplados: el HIS del hospital (consulta y sincronizacion de datos demograficos/clinicos de un paciente por su identificador externo/MRN) y el inyector fisico EmpowerCTA (importacion periodica de inyecciones exportadas por la IRiS Workstation en formato CSV).

## Responsabilidades
- Exponer una vista previa de datos de un paciente tal como los devolveria el HIS, sin persistir nada (`GET /his/{id}`).
- Sincronizar (crear o actualizar) el `Paciente` local con los datos del HIS (`POST /his/{id}/sincronizar`).
- Disparar manualmente la importacion de archivos CSV exportados por el inyector, que es el UNICO mecanismo que crea filas en la tabla `inyecciones` (`POST /sincronizar-inyector`).
- Exponer el historial paginado de lotes de sincronizacion, tanto manuales como del job programado.

Fuente real: `controller/IntegracionClinicaController.java`, `service/IntegracionClinicaService.java`, `service/HisIntegracionServiceSimulado.java`, `service/HisIntegracionService.java` (interfaz), `service/SincronizacionInyectorService.java`, DTOs `dto/DatosPacienteHisDTO.java`, `dto/LoteSincronizacionDTO.java`.

## URL base
`{{apiBaseUrl}}/api/integracion-clinica`

## Dependencias
- `GET /his/{id}` y `POST /his/{id}/sincronizar` dependen de que exista (o no) un `Paciente` en la BD local con ese `identificadorExterno` — el HIS "simulado" NO tiene una fuente de datos independiente, consulta la propia tabla `pacientes`.
- `POST /sincronizar-inyector` depende de que existan archivos `.csv` en la carpeta configurada por la propiedad `app.sincronizacion.carpeta` (default `./importaciones-iris`, relativa al working directory del proceso Spring Boot que corre en el SERVIDOR) y de que existan previamente en BD los `inyectores` (por `numeroSerie`), `protocolos` y `agentes de contraste` referenciados en cada fila del CSV (si no existen, la fila para el inyector lanza excepcion y cuenta como fallo; protocolo/agente ausentes simplemente quedan `null` en la inyeccion creada).

## Endpoints
| Metodo | Path | Request name en la coleccion |
|---|---|---|
| GET | `/api/integracion-clinica/his/{identificadorExterno}` | "Buscar paciente en HIS - existente (200, simulado=true)", "Buscar paciente en HIS - inexistente (404)" |
| POST | `/api/integracion-clinica/his/{identificadorExterno}/sincronizar` | "Sincronizar paciente desde HIS - existente (200, crea/actualiza local)", "Sincronizar paciente desde HIS - inexistente (404)" |
| POST | `/api/integracion-clinica/sincronizar-inyector` | "Sincronizar inyector - disparo manual (200)", "Sincronizar inyector sin JWT (401)" |
| GET | `/api/integracion-clinica/historial-sincronizacion?page&size` | "Historial de sincronizacion (200, paginado)", "Historial de sincronizacion sin JWT (401)" |

## Autenticacion
JWT Bearer obligatorio en todos los endpoints. Sin `@PreAuthorize`/`@RequierePermiso` en `IntegracionClinicaController`: cualquiera de los 5 roles autenticados puede disparar una sincronizacion, incluida la del inyector, lo cual crea datos clinicos (inyecciones). Esto es relevante para pruebas de seguridad: no hay una restriccion tipo "solo BIOMEDICA puede sincronizar el inyector" en el codigo actual.

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR — los 5, sin diferenciacion a nivel de anotacion.

## Variables necesarias
- `apiBaseUrl`, `accessToken`
- `identificadorExternoPacienteExistente` — MRN de un paciente que YA existe en la BD local (el HIS simulado solo "encuentra" lo que ya esta local)
- `loteSincronizacionId` — se autocompleta tras "Sincronizar inyector - disparo manual (200)"

## Datos de prueba
- Un paciente existente con `identificadorExterno` conocido, para los casos positivos de `/his/{id}` y `/his/{id}/sincronizar`.
- Para probar `POST /sincronizar-inyector` con importacion real de datos (no solo el disparo vacio), es necesario **colocar manualmente** un archivo `.csv` en la carpeta configurada en el servidor (`app.sincronizacion.carpeta`), con este formato exacto (una fila = una inyeccion, separador coma, sin encabezado en la primera columna de datos — la linea 0 se descarta como encabezado):

```
numero_serie_inyector,mrn_paciente,fecha_hora_iso,protocolo,agente,volumen_ml,presion_max_psi,estado
SN-EMP-00231,MRN-000042,2026-07-17T08:30:00,Torax con contraste,Omnipaque 350,90.5,120.0,COMPLETADA
```
`estado` debe ser uno de: `COMPLETADA`, `ABORTADA`, `ERROR` (enum `EstadoInyeccion`). `fecha_hora_iso` debe ser parseable por `DateTimeFormatter.ISO_LOCAL_DATE_TIME` (ej. `2026-07-17T08:30:00`). El `numero_serie_inyector` debe coincidir (case-insensitive) con un inyector ya existente en BD, o la fila falla.

Este archivo CSV **no se sube via Postman**: la request de la coleccion no tiene body ni adjunto de archivo, porque el endpoint real no acepta ninguno (ver Limitaciones). Debe copiarse directamente en el filesystem del servidor antes de llamar al endpoint.

## Orden recomendado
1. "Buscar paciente en HIS - existente (200, simulado=true)".
2. "Buscar paciente en HIS - inexistente (404)".
3. "Sincronizar paciente desde HIS - existente (200, crea/actualiza local)".
4. "Sincronizar paciente desde HIS - inexistente (404)".
5. (Opcional, manual) Copiar un CSV de ejemplo en la carpeta del servidor.
6. "Sincronizar inyector - disparo manual (200)" — captura `loteSincronizacionId`.
7. "Sincronizar inyector sin JWT (401)".
8. "Historial de sincronizacion (200, paginado)".
9. "Historial de sincronizacion sin JWT (401)".

## Casos positivos
- `GET /his/{id}` de un paciente existente → 200, `simulado: true`, `fuente` indica explicitamente el modo simulado.
- `POST /his/{id}/sincronizar` de un paciente existente → 200, hace upsert del `Paciente` local y setea `sincronizadoHisEn`.
- `POST /sincronizar-inyector` sin archivos nuevos en la carpeta → 200 igualmente, con `registrosImportados: 0` y `estado: "EXITOSO"` (no es un error que no haya nada que importar).
- `POST /sincronizar-inyector` con un CSV valido colocado de antemano en el servidor → 200, `registrosImportados` > 0, y las inyecciones importadas quedan visibles luego en el modulo 03 (`/api/pacientes/{id}/inyecciones`).
- `GET /historial-sincronizacion` → 200, `Page<LoteSincronizacionDTO>` con los lotes generados (manuales y programados).

## Casos negativos
- `GET /his/{id}` con un `identificadorExterno` que no existe en absoluto en la BD local → **404** explicito (`ResponseEntity.notFound().build()` en el controller — a diferencia de Pacientes/Checklist, aqui SI es un 404 real, no un 400 de `IllegalArgumentException`).
- `POST /his/{id}/sincronizar` con `identificadorExterno` inexistente → **404**, por el mismo motivo: como el HIS simulado solo "trae" pacientes que ya existen localmente, no puede sincronizar (ni crear) uno que nunca existio.
- Cualquier endpoint sin `Authorization` header → 401.

## Reglas de negocio
- **El HIS es simulado.** `HisIntegracionServiceSimulado` NO llama a ningun sistema externo real (no hay HL7 ni FHIR implementado): consulta `pacienteRepository.findByIdentificadorExterno()` sobre la propia BD y marca `simulado: true` (controlado por la propiedad `app.his.habilitado`, `false` por defecto). Si en el futuro se activa `app.his.habilitado=true`, se espera una implementacion distinta (`HisIntegracionServiceFhir` u otra) que aun no existe en el codigo.
- **Las inyecciones NO se crean manualmente via API.** No existe ningun `POST /api/inyecciones` en todo el backend. La UNICA via para que aparezca una fila en `inyecciones` es `POST /api/integracion-clinica/sincronizar-inyector`, que procesa archivos CSV exportados por la IRiS Workstation (o el job `@Scheduled` cada 15 minutos si `app.sincronizacion.habilitada=true`, deshabilitado por defecto). Cualquier prueba que necesite datos de inyecciones debe pasar por este mecanismo (o por un seed directo de BD fuera de la API).
- `sincronizarAhora()` tambien puede crear pacientes nuevos como efecto colateral: si el `mrn_paciente` de una fila del CSV no existe, se crea un `Paciente` minimo (`Paciente.builder().identificadorExterno(mrn).build()`, sin nombre ni demograficos) — esta es la unica via, ademas del HIS, por la que puede aparecer un paciente nuevo en el sistema.
- El estado del lote (`EXITOSO`/`PARCIAL`/`FALLIDO`) se calcula segun archivos procesados vs. fallidos, no segun filas individuales dentro de un archivo (una fila malformada dentro de un CSV que si se pudo leer no marca el lote como `PARCIAL`, solo se salta esa fila si `campos.length < 8`, o hace fallar el archivo completo si lanza excepcion en medio del parseo).

## Codigos HTTP esperados
| Situacion | Codigo |
|---|---|
| Consulta/sincronizacion HIS de paciente existente | 200 |
| Consulta/sincronizacion HIS de paciente inexistente | 404 |
| Disparo de sincronizacion de inyector (con o sin archivos) | 200 |
| Historial de sincronizacion | 200 |
| Falta o JWT invalido | 401 |
| Fallo de IO al crear/leer la carpeta de importacion | 200 con `estado: "FALLIDO"` en el `LoteSincronizacionDTO` (no es un error HTTP — el endpoint siempre responde 200, el fallo se refleja en el campo `estado` del lote) |

## Ejemplos de request/response reales
`GET /api/integracion-clinica/his/MRN-000042`
```json
{
  "identificadorExterno": "MRN-000042",
  "nombreCompleto": "Maria Fernanda Lopez",
  "sexo": "F",
  "pesoKg": 68.50,
  "alergias": "Penicilina",
  "simulado": true,
  "fuente": "SIMULADO (paciente ya existente en el sistema local)"
}
```
`POST /api/integracion-clinica/sincronizar-inyector` (sin archivos nuevos)
```json
{
  "id": 118,
  "fuente": "IRIS_WORKSTATION",
  "fechaHora": "2026-07-17T10:20:03.556",
  "registrosImportados": 0,
  "estado": "EXITOSO",
  "usuario": "Sistema (automatico)",
  "detalle": "No se encontraron archivos nuevos para importar en ./importaciones-iris"
}
```

## Como ejecutar en Postman/Newman
```bash
newman run ContrastIQ.postman_collection.json \
  -e ContrastIQ.local.postman_environment.json \
  --folder "13 - Integracion Clinica"
```
Para probar la importacion real de inyecciones, coordinar con quien opere el servidor backend para colocar el CSV de prueba en `app.sincronizacion.carpeta` ANTES de correr el request "Sincronizar inyector - disparo manual (200)".

## Pruebas de seguridad
- Confirmar 401 sin token en los 4 endpoints.
- Confirmar que cualquier rol autenticado puede disparar `sincronizar-inyector` (no hay restriccion por rol en el codigo) — si el negocio esperaba que solo BIOMEDICA/ADMIN pudiera hacerlo, es un hallazgo a reportar, no un supuesto a validar como si ya existiera.
- No hay restriccion de sede (DEF-03) en ningun endpoint de este controller: la sincronizacion del inyector es global (procesa todos los CSV de la carpeta, sin distincion de sede del usuario que la dispara).

## Pruebas de integracion
- Encadenar: "Sincronizar paciente desde HIS" → confirmar en el modulo 03 que el paciente aparece actualizado (`sincronizadoHisEn` no nulo, aunque ese campo no se expone directamente en `PacienteDetalleDTO` — se puede verificar indirectamente por los datos actualizados).
- Encadenar: colocar CSV → "Sincronizar inyector - disparo manual" → verificar en el modulo 03 (`/api/pacientes/{id}/inyecciones`) que la inyeccion importada aparece con los valores esperados → verificar en "Historial de sincronizacion" que el lote queda registrado con `registrosImportados` coincidente.
- Probar un CSV con un `numero_serie_inyector` que no existe: la fila falla, el archivo cuenta como fallido, el lote resultante debe tener `estado: "FALLIDO"` (si es el unico archivo) o `"PARCIAL"` (si hay mezcla con archivos exitosos), y el `detalle` debe incluir el mensaje de error.

## Limpieza de datos
- Los lotes de sincronizacion (`lotes_sincronizacion`) y las inyecciones/pacientes creados por CSV no tienen endpoint de borrado — requieren limpieza manual de BD.
- Los archivos CSV procesados se MUEVEN (no se copian) a la subcarpeta `procesados/` dentro de la carpeta de importacion tras un procesamiento exitoso — para reintentar el mismo archivo hay que moverlo de vuelta manualmente en el filesystem del servidor.

## Resultados esperados
Los casos de HIS (200/404) y el historial deben pasar siempre que haya al menos un paciente de prueba. El disparo de `sincronizar-inyector` siempre debe dar 200, incluso sin archivos — solo el CONTENIDO del lote (`registrosImportados`, `estado`) cambia segun haya o no CSV validos.

## Problemas frecuentes
- Intentar enviar un archivo CSV como `multipart/form-data` en el body de `POST /sincronizar-inyector` esperando que el backend lo procese: el endpoint real IGNORA cualquier body — no tiene `@RequestParam`/`@RequestBody`. El archivo debe existir de antemano en el filesystem del servidor. Esta es la discrepancia mas importante a comunicar al equipo si se asumia lo contrario.
- Confundir el 404 de este modulo con el 400 de Pacientes/Checklist: aqui el controller SI construye un 404 explicito (`ResponseEntity.notFound()`), mientras que en los otros dos controllers un recurso inexistente da 400 via `IllegalArgumentException`. Son patrones distintos dentro del mismo backend, ambos verificados en el codigo.
- El job programado (`@Scheduled` cada 15 min) esta deshabilitado por defecto (`app.sincronizacion.habilitada=false`); si un ambiente de pruebas lo tiene habilitado, los lotes del historial pueden incluir corridas automaticas ademas de las manuales disparadas desde Postman, lo cual puede confundir el conteo esperado de lotes en aserciones estrictas de cantidad.

## Limitaciones
- **La creacion de inyecciones NO es un endpoint de API convencional.** No hay forma de crear una inyeccion "de prueba" via un POST directo con JSON; siempre requiere: (a) colocar un CSV en el filesystem del servidor con el formato exacto documentado arriba, y (b) disparar `POST /sincronizar-inyector` (o esperar el job programado). Esto limita la automatizacion end-to-end de esta suite en un pipeline puramente HTTP: cualquier corrida de Newman que dependa de datos de inyecciones nuevas necesita un paso previo fuera de Postman (script de shell/CI que copie el CSV al servidor).
- **El HIS es 100% simulado.** No hay integracion HL7/FHIR real implementada; `buscarPaciente()` nunca trae datos que no existan ya en la BD local, por lo que `POST /his/{id}/sincronizar` nunca puede "traer un paciente nuevo desde fuera" — solo actualiza pacientes que el sistema ya conoce.
- El parseo del CSV es posicional y sensible al orden exacto de columnas (8 columnas fijas, separador coma simple, sin manejo de comillas/escapes) — no es un parser CSV robusto (no usa una libreria como OpenCSV), asi que campos con comas dentro (ej. nombres de protocolo con coma) romperian el parseo.

## Evidencias recomendadas
- Captura del 404 real de `/his/{id}` para contrastar con el 400 de Pacientes/Checklist.
- Captura del `LoteSincronizacionDTO` de un disparo con CSV real importado, mostrando `registrosImportados > 0`.
- Captura de la carpeta `importaciones-iris/procesados/` del servidor tras una corrida exitosa, como evidencia de que el archivo se movio.
