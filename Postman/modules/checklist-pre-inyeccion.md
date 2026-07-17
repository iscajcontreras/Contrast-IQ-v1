# Modulo 04 - Checklist Pre-Inyeccion

## Objetivo
Validar el checklist de seguridad del paciente que debe completarse (y firmarse digitalmente) antes de una inyeccion de contraste: verificacion de identidad, revision de GFR (funcion renal), revision de alergias y consentimiento informado firmado.

## Responsabilidades
- Crear un checklist asociado a un paciente (y opcionalmente a una sala), dejando un snapshot congelado del GFR y alergias del paciente en ese momento.
- Rechazar checklists incompletos o con cualquiera de los 4 checks de seguridad en `false`.
- Exponer el historial de checklists de un paciente.

Fuente real: `controller/ChecklistPreInyeccionController.java`, `service/ChecklistPreInyeccionService.java`, `model/ChecklistPreInyeccion.java`, DTOs `dto/ChecklistPreInyeccionDTO.java`, `dto/CrearChecklistRequest.java`.

## URL base
`{{apiBaseUrl}}/api/checklists`

## Dependencias
- Requiere un `pacienteId` existente (ver modulo 03/13 para como se generan pacientes).
- `salaId` es opcional; si se envia, debe existir en `salas` (catalogo, modulo 05).
- El `operador` del checklist se resuelve del JWT (`sub` = email) contra la tabla `usuarios` — el usuario autenticado debe existir como `Usuario` real en BD, no solo tener un JWT valido en abstracto.

## Endpoints
| Metodo | Path | Request name en la coleccion |
|---|---|---|
| POST | `/api/checklists` | "Crear checklist valido (201)", "Crear checklist sin salaId (201 - sala opcional)", "Crear checklist con identidadVerificada=false (400 - AssertTrue)", "Crear checklist con gfrRevisado=false (400 - AssertTrue)", "Crear checklist con alergiasRevisadas=false (400 - AssertTrue)", "Crear checklist con consentimientoFirmado=false (400 - AssertTrue)", "Crear checklist sin pacienteId (400 - NotNull)", "Crear checklist sin firma (400 - NotBlank firmaImagenBase64/firmaNombre)", "Crear checklist con paciente inexistente (400)", "Crear checklist con sala inexistente (400)", "Crear checklist sin JWT (401)" |
| GET | `/api/checklists/paciente/{pacienteId}` | "Historial de checklists por paciente (200)", "Historial de checklists de paciente inexistente (200 - lista vacia)" |

## Autenticacion
JWT Bearer obligatorio (`Authorization: Bearer {{accessToken}}`). Sin `@PreAuthorize`/`@RequierePermiso` en este controller: cualquiera de los 5 roles puede crear/consultar checklists.

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR (los 5, sin diferenciacion por rol a nivel de codigo). En la practica clinica el checklist lo llenaria un TECNICO o RADIOLOGO, pero el backend actual no lo restringe.

## Variables necesarias
- `apiBaseUrl`, `accessToken`
- `pacienteId` — reutilizado del modulo 03 (o generado ahi mismo)
- `salaId` — obtenido del modulo 05 (catalogo de salas)
- `checklistId` — se autocompleta con `pm.environment.set` tras crear un checklist valido

## Datos de prueba
- Un paciente existente con `gfrMlMin` cargado (para verificar `riesgoRenalMomento` calculado y congelado).
- Una sala existente (opcional, para probar el caso con `salaId`).
- Una imagen de firma en base64 pequena para no inflar el payload de pruebas (el campo soporta hasta 100.000.000 caracteres en el esquema, pero para pruebas basta un PNG minimo de 1x1 — ya incluido en las requests de la coleccion).

## Orden recomendado
1. "Crear checklist valido (201)" — captura `checklistId`.
2. "Crear checklist sin salaId (201 - sala opcional)".
3. "Crear checklist con identidadVerificada=false (400 - AssertTrue)".
4. "Crear checklist con gfrRevisado=false (400 - AssertTrue)".
5. "Crear checklist con alergiasRevisadas=false (400 - AssertTrue)".
6. "Crear checklist con consentimientoFirmado=false (400 - AssertTrue)".
7. "Crear checklist sin pacienteId (400 - NotNull)".
8. "Crear checklist sin firma (400 - NotBlank firmaImagenBase64/firmaNombre)".
9. "Crear checklist con paciente inexistente (400)".
10. "Crear checklist con sala inexistente (400)".
11. "Crear checklist sin JWT (401)".
12. "Historial de checklists por paciente (200)".
13. "Historial de checklists de paciente inexistente (200 - lista vacia)".

## Casos positivos
- Checklist con los 4 checks en `true`, `pacienteId` valido, `firmaNombre`/`firmaImagenBase64` no vacios → 201, con `riesgoRenalMomento` calculado a partir del GFR actual del paciente y `alergiasMomento` copiado del paciente (snapshot).
- Checklist sin `salaId` → 201, `sala: null` en la respuesta (campo opcional).

## Casos negativos
- Cualquiera de `identidadVerificada`, `gfrRevisado`, `alergiasRevisadas`, `consentimientoFirmado` en `false` (no solo ausente) → **400**, porque son `@AssertTrue` ademas de `@NotNull`: `false` es un valor valido para el binding pero invalido para la regla de negocio.
- `pacienteId` ausente → 400 (`@NotNull`).
- `firmaNombre` o `firmaImagenBase64` vacios/en blanco → 400 (`@NotBlank`).
- `pacienteId` que no existe en BD → 400 (`IllegalArgumentException` en el servicio, no una violacion de Bean Validation — mismo patron que en Pacientes).
- `salaId` que no existe en BD → 400 (`IllegalArgumentException`).
- Sin JWT → 401.

