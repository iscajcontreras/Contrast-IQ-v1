# Modulo 05 - Catalogos

## Objetivo
Validar los endpoints de solo lectura que alimentan los `<select>` de la barra de filtros del dashboard Angular: roles, sedes, salas, inyectores, protocolos de inyeccion, agentes de contraste e identificadores anatomicos.

## Responsabilidades
- Devolver listas simples `{id, etiqueta}` (`OpcionFiltroDTO`) para poblar combos en el frontend.
- Permitir filtrado en cascada (salas por sede, inyectores por sala, protocolos por identificador anatomico) sin fallar cuando el filtro no tiene resultados.

Fuente real: `controller/CatalogoController.java`, `service/CatalogoService.java`, DTO `dto/OpcionFiltroDTO.java`, modelos `model/Sede.java`, `model/Sala.java`, `model/Inyector.java`, `model/ProtocoloInyeccion.java`, `model/AgenteContraste.java`, `model/IdentificadorAnatomico.java`.

## URL base
`{{apiBaseUrl}}/api/catalogos`

## Dependencias
- Requiere datos maestros ya cargados (sedes, salas, inyectores, protocolos, agentes, identificadores anatomicos, roles) — estos catalogos normalmente se pueblan por migraciones/seed, no hay endpoints POST/PUT de catalogo en este controller.

## Endpoints
| Metodo | Path | Request name en la coleccion |
|---|---|---|
| GET | `/api/catalogos/roles` | "Catalogo de roles (200)" |
| GET | `/api/catalogos/sedes` | "Catalogo de sedes (200)", "Catalogo de sedes sin JWT (401)" |
| GET | `/api/catalogos/salas?sedeId` | "Catalogo de salas por sede (200)", "Catalogo de salas sin sedeId (200 - todas las salas)", "Catalogo de salas con sedeId inexistente (200 - lista vacia)" |
| GET | `/api/catalogos/inyectores?salaId` | "Catalogo de inyectores por sala (200)", "Catalogo de inyectores con salaId inexistente (200 - lista vacia)" |
| GET | `/api/catalogos/protocolos?identificadorAnatomicoId` | "Catalogo de protocolos (200 - todos activos)", "Catalogo de protocolos por identificador anatomico (200)" |
| GET | `/api/catalogos/agentes-contraste` | "Catalogo de agentes de contraste (200)" |
| GET | `/api/catalogos/identificadores-anatomicos` | "Catalogo de identificadores anatomicos (200)" |

## Autenticacion
JWT Bearer obligatorio en todos los endpoints. Sin `@PreAuthorize`/`@RequierePermiso` en `CatalogoController`.

## Roles permitidos
ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR — los 5, sin diferenciacion. **Importante:** a diferencia del modulo de Pacientes, aqui NO se aplica la restriccion de sede (DEF-03): `CatalogoService.sedes()`/`salasPorSede()` no consultan `UsuarioAutenticadoService`, por lo que un usuario no-ADMIN ve el catalogo completo de sedes/salas de todo el hospital en estos combos, aunque luego su acceso a datos clinicos si quede restringido a su propia sede en otros modulos.

## Variables necesarias
- `apiBaseUrl`, `accessToken`
- `sedeId`, `salaId`, `identificadorAnatomicoId` — se autocompletan con `pm.environment.set` a partir de las respuestas de "Catalogo de sedes", "Catalogo de salas por sede" e "Catalogo de identificadores anatomicos" respectivamente.

## Datos de prueba
- Al menos una sede, una sala activa asociada a esa sede, un inyector asociado a esa sala, un protocolo activo, un agente de contraste activo y un identificador anatomico — normalmente ya vienen de datos semilla/migraciones del proyecto.
- Al menos un rol cargado en la tabla `roles` (los 5 valores del enum `NombreRol`).

## Orden recomendado
1. "Catalogo de roles (200)".
2. "Catalogo de sedes (200)" — captura `sedeId`.
3. "Catalogo de salas por sede (200)" — captura `salaId`.
4. "Catalogo de salas sin sedeId (200 - todas las salas)".
5. "Catalogo de salas con sedeId inexistente (200 - lista vacia)".
6. "Catalogo de inyectores por sala (200)".
7. "Catalogo de inyectores con salaId inexistente (200 - lista vacia)".
8. "Catalogo de identificadores anatomicos (200)" — captura `identificadorAnatomicoId`.
9. "Catalogo de protocolos (200 - todos activos)".
10. "Catalogo de protocolos por identificador anatomico (200)".
11. "Catalogo de agentes de contraste (200)".
12. "Catalogo de sedes sin JWT (401)".

