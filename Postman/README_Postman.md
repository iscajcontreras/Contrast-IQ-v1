# README — Suite de Pruebas Postman de ContrastIQ

> Generado el 17 de julio de 2026 contra el código fuente real de `BackEnd_ContrastIQ`
> (19 controllers REST, Spring Boot 3.5 / Java 17, MySQL `contrast_iq`). Antes de leer
> este documento, ten claro que **ContrastIQ es un monolito**, no una arquitectura de
> microservicios — ver la sección 0 para el detalle de qué se adaptó respecto a la
> plantilla original solicitada.

---

## Índice

0. [Diferencias entre la plantilla solicitada y la arquitectura real](#0-diferencias-entre-la-plantilla-solicitada-y-la-arquitectura-real)
1. [Requisitos previos](#1-requisitos-previos)
2. [Cómo importar la colección y los entornos](#2-cómo-importar-la-colección-y-los-entornos)
3. [Cómo obtener tokens (flujo de autenticación)](#3-cómo-obtener-tokens-flujo-de-autenticación)
4. [Orden de ejecución recomendado](#4-orden-de-ejecución-recomendado)
5. [Collection Runner (Postman de escritorio/web)](#5-collection-runner-postman-de-escritoriow eb)
6. [Newman (línea de comandos)](#6-newman-línea-de-comandos)
7. [Datos externos (`ContrastIQ_Test_Data.json`)](#7-datos-externos-contrastiq_test_datajson)
8. [Pruebas por rol](#8-pruebas-por-rol)
9. [Pruebas negativas](#9-pruebas-negativas)
10. [Pruebas destructivas](#10-pruebas-destructivas)
11. [Limpieza de datos (`99 - Cleanup`)](#11-limpieza-de-datos-99---cleanup)
12. [Interpretación de resultados](#12-interpretación-de-resultados)
13. [Problemas frecuentes](#13-problemas-frecuentes)
14. [Endpoints no cubiertos](#14-endpoints-no-cubiertos)
15. [Limitaciones de la suite](#15-limitaciones-de-la-suite)
16. [Riesgos](#16-riesgos)
17. [Recomendaciones de CI/CD (GitHub Actions)](#17-recomendaciones-de-cicd-github-actions)
18. [Estructura de carpetas de la colección](#18-estructura-de-carpetas-de-la-colección)
19. [Convenciones de nombres de variables](#19-convenciones-de-nombres-de-variables)
20. [Convenciones de scripts de test](#20-convenciones-de-scripts-de-test)
21. [Seguridad y manejo de secretos](#21-seguridad-y-manejo-de-secretos)
22. [Mantenimiento de la suite](#22-mantenimiento-de-la-suite)
23. [Evidencias y reportes](#23-evidencias-y-reportes)
24. [Referencias cruzadas](#24-referencias-cruzadas)

---

## 0. Diferencias entre la plantilla solicitada y la arquitectura real

La plantilla original de esta tarea asumía una arquitectura de **microservicios** con
API Gateway, multi-tenant, e integraciones clínicas estándar (HL7/FHIR/DICOM). Ninguna
de esas piezas existe en ContrastIQ. Tabla de equivalencias:

| Pedido por la plantilla | ¿Existe en ContrastIQ? | Qué se hizo en su lugar |
|---|---|---|
| API Gateway | **No** | Un solo backend Spring Boot en el puerto 8080. Todas las URLs de la colección usan `{{apiBaseUrl}}/api/...` apuntando directo al backend. |
| Microservicios separados (auth-service, patient-service, protocol-service, injector-service, inventory-service, maintenance-service, quality-service, notification-service, audit-service, integration-service, analytics-service) | **No** | 19 REST Controllers dentro de un solo proceso monolítico. La colección organiza carpetas por controller/dominio real, no por microservicio ficticio. |
| Multi-tenant (`tenantId`/`hospitalId`/`siteId`) | **No** — se confirmó que no existe ningún concepto de tenant/company en el código; había tablas `tenant_*` huérfanas ya removidas | El aislamiento real es por **sede** (`sedeId`, tabla `sedes`), aplicado server-side para roles no-ADMIN vía `UsuarioAutenticadoService.sedeIdRestriccion()` (regla DEF-03). La colección usa `sedeId`/`sedeIdOtraSede` y prueba el aislamiento por sede en `91 - Authorization Tests` y en los módulos de Pacientes/Inyecciones/Lotes/Extravasaciones. |
| Integración HL7/FHIR real | **No** | El HIS está simulado (`HisIntegracionServiceSimulado`): en realidad consulta la propia base de datos local y marca `"simulado": true` en la respuesta. No hay ningún parser HL7 ni cliente FHIR. |
| Integración DICOM | **No** | No existe absolutamente ningún código relacionado a DICOM en el repo. No se generó ninguna carpeta de pruebas DICOM. |
| Notificaciones push / API de notificaciones separada | **No** — no hay `notification-service` | Existe un broker STOMP nativo sobre WebSocket en `/ws`, publicando a `/topic/alertas` (`WebSocketConfig`, usado por `AlertaService.crear()`). Postman/Newman no prueban WebSocket de forma nativa — se documenta como limitación, no se creó ninguna request para probarlo (ver sección 15). |
| Kubernetes / Docker Compose | **No** — no hay manifiestos K8s ni `docker-compose.yml` en el repo | No se generó ninguna sección de despliegue orquestado. La sección 17 (CI/CD) propone GitHub Actions sobre el backend Spring Boot tal cual existe. |
| Health Check / Actuator (`/actuator/health`) | **No** — se confirmó por grep en `pom.xml` que `spring-boot-starter-actuator` NO está entre las dependencias | Se omitió la carpeta "00 - Health Check" de la colección. Si en el futuro se agrega Actuator, agregar esa carpeta con `GET {{apiBaseUrl}}/actuator/health`. |
| Creación manual de "procedimiento"/inyección vía API (flujo clínico con inyector en vivo) | **No** | Las inyecciones se crean **exclusivamente** por sincronización CSV automática/manual (`POST /api/integracion-clinica/sincronizar-inyector`, `SincronizacionInyectorService`). No existe ningún `POST /api/inyecciones`. La colección documenta esto explícitamente en `06 - Inyecciones` y en `modules/inyecciones.md`, y usa un `inyeccionId` de un registro ya existente (del seed o de una corrida previa de sincronización) para el resto del flujo. |
| "Reservar consumible" / "consultar costos" de inventario | **No** | Solo existen lotes de contraste (`LoteController`), pedidos de reabastecimiento (`PedidoReabastecimientoController`) y merma (`MermaController`). No hay ningún endpoint de reserva ni de costos. |
| "Crear plan preventivo" / "registrar refacciones" de mantenimiento | **No** | `MantenimientoPredictivoController` solo expone **lectura**: predicciones de falla por umbral de ciclos (constante `600` en código) y calendario de calibración (intervalo `365` días en código). No hay CRUD de planes ni de refacciones. |
| Correlation ID / trazabilidad distribuida (header `X-Correlation-Id` propagado por el backend) | **No** — no existe ningún filtro/interceptor en el código que lea o propague un header de correlación | Se dejó la variable `correlationId` únicamente como identificador de **test-run local** (para anotar tus propias evidencias/logs de la corrida de Postman). **No se envía como header real a ningún request** ni tiene ningún efecto en el backend. Ver nota en `ContrastIQ_Local.postman_environment.json`. |

**Lo que SÍ existe y se cubrió a fondo**: 19 controllers reales, JWT Bearer directo (sin
Authorization Server separado), 5 roles reales (`ADMIN`, `TECNICO`, `RADIOLOGO`,
`BIOMEDICA`, `VISUALIZADOR`), un sistema de permisos propio por módulo
(`@RequierePermiso`, tablas `modulos`/`permisos`/`rol_modulo_permiso`) que **solo se
aplica en 3 de los 19 controllers** (`AdministracionController`, `ExtravasacionController`,
`MermaController`) — el resto de los controllers restringidos lo están por
`@PreAuthorize("hasRole('ADMIN')")` a nivel de clase o método
(`UsuarioController`, `AdministracionController`, `HistorialAccesoController`), y **todos
los demás controllers solo exigen autenticación**, sin restricción de rol adicional. Esta
asimetría es un hallazgo real del código, documentado en `91 - Authorization Tests` y en
cada `modules/*.md` correspondiente — no es un defecto de esta suite.

---

## 1. Requisitos previos

- **Postman** de escritorio o web, versión reciente (soporte de Collection Schema v2.1 —
  cualquier versión desde 2021 en adelante sirve).
- **Node.js 18+** si vas a correr **Newman** por línea de comandos.
- El backend `BackEnd_ContrastIQ` corriendo localmente (`mvn spring-boot:run` o el jar
  empacado) escuchando en `http://localhost:8080` (confirmado en
  `src/main/resources/application.properties`: `server.port=8080`).
- MySQL con el esquema `contrast_iq` cargado. Idealmente con el seed de 40 usuarios de
  prueba documentado en `BackEnd_ContrastIQ/USUARIOS_DATOS_PRUEBA.md`
  (`seed_completo_2anios.sql`) — sin ese seed, varios `GET` de listados devolverán
  colecciones vacías (no es un error, es comportamiento esperado sin datos).
- Si vas a correr `13 - Integracion Clinica > Sincronizar Inyector`, necesitas al menos
  un archivo `.csv` válido con el formato que espera `SincronizacionInyectorService` en la
  carpeta configurada por `app.sincronizacion.carpeta` (por defecto
  `./importaciones-iris`, relativo al directorio de trabajo del backend) — **no** es un
  parámetro HTTP del request, es un archivo del sistema de archivos del servidor (ver
  `modules/integracion-clinica.md`, sección Limitaciones).

## 2. Cómo importar la colección y los entornos

1. Abre Postman → **Import** → arrastra o selecciona:
   - `ContrastIQ_API.postman_collection.json`
   - `ContrastIQ_Local.postman_environment.json`
   - `ContrastIQ_QA.postman_environment.json`
   - `ContrastIQ_Production_Template.postman_environment.json`
2. Selecciona el entorno **"ContrastIQ - Local"** en el selector superior derecho antes de
   correr cualquier request contra tu backend local.
3. Verifica que `apiBaseUrl` en el entorno activo sea `http://localhost:8080` (SIN `/api`
   al final — todas las requests de la colección ya agregan `/api/...` en su URL).

## 3. Cómo obtener tokens (flujo de autenticación)

1. Corre la carpeta **"01 - Auth"** completa una vez al inicio de cada sesión de pruebas.
   Las 5 requests de login por rol (`Login - ADMIN`, `Login - TECNICO`, `Login -
   RADIOLOGO`, `Login - BIOMEDICA`, `Login - VISUALIZADOR`) guardan automáticamente el
   `accessToken` de cada rol en su propia variable de entorno (`adminToken`,
   `tecnicoToken`, `radiologoToken`, `biomedicaToken`, `visualizadorToken`) vía
   `pm.environment.set(...)` en el script de test.
2. `Login - Generico` además guarda `accessToken`/`refreshToken` "genéricos" (el último
   login corrido), usados por defecto en la auth Bearer a nivel de colección.
3. Los tokens vencen en 3600 segundos (`app.jwt.access-token-minutos=60`) — si una corrida
   larga falla con 401 a mitad de camino, vuelve a correr `01 - Auth` antes de continuar.
4. El **refresh token es de un solo uso** (`AuthService.refrescar` lo revoca y emite uno
   nuevo) — no reutilices el mismo `refreshToken` dos veces en la carpeta `Refresh`.

## 4. Orden de ejecución recomendado

1. `01 - Auth` (obtener todos los tokens)
2. `05 - Catalogos` (para llenar `sedeId`, `salaId`, `inyectorId`, `protocoloId`,
   `agenteId`/`agenteContrasteId`, `identificadorAnatomicoId` a mano en el entorno, o
   leerlos de las respuestas)
3. `03 - Pacientes` → guarda `pacienteId`
4. `04 - Checklist Pre-Inyeccion` (usa `pacienteId`) → guarda `checklistId`
5. `13 - Integracion Clinica` (opcional: sincronizar para generar inyecciones nuevas) →
   de lo contrario usa un `inyeccionId` existente del seed
6. `06 - Inyecciones`, `07 - Dashboard Operativo`, `11 - Extravasaciones`, `12 - Alertas`
7. `08 - Lotes y Mermas`, `09 - Pedidos de Reabastecimiento`, `10 - Mantenimiento y
   Tickets`, `14 - Reportes`
8. `02 - Usuarios, Roles y Permisos`, `15 - Historial de Accesos` (requieren `adminToken`)
9. `90 - Negative Tests`, `91 - Authorization Tests`, `92 - Validation Tests`
10. `99 - Cleanup` (solo si `enableCleanup=true`)

## 5. Collection Runner (Postman de escritorio/web)

1. Click derecho sobre la carpeta raíz **"Contrast>IQ API"** → **Run collection**.
2. Selecciona el entorno "ContrastIQ - Local".
3. Marca "Save responses" si quieres adjuntar evidencia luego.
4. Respeta el orden de carpetas (el Runner por defecto sigue el orden de la colección,
   que ya está numerado 01→99 para esto).
5. Si vas a correr solo un subconjunto (por ejemplo, solo pruebas de un rol), deselecciona
   las demás carpetas en el panel de selección del Runner.

## 6. Newman (línea de comandos)

Instalación:

```bash
npm install -g newman
npm install -g newman-reporter-htmlextra   # reporter HTML enriquecido (opcional pero recomendado)
```

Ejecución básica contra el entorno local, con reporters `cli` + `junit` + `html`:

```bash
newman run ContrastIQ_API.postman_collection.json \
  -e ContrastIQ_Local.postman_environment.json \
  --reporters cli,junit,htmlextra \
  --reporter-junit-export ./resultados/contrastiq-junit.xml \
  --reporter-htmlextra-export ./resultados/contrastiq-report.html \
  --insecure
```

Notas:
- `--insecure` solo es necesario si tu backend local usa TLS autofirmado; contra
  `http://localhost:8080` en texto plano no hace falta.
- El reporter `junit` es el que consume Jenkins/GitHub Actions para mostrar
  pass/fail por test en la interfaz de CI.
- Para excluir `99 - Cleanup` en una corrida de solo verificación (por ejemplo en un
  ambiente que no quieres tocar), usa `--folder` repetido con cada carpeta que sí quieras
  correr, o usa `enableCleanup=false` en el archivo de entorno pasado con `-e`.
- Para correr un solo módulo: `newman run ... --folder "08 - Lotes y Mermas"`.

## 7. Datos externos (`ContrastIQ_Test_Data.json`)

Este archivo NO se importa directo en Postman como colección ni como entorno — es un
documento de referencia con datos sintéticos organizados por sección (pacientes válidos,
datos límite, usuarios por rol, lotes vigentes/caducados, agentes de contraste,
protocolos, checklists válidos/inválidos). Úsalo para:
- Copiar bodies de ejemplo cuando necesites variar un caso a mano.
- Alimentar un script externo (por ejemplo con `newman run -d` usando un CSV/JSON de
  datos, si conviertes las secciones relevantes a un array plano para
  data-driven testing).

## 8. Pruebas por rol

Cada carpeta de módulo indica en su `modules/*.md` correspondiente (sección "Roles
permitidos") qué roles están autorizados para cada endpoint, verificado leyendo
`@PreAuthorize`/`@RequierePermiso` línea por línea en el controller real — no asumido.
La carpeta `91 - Authorization Tests` centraliza la matriz completa rol × endpoint
restringido en un solo lugar para smoke-testing rápido de autorización.

## 9. Pruebas negativas

Cada carpeta de módulo incluye, junto a cada caso positivo, los casos negativos que
**sí aplican según las validaciones/roles reales de ese endpoint específico** — no se
rellenó con negativos genéricos copiados. La carpeta `90 - Negative Tests` cubre además
casos transversales que no pertenecen a un módulo en particular (JSON malformado,
Content-Type incorrecto, método HTTP no soportado, ruta inexistente, path variable no
numérica, header Authorization mal formado). `92 - Validation Tests` agrupa un smoke test
rápido de Bean Validation (`@NotBlank`/`@NotNull`/`@Email`/`@Size`/`@AssertTrue`) sin
tener que correr la suite completa.

## 10. Pruebas destructivas

Las requests que crean, modifican o desactivan datos (alta de usuario, alta de lote,
alta de ticket, cambios de estado de pedido, resolución de alerta, etc.) están marcadas
en su descripción como "destructiva" cuando corresponde y se recomienda gatearlas con la
variable `enableDestructiveTests`. En el entorno de Producción (plantilla) esta variable
queda fija en `false` — revisa la sección 0 de `ContrastIQ_Production_Template.postman_environment.json`
antes de siquiera considerar correr esta colección contra un backend con datos clínicos
reales.

## 11. Limpieza de datos (`99 - Cleanup`)

La carpeta `99 - Cleanup` corre al final de la colección y revierte (cuando es posible)
los efectos de las requests destructivas: desactiva el usuario de prueba creado, cierra
el ticket de soporte creado, resuelve la alerta manual creada, cancela el pedido de
reabastecimiento de prueba, y cierra la sesión (`logout`) de las corridas de auth. Cada
request de esta carpeta usa un script `prerequest` que hace `postman.setNextRequest(null)`
si `enableCleanup` no es exactamente `"true"`, de modo que no se ejecuta nada
destructivo por accidente. **No existe limpieza para pacientes, checklists, lotes o
inyecciones creados**, porque el backend no expone ningún `DELETE` para esas entidades —
queda documentado como limitación real (ver sección 15), no como un olvido de la suite.

## 12. Interpretación de resultados

- Cada request valida `status code` esperado y, cuando aplica, `Content-Type` y forma del
  JSON de respuesta contra el DTO real (nombres de campo exactos, tipos básicos).
- Un test fallando en `pm.response.code` casi siempre significa: (a) el backend no está
  corriendo, (b) el token expiró, (c) el dato de referencia (`{{pacienteId}}`, etc.) no
  existe en tu base — revisa primero estas tres causas antes de asumir un defecto real.
- El reporte `junit.xml` de Newman es el insumo estándar para dashboards de CI; el reporte
  `htmlextra` es más legible para revisión manual humana.

## 13. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| Todo devuelve 401 | Token vencido o entorno equivocado seleccionado | Vuelve a correr `01 - Auth`; confirma el entorno activo en Postman |
| `GET /api/pacientes` devuelve `content: []` | No hay seed cargado en tu MySQL local | Carga `USUARIOS_DATOS_PRUEBA.md` / seed correspondiente, o usa Integración Clínica para sincronizar datos |
| `POST /api/checklists` siempre 400 | Alguna de las 4 banderas de seguridad quedó en `false` en el body | Revisa que `identidadVerificada`, `gfrRevisado`, `alergiasRevisadas`, `consentimientoFirmado` estén en `true` para el caso positivo |
| `POST /api/integracion-clinica/sincronizar-inyector` no crea nada nuevo | No hay archivos `.csv` nuevos en la carpeta configurada del servidor | Revisa `app.sincronizacion.carpeta` en `application.properties` del backend y coloca un CSV de prueba ahí antes de llamar al endpoint |
| Newman falla con `ENOTFOUND localhost` en un contenedor Docker | El backend corre en el host, no dentro del contenedor de Newman | Usa `host.docker.internal` en `apiBaseUrl` si corres Newman dentro de Docker apuntando a un backend en el host |
| `403` inesperado en un endpoint que "debería" estar abierto a todos los roles | Falsa expectativa de que todo tiene `@RequierePermiso` | Revisa la sección 0 de este README y `91 - Authorization Tests`: la mayoría de los controllers SOLO exige autenticación, sin restricción de rol — un 403 real ahí normalmente significa que sí estás pegándole a uno de los 5 endpoints que sí restringen (`UsuarioController`, `AdministracionController`, `HistorialAccesoController`, mutación de `ExtravasacionController`, 4 de 5 endpoints de `MermaController`) |

## 14. Endpoints no cubiertos

Ver la tabla de cobertura al final de la respuesta del agente que generó esta suite (o
pídela de nuevo con "dame la tabla de cobertura de ContrastIQ"). Resumen: **todos los
endpoints reales identificados en los 19 controllers quedaron representados** con al
menos un caso positivo en la colección. Donde un endpoint no tuvo casos negativos, fue
porque el código no expone ninguna validación/restricción adicional aplicable (por
ejemplo, catálogos de solo lectura sin filtros obligatorios).

## 15. Limitaciones de la suite

- **WebSocket (`/ws`, topic `/topic/alertas`)**: no probado por Postman/Newman — requiere
  un cliente STOMP dedicado (fuera del alcance de una colección HTTP).
- **`POST /api/checklists` → `restablecer-password` (caso positivo real)**: no es
  100% automatizable de punta a punta sin acceso a un correo real, porque el token de
  recuperación se genera server-side y no se expone por ningún endpoint de lectura
  (`olvidar-password` responde con un mensaje genérico a propósito). El request de
  `restablecer-password` positivo en la colección usa `pm.test.skip()`/comentarios
  explicando esta limitación.
- **`POST /api/integracion-clinica/sincronizar-inyector`**: depende de archivos `.csv`
  en el sistema de archivos del servidor, no de un body HTTP — no se puede parametrizar
  completamente desde Postman sin acceso al filesystem del backend.
- **Sin `DELETE` real en la mayoría de las entidades**: pacientes, checklists, lotes,
  inyecciones, agentes de contraste, protocolos no tienen endpoint de borrado — por
  diseño (trazabilidad clínica/regulatoria). La limpieza (sección 11) hace lo que puede
  con los `PATCH`/cambios de estado disponibles.
- **No hay ambiente QA real**: `ContrastIQ_QA.postman_environment.json` es un placeholder.
- **No se generó Actuator/Health Check**: confirmado que la dependencia no está en
  `pom.xml`.

## 16. Riesgos

- Correr `92 - Validation Tests` o `90 - Negative Tests` de login en loop repetido puede
  **bloquear cuentas reales** por el mecanismo de bloqueo tras varios intentos fallidos
  (`historial_accesos` + `bloqueado_hasta` en `Usuario`, ver `AuthService`) — evita correr
  la colección completa muchas veces seguidas contra el mismo usuario en poco tiempo.
- Los datos clínicos simulados (pacientes, checklists) quedan persistidos en la base tras
  cada corrida — en un ambiente compartido, coordina con el equipo antes de correr pruebas
  destructivas.
- `enableDestructiveTests`/`enableCleanup` en `true` sobre un ambiente compartido pueden
  interferir con otras pruebas manuales en curso.

## 17. Recomendaciones CI/CD (GitHub Actions)

El repo no tiene todavía ningún workflow de GitHub Actions (`.github/workflows` no existe
en ninguno de los dos proyectos, backend ni frontend, al momento de generar esta suite).
Se recomienda agregar uno para correr esta colección automáticamente. Ejemplo de
workflow (`(.github/workflows/postman-tests.yml)`, a crear cuando el equipo lo apruebe):

```yaml
name: API Tests - ContrastIQ (Postman/Newman)

on:
  pull_request:
    paths:
      - "backend/BackEnd_ContrastIQ/**"
  workflow_dispatch:

jobs:
  postman-tests:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: contrast_iq
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - name: Cargar seed de MySQL
        run: mysql -h127.0.0.1 -uroot -proot contrast_iq < backend/BackEnd_ContrastIQ/schema_contrastiq_completo.sql
      - name: Levantar backend en background
        working-directory: backend/BackEnd_ContrastIQ
        run: |
          nohup mvn spring-boot:run &
          npx wait-on http://localhost:8080/api/catalogos/roles -t 60000
      - uses: actions/setup-node@v4
        with:
          node-version: "20"
      - run: npm install -g newman newman-reporter-htmlextra
      - name: Correr suite Postman
        working-directory: postman
        run: |
          newman run ContrastIQ_API.postman_collection.json \
            -e ContrastIQ_Local.postman_environment.json \
            --reporters cli,junit,htmlextra \
            --reporter-junit-export ../resultados/contrastiq-junit.xml \
            --reporter-htmlextra-export ../resultados/contrastiq-report.html
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: reporte-postman
          path: resultados/
```

Ajustes a considerar: el workflow de ejemplo asume que `wait-on` y un endpoint público
(`/api/catalogos/roles`, sin auth requerida más allá de un JWT válido — ajústalo si
decides exponer un healthcheck real) están disponibles para esperar el arranque; sin
Actuator, usa el catálogo más simple disponible o agrega un endpoint de salud dedicado.

## 18. Estructura de carpetas de la colección

```
Contrast>IQ API
├── 01 - Auth
├── 02 - Usuarios, Roles y Permisos
│   ├── Usuarios
│   ├── Permisos (me)
│   └── Administracion (Roles y Permisos)
├── 03 - Pacientes
├── 04 - Checklist Pre-Inyeccion
├── 05 - Catalogos
├── 06 - Inyecciones
├── 07 - Dashboard Operativo
├── 08 - Lotes y Mermas
│   ├── Lotes
│   └── Mermas
├── 09 - Pedidos de Reabastecimiento
├── 10 - Mantenimiento y Tickets
│   ├── Mantenimiento Predictivo
│   └── Tickets de Soporte
├── 11 - Extravasaciones
├── 12 - Alertas
├── 13 - Integracion Clinica
├── 14 - Reportes
├── 15 - Historial de Accesos
├── 90 - Negative Tests
├── 91 - Authorization Tests
├── 92 - Validation Tests
└── 99 - Cleanup
```

No existe carpeta "00 - Health Check" (ver sección 0).

## 19. Convenciones de nombres de variables

Ver la lista completa y comentada en `ContrastIQ_Local.postman_environment.json`
(`_postman_notes`). En resumen: `apiBaseUrl` sin `/api`; un token por rol
(`adminToken`, `tecnicoToken`, `radiologoToken`, `biomedicaToken`, `visualizadorToken`)
más `accessToken`/`refreshToken` "genéricos"; un `...Id` por cada entidad clave del
dominio real (`sedeId`, `salaId`, `inyectorId`, `pacienteId`, `inyeccionId`,
`checklistId`, `loteId`, `agenteContrasteId`, `protocoloId`,
`identificadorAnatomicoId`, `pedidoId`, `ticketId`, `alertaId`,
`eventoExtravasacionId`, `usuarioId`, `rolId`, `moduloId`, `permisoId`); variantes
`...OtraSede` para pruebas de aislamiento DEF-03; `randomEmail`/`randomCode`/`timestamp`
generados por el script `prerequest` de la colección; `enableCleanup`/
`enableDestructiveTests` como flags booleanos en texto (`"true"`/`"false"`).

## 20. Convenciones de scripts de test

Todas las requests usan `event: [{ listen: "test", script: { exec: [...] } }]` (formato
Postman v2.1 real, no la clave legacy `tests`). Patrón común:

```javascript
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});
pm.test('Content-Type es JSON', function () {
    pm.expect(pm.response.headers.get('Content-Type')).to.include('application/json');
});
pm.test('Estructura de respuesta valida', function () {
    const body = pm.response.json();
    pm.expect(body).to.have.property('id');
    // ... resto de campos segun el DTO real
});
if (pm.response.code === 200 || pm.response.code === 201) {
    pm.environment.set('algoId', pm.response.json().id);
}
```

## 21. Seguridad y manejo de secretos

- Ningún archivo de esta suite contiene contraseñas o tokens productivos reales.
- `ContrastIQ_Local.postman_environment.json` sí contiene las contraseñas del seed de
  desarrollo (`password`, documentadas públicamente en
  `BackEnd_ContrastIQ/USUARIOS_DATOS_PRUEBA.md`) — **no la uses fuera de tu máquina
  local/desarrollo**.
- `ContrastIQ_QA.postman_environment.json` y `ContrastIQ_Production_Template.postman_environment.json`
  usan `CHANGE_ME` en todos los campos sensibles.
- Todas las variables de token están marcadas `"type": "secret"` en los archivos de
  entorno, para que Postman las enmascare en la UI.

## 22. Mantenimiento de la suite

Si el backend cambia (nuevo endpoint, DTO modificado, nueva regla `@RequierePermiso`):
1. Actualiza el controller/DTO real primero.
2. Actualiza la request correspondiente en `ContrastIQ_API.postman_collection.json`
   (nombre de campo, validación, o restricción de rol).
3. Actualiza el `modules/*.md` correspondiente (la sección "Endpoints" debe seguir
   listando el nombre exacto de la request).
4. Revalida el JSON con `python3 -c "import json; json.load(open('ContrastIQ_API.postman_collection.json'))"`.

## 23. Evidencias recomendadas

Guarda, por corrida: el `contrastiq-junit.xml` y `contrastiq-report.html` de Newman (o el
export de resultados del Collection Runner), la versión de commit del backend contra la
que corriste, y el nombre del entorno usado. Cada `modules/*.md` tiene además su propia
sección "Evidencias recomendadas" con capturas/checks específicos de ese módulo (por
ejemplo, para `checklist-pre-inyeccion.md`: captura del 400 con las 4 banderas en false).

## 24. Referencias cruzadas

- Documentación SQL real por controller: `../backend/BackEnd_ContrastIQ/All_Sql_Statement.MD`
- Usuarios/contraseñas de prueba: `../backend/BackEnd_ContrastIQ/USUARIOS_DATOS_PRUEBA.md`
- Documentos por módulo de esta suite: `modules/*.md`
- Contexto general del proyecto (doc del Project de Claude): `CONTEXTO_PROYECTO.md`,
  `PROXIMOS_PASOS.md`, `PREGUNTAS_PRIORIDAD_3.md`
