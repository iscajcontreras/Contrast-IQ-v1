# Modulo 07 - Dashboard Operativo

## Objetivo
Validar `DashboardController` (`/api/dashboard`), que alimenta las 4 tarjetas KPI y los 2
graficos de la parte superior del dashboard (uso de contraste por dia, distribucion por
identificador anatomico). A diferencia de `06 - Inyecciones` (lista fila por fila), este modulo
solo expone vistas **agregadas** sobre un rango de fechas.

## Responsabilidades
- `GET /api/dashboard/kpis`: 4 metricas agregadas del periodo (rango opcional, default = hoy).
- `GET /api/dashboard/uso-contraste`: serie diaria de volumen de contraste usado (rango
  obligatorio).
- `GET /api/dashboard/distribucion-protocolo`: distribucion porcentual por identificador
  anatomico (rango obligatorio; el nombre del endpoint dice "protocolo" mas el DTO/metodo real
  agrupa por **identificador anatomico**, no por protocolo -- ver "Problemas frecuentes").

## URL base
`{{apiBaseUrl}}/api/dashboard`

## Dependencias
Depende de que existan inyecciones en el rango de fechas consultado (mismo seed que
`06 - Inyecciones`). No depende de ningun otro modulo de esta carpeta.

## Autenticacion
Header `Authorization: Bearer {{accessToken}}`. `DashboardController` **no tiene
`@RequierePermiso`** en ningun endpoint: solo exige autenticacion generica
(`anyRequest().authenticated()` en `ResourceServerConfig`). Cualquiera de los 5 roles puede
consultarlo.

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR -- los 5, sin diferenciacion. A diferencia de
`06 - Inyecciones`, **este controller no aplica la restriccion de sede (DEF-03)**: las 3
consultas del dashboard operativo son globales, no filtran por `sede_id` del usuario. Esto se
documenta como hallazgo (ver "Problemas frecuentes"), no como comportamiento verificado a
propósito en el codigo fuente leido.

## Endpoints

| Metodo | Path | Request en la coleccion |
|---|---|---|
| GET | `/api/dashboard/kpis` | `GET KPIs del dashboard - rango por defecto (hoy)` / `GET KPIs del dashboard - rango de fechas` |
| GET | `/api/dashboard/uso-contraste` | `GET Uso de contraste por dia` / `GET Uso de contraste por dia - falta parametro obligatorio (negativo)` |
| GET | `/api/dashboard/distribucion-protocolo` | `GET Distribucion por identificador anatomico` / `GET Distribucion por identificador anatomico - falta parametro obligatorio (negativo)` |

### Parametros
- `kpis`: `desde`, `hasta` (`LocalDate`, ISO `yyyy-MM-dd`), **ambos opcionales** -- si no se
  mandan, el controller usa `LocalDate.now()` para los dos (hoy 00:00 a hoy 23:59:59).
- `uso-contraste` y `distribucion-protocolo`: `desde`, `hasta` **obligatorios** (`@RequestParam`
  sin `required = false`).

## Variables necesarias
`apiBaseUrl`, `accessToken`. No hay IDs que este modulo genere ni consuma de otras carpetas.

## Datos de prueba
No aplica: solo lectura agregada sobre datos ya existentes del seed.

## Orden recomendado
1. `GET KPIs del dashboard - rango por defecto (hoy)`
2. `GET KPIs del dashboard - rango de fechas`
3. `GET Uso de contraste por dia`
4. `GET Uso de contraste por dia - falta parametro obligatorio (negativo)`
5. `GET Distribucion por identificador anatomico`
6. `GET Distribucion por identificador anatomico - falta parametro obligatorio (negativo)`

## Casos positivos
- KPIs sin parametros (usa el dia de hoy).
- KPIs con `desde`/`hasta` explicitos.
- Uso de contraste y distribucion con `desde`/`hasta` validos.

## Casos negativos
- Omitir `desde`/`hasta` en `uso-contraste` o `distribucion-protocolo` (son obligatorios, sin
  `required = false`): Spring lanza `MissingServletRequestParameterException`. Como
  `ManejadorGlobalExcepciones` **no tiene un `@ExceptionHandler` especifico** para esa excepcion,
  cae en el catch-all `@ExceptionHandler(Exception.class)`, que responde **500 Internal Server
  Error** en vez del 400 que uno esperaria de un parametro faltante. Se documenta el
  comportamiento real, no el ideal.
- No se prueba "rango de fechas invertido" (`desde` > `hasta`): ni el controller ni
  `DashboardService` validan ese orden -- las queries (`countByFechaHoraInicioBetween`,
  `sumarVolumenEntreFechas`, etc.) simplemente no devuelven filas si el rango es logicamente
  invalido, sin lanzar ningun error. No se inventa ese negativo porque el codigo no lo valida.

## Reglas de negocio
- `kpis` calcula `volumenPromedioMl = volumenTotal / totalInyecciones` (con `RoundingMode.HALF_UP`,
  2 decimales); si `totalInyecciones == 0` el promedio es `0` en vez de dividir por cero.
- `alertasEdaFueraDeRango` cuenta eventos de `eventos_extravasacion` con
  `estadoEda = FUERA_DE_RANGO` **dentro del mismo rango de fechas** que las demas metricas.