## Reglas de negocio
- Los 4 checks de seguridad (`identidadVerificada`, `gfrRevisado`, `alergiasRevisadas`, `consentimientoFirmado`) deben venir **explicitamente en `true`** — la validacion `@AssertTrue` de Jakarta Validation solo pasa con `true`; `false`, `null` o ausente fallan todos con 400 (aunque el mensaje de error varia: `null` dispara el mensaje de `@NotNull`, `false` dispara el de `@AssertTrue`, ambos terminan en el mismo campo dentro de `errores`).
- El checklist congela (`gfrValorMomento`, `riesgoRenalMomento`, `alergiasMomento`) el estado clinico del paciente EN EL MOMENTO de crear el checklist — cambios posteriores al paciente no alteran checklists ya creados.
- El checklist **no esta atado a una fila de `inyecciones`**: es un control de negocio previo, independiente de si la inyeccion real llega a registrarse despues via sincronizacion del inyector (modulo 13). No hay ningun enlace `checklist -> inyeccion` en el modelo actual.
- El operador se determina siempre del JWT autenticado (email como `sub`), nunca del body — no es posible crear un checklist "a nombre de otro usuario" via este endpoint.

## Codigos HTTP esperados
| Situacion | Codigo |
|---|---|
| Checklist creado correctamente | 201 |
| Body invalido (Bean Validation: `@NotNull`, `@AssertTrue`, `@NotBlank`) | 400 |
| Paciente o sala referenciados no existen | 400 |
| Falta o JWT invalido | 401 |
| Consulta de historial (paciente existente o no) | 200 |

## Ejemplos de request/response reales
`POST /api/checklists`
```json
{
  "pacienteId": 42,
  "salaId": 3,
  "identidadVerificada": true,
  "gfrRevisado": true,
  "alergiasRevisadas": true,
  "consentimientoFirmado": true,
  "observaciones": "Paciente estable, sin antecedentes relevantes.",
  "firmaNombre": "Dr. Juan Perez",
  "firmaImagenBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
}
```
Respuesta 201 (resumida):
```json
{
  "id": 501,
  "pacienteId": 42,
  "sala": "Sala 2 - TAC",
  "operador": "Dra. Ana Gomez",
  "identidadVerificada": true,
  "gfrRevisado": true,
  "gfrValorMomento": 54.30,
  "riesgoRenalMomento": true,
  "alergiasRevisadas": true,
  "alergiasMomento": "Penicilina",
  "consentimientoFirmado": true,
  "fechaHora": "2026-07-17T10:12:41.223"
}
```
`identidadVerificada: false` → 400
```json
{
  "timestamp": "2026-07-17T10:13:00.001",
  "mensaje": "Datos invalidos",
  "errores": {
    "identidadVerificada": "Debes verificar la identidad del paciente antes de continuar"
  }
}
```

## Como ejecutar en Postman/Newman
```bash
newman run ContrastIQ.postman_collection.json \
  -e ContrastIQ.local.postman_environment.json \
  --folder "04 - Checklist Pre-Inyeccion"
```
Requiere `pacienteId` (modulo 03) y opcionalmente `salaId` (modulo 05) ya en el environment.

## Pruebas de seguridad
- Confirmar 401 sin token.
- No hay restriccion de sede (DEF-03) implementada en este servicio (a diferencia de Pacientes): un usuario de cualquier sede puede crear/consultar checklists de un paciente de cualquier otra sede. Se documenta como hallazgo a validar con el equipo de negocio, no como bug confirmado, pero es una discrepancia respecto al patron DEF-03 aplicado en `PacienteController`.

## Pruebas de integracion
- Crear un checklist para un paciente recien sincronizado desde el HIS (modulo 13) y verificar que `alergiasMomento`/`gfrValorMomento` reflejan lo recien sincronizado.
- Verificar que `GET /api/checklists/paciente/{id}` devuelve los checklists en orden `fechaHora DESC` tras crear varios seguidos.

## Limpieza de datos
No hay endpoint DELETE para checklists. Los generados en pruebas quedan permanentemente en `checklist_pre_inyeccion` salvo limpieza manual de BD entre corridas.

## Resultados esperados
Los 8 casos negativos de validacion deben devolver 400 con el campo correspondiente en `errores`; los 2 positivos, 201 con el checklist persistido; el historial, 200 con array (vacio o no).

## Problemas frecuentes
- Enviar `identidadVerificada: false` esperando 200 con un checklist "incompleto" — el diseño explicitamente lo prohibe, no es un bug.
- Olvidar que `pacienteId` inexistente da 400 y no 404 — igual que en Pacientes, no es el REST "estandar" pero es el comportamiento real y verificado del backend.
- Payloads de `firmaImagenBase64` muy grandes en pruebas automatizadas de carga: el campo soporta hasta 100MB en el esquema, pero para tests funcionales basta un PNG minimo.

## Limitaciones
- No existe endpoint para editar o eliminar un checklist ya creado.
- No hay vinculo estructural entre un checklist y la inyeccion que eventualmente se realice; la trazabilidad "este checklist correspondio a esta inyeccion" debe inferirse externamente (por paciente + ventana de tiempo), no esta modelada en BD.
- Sin restriccion de sede: cualquier usuario autenticado puede crear/ver checklists de pacientes de cualquier sede.

## Evidencias recomendadas
- Captura de los 4 casos 400 de `@AssertTrue` mostrando el campo especifico en `errores`.
- Captura del checklist creado (201) con `riesgoRenalMomento` coincidente con el GFR real del paciente usado.
