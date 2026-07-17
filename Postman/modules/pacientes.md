# Modulo 03 - Pacientes

## Objetivo
Validar el "Dashboard de Paciente" de ContrastIQ: busqueda paginada, perfil clinico consolidado (funcion renal, indicadores de exposicion al contraste) e historial de inyecciones y reacciones de un paciente concreto. Es la vista que usa, por ejemplo, un radiologo para revisar antecedentes antes de autorizar una nueva inyeccion.

## Responsabilidades
- Exponer busqueda de pacientes con paginacion y orden fijo por `nombreCompleto`.
- Exponer el detalle clinico agregado de un paciente (GFR, riesgo renal, totales calculados sobre su historial de inyecciones).
- Exponer el historial completo de inyecciones de un paciente.
- Exponer el historial de reacciones/eventos de extravasacion (EDA) de un paciente.
- Aplicar la restriccion de sede (DEF-03) a todo acceso por `pacienteId` para usuarios no-ADMIN.

Fuente real: `controller/PacienteController.java`, `service/PacienteService.java`, `model/Paciente.java`, DTOs `dto/PacienteResumenDTO.java`, `dto/PacienteDetalleDTO.java`, `dto/HistorialInyeccionPacienteDTO.java`, `dto/ReaccionPacienteDTO.java`.

## URL base
`{{apiBaseUrl}}/api/pacientes` (monolito en el puerto 8080; `apiBaseUrl` local = `http://localhost:8080/api`).

## Dependencias
- Requiere que existan pacientes en BD. Los pacientes se crean/actualizan por dos vias reales: sincronizacion desde HIS (`POST /api/integracion-clinica/his/{id}/sincronizar`, ver modulo 13) o como efecto colateral de `SincronizacionInyectorService` al importar un CSV de inyecciones (crea el paciente si el MRN no existe).
- El historial de inyecciones/reacciones depende de que ese paciente tenga filas en `inyecciones` (solo se crean via sincronizacion del inyector, ver modulo 13) y en `eventos_extravasacion`.
- Depende de `UsuarioAutenticadoService.sedeIdRestriccion()` para la restriccion por sede.

## Endpoints
| Metodo | Path | Request name en la coleccion |
|---|---|---|
| GET | `/api/pacientes?busqueda&page&size` | "Buscar pacientes (200 - sin filtro)", "Buscar pacientes con termino de busqueda (200)", "Buscar pacientes sin JWT (401)" |
| GET | `/api/pacientes/{id}` | "Obtener detalle de paciente (200)", "Obtener detalle de paciente inexistente (400)", "Obtener detalle de paciente de otra sede (403 - no ADMIN)" |
| GET | `/api/pacientes/{id}/inyecciones` | "Historial de inyecciones de paciente (200)", "Historial de inyecciones de paciente inexistente (200 - lista vacia)" |
| GET | `/api/pacientes/{id}/reacciones` | "Reacciones de paciente (200)", "Reacciones de paciente de otra sede (403 - no ADMIN)" |

## Autenticacion
JWT Bearer obligatorio en todas las rutas (`Authorization: Bearer {{accessToken}}`). No hay `@PreAuthorize` por rol ni `@RequierePermiso` en `PacienteController`: cualquiera de los 5 roles autenticados puede llamar estos endpoints. El control real de acceso NO es por rol sino por sede (ver Reglas de negocio).

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR (los 5, sin distincion a nivel de anotacion). La diferencia de comportamiento entre roles es por sede, no por rol: ADMIN siempre ve todo; los otros 4 quedan restringidos a su `sedeId` si tienen una asignada.

## Variables necesarias
- `apiBaseUrl` (colección/entorno)
- `accessToken` — token de un usuario cualquiera (para casos positivos, usar preferentemente ADMIN para evitar falsos negativos por DEF-03)
- `tecnicoOtraSedeToken` — token de un usuario NO-ADMIN con `sedeId` asignada, usado en los casos negativos 403
- `pacienteId` — se autocompleta con `pm.environment.set` desde las respuestas de busqueda/detalle
- `pacienteIdOtraSede` — id de un paciente que SOLO tiene inyecciones en una sede distinta a la del usuario de `tecnicoOtraSedeToken` (debe prepararse a mano, ver Datos de prueba)
- `pacienteBusquedaTexto` — texto parcial de nombre/identificador para el caso de busqueda filtrada