- `distribucion-protocolo` agrupa por identificador anatomico (no por protocolo, pese al nombre
  del endpoint) y calcula el porcentaje de cada grupo sobre el total del rango, redondeado a 1
  decimal.

## Codigos HTTP esperados
| Escenario | Status |
|---|---|
| KPIs (con o sin rango) | 200 |
| Uso de contraste / distribucion con rango completo | 200 |
| Falta `desde`/`hasta` en endpoints que los exigen | 500 (ver nota arriba) |
| Sin token / token expirado | 401 |

## Ejemplos de request/response reales

`GET {{apiBaseUrl}}/api/dashboard/kpis?desde=2026-07-01&hasta=2026-07-17`
```json
{
  "inyeccionesEnPeriodo": 4820,
  "volumenTotalMl": 421340.50,
  "volumenPromedioMl": 87.42,
  "alertasEdaFueraDeRango": 12,
  "inyectoresActivos": 38,
  "inyectoresTotales": 42
}
```

`GET {{apiBaseUrl}}/api/dashboard/uso-contraste?desde=2026-07-01&hasta=2026-07-03`
```json
[
  { "fecha": "2026-07-01", "volumenMl": 15420.0, "totalInyecciones": 176 },
  { "fecha": "2026-07-02", "volumenMl": 14980.5, "totalInyecciones": 168 },
  { "fecha": "2026-07-03", "volumenMl": 15102.0, "totalInyecciones": 171 }
]
```

`GET {{apiBaseUrl}}/api/dashboard/distribucion-protocolo?desde=2026-07-01&hasta=2026-07-17`
```json
[
  { "identificadorAnatomico": "Torax", "total": 1820, "porcentaje": 37.8 },
  { "identificadorAnatomico": "Abdomen", "total": 1540, "porcentaje": 31.9 },
  { "identificadorAnatomico": "Cabeza", "total": 1460, "porcentaje": 30.3 }
]
```

## Como ejecutar en Postman/Newman
1. Cargar la coleccion y el environment con `apiBaseUrl`/`accessToken`.
2. Ejecutar `07 - Dashboard Operativo` con Postman Runner o
   `newman run coleccion.json -e entorno.json --folder "07 - Dashboard Operativo"`.
3. No depende de otras carpetas para correr (no consume ni genera IDs).

## Pruebas de seguridad
- Confirmar 401 sin `Authorization`.
- Confirmar que **cualquier** rol puede leer estos 3 endpoints (no hay 403 esperado para ningun
  rol en este modulo, a diferencia de `11 - Extravasaciones`).
- Como hallazgo de seguridad a reportar al equipo: verificar manualmente si el dashboard
  operativo deberia respetar DEF-03 (restriccion de sede) igual que `06 - Inyecciones` y
  `11 - Extravasaciones` -- el codigo leido no aplica esa restriccion aqui, lo que podria filtrar
  metricas agregadas de otras sedes a un usuario restringido.

## Pruebas de integracion
- Comparar `kpis.inyeccionesEnPeriodo` de un rango de un solo dia contra
  `GET /api/inyecciones?fechaInicio=...&fechaFin=...` (mismo rango) y verificar que
  `totalElements` de ese listado coincide con el KPI.
- Sumar los `totalInyecciones` de `uso-contraste` para un rango y compararlo contra
  `kpis.inyeccionesEnPeriodo` del mismo rango.

## Limpieza de datos
No aplica: modulo de solo lectura, no crea ni modifica datos.

## Resultados esperados
Los 4 casos positivos devuelven 200 con las formas de DTO documentadas. Los 2 casos negativos
devuelven 500 (comportamiento real observado, no un fallo de la coleccion).

## Problemas frecuentes
- **500 en vez de 400** al omitir `desde`/`hasta`: confirmado por el codigo, no es un bug de la
  coleccion de Postman -- es la ausencia de un `@ExceptionHandler` para
  `MissingServletRequestParameterException`.
- **Nombre engañoso del endpoint**: `/distribucion-protocolo` en realidad agrupa por
  identificador anatomico (`distribucionPorIdentificadorAnatomico()` en el servicio), no por
  `protocoloId`. No confundir con el filtro `protocoloId` de `06 - Inyecciones`.
- **Rangos de fechas grandes**: sobre el seed de ~90,000 inyecciones en 2 anios, pedir un rango
  amplio (ej. todo el historico) puede tardar mas en `uso-contraste` por la agregacion diaria;
  no es un error, solo mas lento.

## Limitaciones
- No hay forma de validar server-side un rango de fechas invertido (`desde` > `hasta`): el
  backend simplemente devuelve datos vacios/cero, no un error explicito.
- No se puede filtrar el dashboard operativo por sede/sala/protocolo: solo acepta rango de
  fechas. Para vistas filtradas fila por fila, usar `06 - Inyecciones`.

## Evidencias recomendadas
- Captura de los 4 KPIs con datos reales del seed.
- Captura de los 2 graficos (uso de contraste, distribucion) con al menos 3 puntos de datos.
- Captura del 500 real obtenido al omitir `desde`/`hasta`, como evidencia del hallazgo reportado.
