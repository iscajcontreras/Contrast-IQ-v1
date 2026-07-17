# Modulo 14 - Reportes

## Objetivo

Cubrir con pruebas de Postman/Newman el modulo de reportes ejecutivos (`ReporteEjecutivoController`, `/api/reportes`): comparativa de KPIs entre sedes para un rango de fechas, disponible tanto en JSON como en un archivo `.xlsx` generado en memoria con Apache POI.

## Responsabilidades

- Calcular, por sede, el total de inyecciones, el volumen total en ml, las inyecciones fallidas y la tasa de falla porcentual, para un rango `desde`/`hasta`.
- Exportar esa misma comparativa como archivo Excel (`.xlsx`) descargable, con formato (titulo, encabezados con color, autosize de columnas).

## URL base

`{{apiBaseUrl}}/api/reportes`

## Dependencias

- Requiere inyecciones existentes en el rango de fechas consultado para que la comparativa no devuelva un arreglo vacio (aunque un arreglo vacio tambien es una respuesta 200 valida).
- No depende de otros folders del grupo D, pero conceptualmente se relaciona con los KPIs de Dashboard e Inyecciones (misma fuente de datos: `inyecciones`).

## Autenticación

JWT Bearer en `Authorization: Bearer {{accessToken}}`. Sin token, 401.

## Roles permitidos

**Hallazgo importante**: `ReporteEjecutivoController` no tiene ninguna anotacion `@RequierePermiso` ni `@PreAuthorize` en ninguno de sus 2 endpoints. Cualquiera de los 5 roles con JWT valido puede consultar la comparativa y descargar el Excel. Esto contrasta con la matriz de permisos sembrada, donde el modulo `REPORTES` esta explicitamente configurado solo para RADIOLOGO (`VER`, `EXPORTAR`) ademas de ADMIN (todos los permisos por defecto) — TECNICO, BIOMEDICA y VISUALIZADOR no tienen `REPORTES` en el seed, pero igual pueden usar ambos endpoints porque el controller no valida el permiso.

## Endpoints

| Metodo | Path | Nombre en groupD.json | Query obligatorio |
|---|---|---|---|
| GET | `/api/reportes/comparativa-sedes` | Comparativa entre sedes (JSON) | `desde`, `hasta` (LocalDateTime ISO) |
| GET | `/api/reportes/comparativa-sedes/excel` | Comparativa entre sedes - exportar Excel (.xlsx) | `desde`, `hasta` (LocalDateTime ISO) |

## Variables necesarias

- `apiBaseUrl`, `accessToken`

## Datos de prueba

- `desde`/`hasta` en formato `LocalDateTime` ISO completo (`yyyy-MM-ddTHH:mm:ss`), no solo fecha — a diferencia de las vistas de merma "por rango" (`resumen`, `por-sede`, etc.) que usan `LocalDate`.

## Orden recomendado

1. Comparativa entre sedes (JSON) — caso positivo y negativos (rango invertido, falta parametro, sin token).
2. Comparativa entre sedes - exportar Excel (.xlsx) — caso positivo (validando Content-Type y tamano de body, NO parseando JSON) y negativos (rango invertido, sin token).

## Casos positivos