## Datos de prueba
Para ejecutar la suite completa se necesita, como minimo:
1. Un usuario ADMIN con `accessToken` valido.
2. Un usuario no-ADMIN (TECNICO/RADIOLOGO/BIOMEDICA/VISUALIZADOR) con `sedeId` asignado y `tecnicoOtraSedeToken` valido.
3. Al menos un paciente con inyecciones registradas en una sede DISTINTA a la de ese usuario no-ADMIN, para forzar el 403 de DEF-03 (`pacienteIdOtraSede`).
4. Al menos un paciente con `gfrMlMin < 60` para observar `riesgoRenal: true` en el detalle.
5. Al menos un paciente con eventos de extravasacion (`estadoEda = FUERA_DE_RANGO`) para poblar `alertasEdaFueraDeRango` y el endpoint de reacciones.

Recuerda que los pacientes e inyecciones de prueba se generan indirectamente via los modulos 13 (Integracion Clinica) y de checklists/inyector — no hay un `POST /api/pacientes` manual en el backend.

## Orden recomendado
1. "Buscar pacientes (200 - sin filtro)" — captura `pacienteId`.
2. "Buscar pacientes con termino de busqueda (200)".
3. "Buscar pacientes sin JWT (401)".
4. "Obtener detalle de paciente (200)".
5. "Obtener detalle de paciente inexistente (400)".
6. "Obtener detalle de paciente de otra sede (403 - no ADMIN)".
7. "Historial de inyecciones de paciente (200)".
8. "Historial de inyecciones de paciente inexistente (200 - lista vacia)".
9. "Reacciones de paciente (200)".
10. "Reacciones de paciente de otra sede (403 - no ADMIN)".

## Casos positivos
- Busqueda sin filtro devuelve `Page<PacienteResumenDTO>` con `content[]`.
- Busqueda con `busqueda` no vacio filtra por nombre/identificador (puede devolver `content: []` si no hay match — no es error).
- Detalle de paciente accesible devuelve `PacienteDetalleDTO` completo, incluyendo `riesgoRenal` calculado (`gfrMlMin < 60`).
- Historial de inyecciones y reacciones devuelven arrays (posiblemente vacios) para un paciente accesible.

## Casos negativos
- Sin `Authorization` header → 401 en cualquier endpoint (exigido por `ResourceServerConfig`, no especifico de este controller).
- `GET /api/pacientes/{id}` con id inexistente → **400**, no 404 (`IllegalArgumentException` capturada por `ManejadorGlobalExcepciones.manejarArgumentoInvalido`).
- `GET /api/pacientes/{id}` / `/reacciones` de un paciente cuya unica sede con inyecciones es distinta a la del usuario no-ADMIN → **403** (`AccessDeniedException` de `verificarAccesoAPaciente()`).
- `GET /api/pacientes/{id}/inyecciones` con id inexistente → **200 con lista vacia** para ADMIN (este metodo no valida existencia del paciente, solo consulta inyecciones); para un usuario no-ADMIN puede dar 403 si `existsByPaciente_IdAndInyector_Sala_Sede_Id` devuelve false para ese id inexistente.

## Reglas de negocio
- **DEF-03 (restriccion de sede):** un usuario no-ADMIN con `sedeId` asignado solo puede: (a) ver en la busqueda pacientes con al menos una inyeccion registrada en su sede (`PacienteSpecification.conSedeRestriccion`, implementado como `EXISTS` correlacionado sobre `inyecciones`), y (b) acceder al detalle/inyecciones/reacciones de un paciente solo si ese paciente tiene al menos una inyeccion en la sede del usuario (`inyeccionRepository.existsByPaciente_IdAndInyector_Sala_Sede_Id`). Un ADMIN, o un usuario sin `sedeId` asignado (`sedeIdRestriccion() == null`), no tiene esta restriccion.
- `riesgoRenal` se calcula server-side: `gfrMlMin != null && gfrMlMin < 60` (umbral `UMBRAL_GFR_RIESGO` en `PacienteService`), es informativo, no bloquea nada por si solo (el bloqueo real de seguridad vive en el checklist, modulo 04).
- Los indicadores del detalle (`totalInyecciones`, `volumenTotalRecibidoMl`, `dlpTotalMgyCm`, `ultimaInyeccion`, `alertasEdaFueraDeRango`, `inyeccionesAbortadasOError`) se calculan en tiempo real con queries agregadas sobre `inyecciones`/`eventos_extravasacion`, no son columnas persistidas del paciente.