## Casos positivos
- Cada catalogo devuelve un array de `{id, etiqueta}` (puede estar vacio si no hay datos maestros cargados, sin ser un error).
- Filtros en cascada con un id valido devuelven el subconjunto correcto (salas de esa sede, inyectores de esa sala, protocolos de ese identificador anatomico).
- Filtros sin parametro (`sedeId`/`salaId`/`identificadorAnatomicoId` ausentes) devuelven el catalogo completo sin filtrar.

## Casos negativos
- Sin JWT → 401.
- **`sedeId`/`salaId`/`identificadorAnatomicoId` inexistentes NO producen 404 ni 400** — el repositorio simplemente no encuentra filas y el endpoint responde 200 con `[]`. Este es un comportamiento verificado en el codigo (`CatalogoService`), no una suposicion: no hay ningun `orElseThrow` en estos metodos.

## Reglas de negocio
- `salas` filtra ademas por `activo = true` cuando se pasa `sedeId` (`findBySedeIdAndActivoTrue`); sin `sedeId`, devuelve `findAll()` sin filtrar por `activo` — es decir, el comportamiento "todas las salas" incluye inactivas, mientras que el filtrado por sede excluye inactivas. Es una asimetria real del codigo, documentada aqui para que no se confunda con un bug de datos.
- `protocolos` siempre filtra por `activo = true`, con o sin `identificadorAnatomicoId`.
- `agentes-contraste` siempre filtra por `activo = true`.
- `identificadores-anatomicos` NO filtra por `activo` (no existe ese campo/filtro en este metodo).
- `roles` expone directamente el nombre del enum `NombreRol` como `etiqueta` (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR).

## Codigos HTTP esperados
| Situacion | Codigo |
|---|---|
| Cualquier catalogo, con o sin resultados | 200 |
| Falta o JWT invalido | 401 |

## Ejemplos de request/response reales
`GET /api/catalogos/sedes`
```json
[
  { "id": 1, "etiqueta": "Sede Central" },
  { "id": 2, "etiqueta": "Sede Norte" }
]
```
`GET /api/catalogos/salas?sedeId=999999999`
```json
[]
```
`GET /api/catalogos/inyectores?salaId=3`
```json
[
  { "id": 7, "etiqueta": "Sala 2 - TAC — SN-EMP-00231" }
]
```

## Como ejecutar en Postman/Newman
```bash
newman run ContrastIQ.postman_collection.json \
  -e ContrastIQ.local.postman_environment.json \
  --folder "05 - Catalogos"
```
No depende de otros modulos para ejecutarse (solo requiere `accessToken` y datos maestros ya cargados).

## Pruebas de seguridad
- Confirmar 401 sin token en al menos un endpoint representativo (todos comparten la misma configuracion de seguridad global).
- Confirmar que un usuario no-ADMIN de una sede ve el catalogo COMPLETO de sedes (no solo la suya) — este es el comportamiento real, util para que el equipo de QA no lo reporte como fallo de DEF-03: DEF-03 aplica a datos clinicos, no a catalogos de filtro.

## Pruebas de integracion
- Encadenar sede → sala → inyector para construir los IDs usados en el modulo de Checklist (`salaId`) y para futuros modulos que dependan de inyector/protocolo/agente.
- Verificar que un protocolo desactivado (`activo = false`) desaparece de "Catalogo de protocolos" pero, si aplica, sigue siendo referenciable por su id en datos historicos de inyecciones ya creadas (esto ultimo no se prueba en este folder, es responsabilidad de otro modulo).

## Limpieza de datos
No aplica: estos endpoints son de solo lectura y no crean datos.

## Resultados esperados
Todas las requests del folder deberian devolver 200 siempre que exista un `accessToken` valido, independientemente de si hay datos cargados o no (arrays vacios son una respuesta valida, no un fallo).

## Problemas frecuentes
- Reportar como bug un array vacio quiere decir simplemente que faltan datos maestros — revisar migraciones/seed antes de escalar.
- Confundir la ausencia de restriccion de sede en catalogos con un fallo de DEF-03: es el comportamiento esperado segun el codigo actual.
- Usar `salaId`/`sedeId` de un entorno distinto (por ejemplo, un id que existe en staging pero no en local) produce `[]`, no error — verificar el entorno activo en Postman.

## Limitaciones
- No hay endpoints de escritura (crear/editar/eliminar) para ningun catalogo en este controller — la gestion de datos maestros esta fuera del alcance de esta suite.
- `salas` sin `sedeId` incluye salas inactivas (ver Reglas de negocio) — si el frontend depende de que "todas las salas" tambien excluya inactivas, es un gap a reportar al equipo de backend, no un defecto de esta suite de pruebas.

## Evidencias recomendadas
- Captura de un catalogo con datos y uno con `[]` para dejar constancia de que ambos son 200.
- Captura del 401 sin token.