- Consultar la comparativa en JSON con un rango de fechas valido.
- Descargar el `.xlsx` con el mismo rango y confirmar:
  - `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
  - `Content-Disposition` incluye `comparativa_sedes.xlsx`
  - El cuerpo de la respuesta no esta vacio (`pm.response.responseSize > 0`).

## Casos negativos

- Rango de fechas invertido (`desde` posterior a `hasta`) en ambos endpoints → 400 (`ValidadorRangoFechas`, fix DEF-04 — reutilizado aqui tal como en Merma de insumos).
- Falta parametro obligatorio `desde` o `hasta` → 400 (ambos son `@RequestParam` sin `defaultValue`, sin `required=false`).
- Sin token → 401 en ambos endpoints.

## Reglas de negocio

- `exportarComparativaSedesExcel` reutiliza internamente `comparativaSedes(desde, hasta)` — por eso comparte exactamente la misma validacion de rango y los mismos datos que el endpoint JSON; no hay una segunda fuente de verdad para el Excel.
- `tasaFallaPorcentaje` se calcula como `round(fallidas * 10000.0 / total) / 100.0` (2 decimales), y es `0.0` si `total == 0` (evita division por cero).
- El archivo se genera 100% en memoria (`ByteArrayOutputStream`), sin tocar disco — si la generacion falla por I/O, se traduce a `IllegalStateException` ("No se pudo generar el archivo Excel"), que cae en el manejador generico de `Exception` → 500 (no 400, a diferencia de los otros errores de este modulo).

## Codigos HTTP esperados

| Escenario | Codigo |
|---|---|
| Comparativa JSON OK | 200 |
| Descarga Excel OK | 200 |
| Rango de fechas invertido | 400 |
| Falta parametro obligatorio | 400 |
| Sin token | 401 |
| Fallo interno de generacion del Excel (no reproducible en pruebas normales) | 500 |

## Ejemplos de request/response reales

GET `/api/reportes/comparativa-sedes?desde=2026-07-01T00:00:00&hasta=2026-07-17T23:59:59` (response):
```json
[
  {
    "sedeId": 1,
    "sede": "Hospital Central",
    "totalInyecciones": 340,
    "volumenTotalMl": 42500.00,
    "inyeccionesFallidas": 12,
    "tasaFallaPorcentaje": 3.53
  },
  {
    "sedeId": 2,
    "sede": "Clinica Norte",
    "totalInyecciones": 210,
    "volumenTotalMl": 26000.00,
    "inyeccionesFallidas": 5,
    "tasaFallaPorcentaje": 2.38
  }
]
```

GET `/api/reportes/comparativa-sedes/excel?desde=...&hasta=...` (response): binario `.xlsx`, cabeceras relevantes:
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="comparativa_sedes.xlsx"
```

## Como ejecutar en Postman/Newman

1. Ejecutar el folder de autenticacion primero.
2. Ejecutar "14 - Reportes" en orden.
3. Newman: `newman run ContrastIQ.postman_collection.json -e ContrastIQ.postman_environment.json --folder "14 - Reportes"`.
4. Importante: para el request de Excel, Newman/Postman no debe intentar `pm.response.json()` — los scripts de test de ese request solo validan Content-Type, Content-Disposition y tamano de respuesta, tal como pide el brief de esta tarea.

## Pruebas de seguridad

- Confirmar 401 sin token en ambos endpoints.
- Confirmar que roles fuera del seed de `REPORTES` (TECNICO, BIOMEDICA, VISUALIZADOR) igual reciben 200 — hallazgo de seguridad: el modulo `REPORTES` de la matriz de permisos no se aplica en este controller.

## Pruebas de integracion

- Confirmar que la suma de `totalInyecciones` por sede en la comparativa coincide con el conteo real de inyecciones en ese rango (cruzar contra el folder de Inyecciones/Dashboard).
- Confirmar que el Excel descargado, al abrirse, contiene exactamente las mismas filas/valores que la respuesta JSON del mismo rango (verificacion manual, fuera del alcance de un test automatizado de Postman).

## Limpieza de datos

- No aplica: ambos endpoints son de solo lectura, no crean ni modifican datos.

## Resultados esperados

200 en ambos endpoints con datos validos; 400 en rango invertido o parametro faltante; 401 sin token.

## Problemas frecuentes

- Intentar parsear el `.xlsx` como JSON en el test script (`pm.response.json()`) — falla porque el cuerpo es binario, no texto. El test correcto valida solo Content-Type, Content-Disposition y responseSize.
- Usar formato `LocalDate` (`yyyy-MM-dd`) en `desde`/`hasta` en vez de `LocalDateTime` completo — este modulo, a diferencia de las vistas por-rango de Merma, exige el formato `LocalDateTime` ISO completo.

## Limitaciones

- No hay control de acceso por rol/permiso a nivel de controller, pese a que el modulo `REPORTES` existe en el catalogo y esta pensado para RADIOLOGO + ADMIN.
- No hay paginacion en `comparativa-sedes` — si crece el numero de sedes, la respuesta crece proporcionalmente (en la practica no es un problema, el numero de sedes es acotado).
- El Excel no incluye graficos ni formato condicional, solo tabla con encabezado estilizado.

## Evidencias recomendadas

- Captura de la respuesta JSON de comparativa-sedes con al menos 2 sedes.
- Captura de la descarga del `.xlsx` (headers Content-Type/Content-Disposition) y, si es posible, screenshot del archivo abierto en Excel/LibreOffice mostrando las columnas esperadas.
- Captura del 400 por rango de fechas invertido.