## Codigos HTTP esperados
| Situacion | Codigo |
|---|---|
| Busqueda/detalle/historial exitoso | 200 |
| Falta o JWT invalido | 401 |
| Paciente inexistente en `/pacientes/{id}` | 400 |
| Paciente de otra sede (no-ADMIN) | 403 |
| Error interno no controlado | 500 |

## Ejemplos de request/response reales
`GET /api/pacientes/42`
```json
{
  "id": 42,
  "identificadorExterno": "MRN-000042",
  "nombreCompleto": "Maria Fernanda Lopez",
  "sexo": "F",
  "pesoKg": 68.50,
  "creatininaMgDl": 1.10,
  "gfrMlMin": 54.30,
  "riesgoRenal": true,
  "totalInyecciones": 3,
  "volumenTotalRecibidoMl": 285.00,
  "dlpTotalMgyCm": 612.40,
  "ultimaInyeccion": "2026-07-10T09:15:00",
  "alertasEdaFueraDeRango": 1,
  "inyeccionesAbortadasOError": 0
}
```
`GET /api/pacientes/999999999` (400)
```json
{
  "timestamp": "2026-07-17T10:03:12.114",
  "mensaje": "El paciente no existe"
}
```

## Como ejecutar en Postman/Newman
```bash
newman run ContrastIQ.postman_collection.json \
  -e ContrastIQ.local.postman_environment.json \
  --folder "03 - Pacientes"
```
Ejecutar primero el modulo de autenticacion para poblar `accessToken`/`tecnicoOtraSedeToken`, y el modulo 13 o un seed manual para tener pacientes con inyecciones reales.

## Pruebas de seguridad
- Verificar 401 sin token en los 4 endpoints.
- Verificar 403 (no 200 con datos filtrados silenciosamente) cuando un usuario no-ADMIN intenta leer un paciente de otra sede via `/{id}`, `/{id}/inyecciones`... ojo: solo `/{id}` y `/{id}/reacciones` estan cubiertos explicitamente por `verificarAccesoAPaciente()`; confirmar en cada release que `historialInyecciones()` sigue llamando a la misma validacion (esta en el codigo actual).
- Confirmar que un token de rol distinto (TECNICO vs RADIOLOGO) con la misma sede tiene el mismo acceso — no hay diferenciacion por rol en este controller.

## Pruebas de integracion
- Encadenar: sincronizar paciente desde HIS (modulo 13) → buscarlo aqui → ver su detalle → confirmar que aparece en la busqueda.
- Encadenar: importar un CSV de inyector para un paciente nuevo (modulo 13) → confirmar que aparece en `/api/pacientes` y que `/inyecciones` refleja la fila importada.

## Limpieza de datos
No hay endpoint de borrado de pacientes en este controller (ni en ningun otro conocido del backend). La limpieza entre corridas de test debe hacerse a nivel de base de datos (rollback de transaccion de test, o reset del esquema de pruebas) — no via API.

## Resultados esperados
Todas las corridas del folder deberian terminar en verde salvo que la base de pruebas no tenga los datos minimos descritos en "Datos de prueba" (en ese caso, los negativos de sede fallaran por falta de fixture, no por bug).

## Problemas frecuentes
- Usar `accessToken` de un usuario no-ADMIN sin sede correcta para los casos "positivos" generales puede producir 403/200-vacio inesperados: para los positivos generales usar preferentemente un ADMIN.
- Confundir el 400 de paciente inexistente con un 404: la suite explicitamente prueba que es 400, no reportar como bug si Postman ve 400.
- `busqueda` vacio (`""`) en query string se trata como parametro presente pero vacio; revisar `PacienteSpecification.conBusqueda` si se requiere test adicional de ese borde.

## Limitaciones
- No existe endpoint para crear/editar un paciente manualmente en este controller; la creacion/edicion ocurre solo via HIS (modulo 13) o via importacion CSV del inyector.
- `historialInyecciones()` y `reacciones()` no paginan (devuelven `List`, no `Page`); en pacientes con historiales muy largos esto puede ser lento — no hay limite de resultados en el codigo actual.
- No hay test automatizado en este fragmento para confirmar el orden de la busqueda (`nombreCompleto` ASC); se puede anadir si se requiere verificacion estricta de orden.

## Evidencias recomendadas
- Captura de la respuesta 403 al intentar cross-sede.
- Captura del body 400 de "paciente no existe" para dejar constancia del comportamiento no-REST-estandar.
- Export de la consola de Newman con el folder completo en verde.
