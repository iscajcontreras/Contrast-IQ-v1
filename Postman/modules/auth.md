# Modulo: Auth (`/api/auth`, `/api/me` parcial)

## Objetivo
Cubrir con pruebas Postman/Newman todo el ciclo de autenticacion del backend ContrastIQ: login directo con JWT Bearer, renovacion de sesion (refresh), cierre de sesion (logout), alta de cuentas (registro), y el flujo de recuperacion de contrasena (olvidar/restablecer). Tambien cubre `GET /api/auth/me`, que expone los datos basicos del usuario autenticado.

## Responsabilidades
`AuthController` (`src/main/java/com/contrastiq/backend/controller/AuthController.java`) delega toda la logica en `AuthService`. Es un monolito de un solo puerto (8080): no hay Authorization Server separado, no hay redirects OAuth, no hay PKCE ni doble pantalla de login. El access token es un JWT RS256 firmado en el propio backend (`JwtSecurityConfig`); el refresh token es un valor opaco (UUID) persistido en la tabla `tokens_refresco`, lo que permite revocarlo.

## URL base
`{{apiBaseUrl}}/api/auth` (mas `{{apiBaseUrl}}/api/auth/me` como caso especial que si exige token).

## Dependencias
- Ninguna dependencia de otros modulos para los endpoints publicos (login, refresh, logout, registro, olvidar/restablecer password).
- `GET /api/auth/me` requiere un token valido obtenido de `POST /api/auth/login`.
- El modulo "Usuarios, Roles y Permisos" depende de este modulo: casi todas sus requests usan `{{adminToken}}`, `{{tecnicoToken}}`, etc. generados aqui.

## Autenticacion
- Publicos (whitelist en `ResourceServerConfig`, `auth: {type: "noauth"}` en la coleccion): `POST /login`, `POST /refresh`, `POST /logout`, `POST /registro`, `POST /olvidar-password`, `POST /restablecer-password`.
- Protegido: `GET /me` exige `Authorization: Bearer <accessToken>`.

## Roles permitidos
Todos los endpoints publicos no distinguen rol (no hay usuario autenticado todavia). `GET /me` es accesible para cualquiera de los 5 roles (ADMIN, TECNICO, RADIOLOGO, BIOMEDICA, VISUALIZADOR): solo requiere que el JWT sea valido, no evalua rol.

## Endpoints

| Metodo | Path | DTO request | DTO response | Request Postman equivalente |
|---|---|---|---|---|
| POST | /api/auth/login | `LoginRequest{email,password}` | `TokenResponse{accessToken,refreshToken,expiresIn}` | "Login - Generico (guarda accessToken y refreshToken)", "Login - ADMIN (Teresa Hernandez)", "Login - TECNICO (Francisco Ramos)", "Login - RADIOLOGO (Raul Lopez)", "Login - BIOMEDICA (Fernando Reyes)", "Login - VISUALIZADOR (Elena Torres)" |
| POST | /api/auth/refresh | `RefrescarTokenRequest{refreshToken}` | `TokenResponse` | "Refresh Token - Positivo" |
| POST | /api/auth/logout | `RefrescarTokenRequest{refreshToken}` | vacio (204) | "Logout - Positivo (204)" |
| POST | /api/auth/registro | `RegistroRequest{nombreCompleto,email,password}` | `{mensaje}` (201) | "Registro - Positivo (201)" |
| POST | /api/auth/olvidar-password | `OlvidarPasswordRequest{email}` | `{mensaje}` (200 siempre) | "Olvidar Password - Positivo (200, respuesta generica)" |
| POST | /api/auth/restablecer-password | `RestablecerPasswordRequest{token,nuevaPassword}` | `{mensaje}` (200) | "Restablecer Password - Positivo (NO automatizable end-to-end)" |
| GET | /api/auth/me | (Bearer) | `UsuarioActualDTO{nombreCompleto,email,rol}` | "GET Me - Positivo (200)" |

## Variables necesarias
- `apiBaseUrl` = `http://localhost:8080` (las requests usan `{{apiBaseUrl}}/api/...`).
- `randomEmail`: debe generarse por corrida (recomendado en un pre-request script a nivel de coleccion: `pm.collectionVariables.set('randomEmail', \`qa.${Date.now()}@contrastiq-demo.mx\`)`), para que "Registro - Positivo" no choque con un email ya usado en corridas previas.
- Variables que este modulo **produce** y que otros modulos consumen: `accessToken`, `refreshToken`, `adminToken`, `tecnicoToken`, `radiologoToken`, `biomedicaToken`, `visualizadorToken` (y sus `*RefreshToken` correspondientes).
- `refreshTokenUsado`: la usa el caso negativo de refresh "token ya usado".
- `tokenRecuperacionPassword`: variable manual (no automatizable, ver Limitaciones) para el caso positivo de restablecer password.

## Datos de prueba
Password de todos los usuarios semilla es literalmente `password`.

| Rol | Email |
|---|---|
| ADMIN | teresa.hernandez@contrastiq-demo.mx (usado en la coleccion; tambien existen juan.gutierrez, silvia.morales, hector.gonzalez) |
| TECNICO | francisco.ramos@contrastiq-demo.mx |
| RADIOLOGO | raul.lopez@contrastiq-demo.mx |
| BIOMEDICA | fernando.reyes@contrastiq-demo.mx |
| VISUALIZADOR | elena.torres@contrastiq-demo.mx |

## Orden recomendado de ejecucion
1. "Login - Generico" (deja `accessToken`/`refreshToken`).
2. Los 5 logins por rol ("Login - ADMIN", "Login - TECNICO", "Login - RADIOLOGO", "Login - BIOMEDICA", "Login - VISUALIZADOR") — necesarios antes de correr cualquier otro modulo que use esos tokens.
3. Casos negativos de login (no dependen de nada, pero se recomienda correrlos despues de los positivos para no disparar el bloqueo de cuenta por intentos fallidos antes de necesitar el login real).
4. "Refresh Token - Positivo" → luego "Refresh Token - Negativo - Token ya usado/revocado" (depende del anterior) → "Refresh Token - Negativo - Token invalido/inexistente" (independiente).
5. "Logout - Positivo" (consume `refreshToken`; correrlo casi al final si otras requests del modulo siguen necesitando esa sesion).
6. "Registro - *" (positivo y negativos), "Olvidar Password - *", "Restablecer Password - *".
7. "GET Me - *".
8. "Autorizacion - Negativo - Rol incorrecto (VISUALIZADOR)..." al final (requiere `visualizadorToken` ya generado).

## Casos positivos
- Login exitoso por cada uno de los 5 roles, validando forma de `TokenResponse` y `expiresIn == 3600`.
- Refresh exitoso con rotacion del refresh token (el nuevo es distinto al enviado).
- Logout exitoso (204, cuerpo vacio).
- Registro exitoso (201, rol por defecto VISUALIZADOR).
- Olvidar password (200 con mensaje generico, exista o no la cuenta).
- GET /me devolviendo `nombreCompleto`, `email`, `rol` reales del usuario autenticado.

## Casos negativos
- Login: password vacio (400, `@NotBlank`), email inexistente (401), password incorrecto (401) — ambos con el mismo mensaje "Correo o contrasena incorrectos" para no filtrar existencia de cuenta.
- Refresh: token ya usado/revocado (401 "Refresh token revocado"), token inexistente (401 "Refresh token invalido").
- Logout: `refreshToken` vacio (400 `@NotBlank`).
- Registro: email duplicado (400), password <8 caracteres (400), email con formato invalido (400), los 3 campos obligatorios vacios a la vez (400).
- Olvidar password: email con formato invalido (400, valida antes de tocar el service).
- Restablecer password: token invalido (400 "El enlace de recuperacion no es valido"), nuevaPassword <8 caracteres (400, se valida antes que el token).
- GET /me: sin token (401), token manipulado/con firma invalida (401).
- Cruce de autorizacion: token valido de rol VISUALIZADOR contra un endpoint solo-ADMIN (`GET /api/usuarios`) → 403, para diferenciar "no autenticado" (401) de "autenticado pero sin permiso" (403).

## Reglas de negocio relevantes
- **Bloqueo por fuerza bruta (Fix DEF-01, QA julio 2026):** 5 intentos fallidos consecutivos dentro de una ventana de 15 minutos bloquean la cuenta 15 minutos (`AuthService.MAX_INTENTOS_FALLIDOS/VENTANA_INTENTOS_MINUTOS/BLOQUEO_MINUTOS`). El bloqueo solo aplica si el email corresponde a una cuenta real. **Cuidado al automatizar en Newman**: correr el caso "Login - Negativo - Password incorrecto" en loop puede bloquear la cuenta de pruebas.
- **Rotacion de refresh token:** cada `POST /refresh` revoca el token usado y emite uno nuevo — no es posible refrescar dos veces con el mismo refresh token.
- **No enumeracion de cuentas:** `/olvidar-password` siempre responde 200 con el mismo mensaje, exista o no el correo.
- **Registro con rol fijo:** todo alta via `/registro` recibe el rol VISUALIZADOR (solo lectura) hasta que un ADMIN lo cambie via `PUT /api/usuarios/{id}`.
- El JWT lleva el claim `roles` (con prefijo `ROLE_`) — sin este claim, ningun `@PreAuthorize(hasRole(...))` funcionaria (bug historico corregido, ver comentario en `AuthService.emitirTokens()`).

## Codigos HTTP esperados

| Escenario | Status |
|---|---|
| Login/refresh exitoso | 200 |
| Logout exitoso | 204 |
| Registro exitoso | 201 |
| Olvidar/restablecer password exitoso | 200 |
| GET /me exitoso | 200 |
| Validacion de campos (`@NotBlank`, `@Email`, `@Size`) | 400 |
| Email duplicado en registro / token de recuperacion invalido o usado | 400 |
| Credenciales invalidas / refresh token invalido, expirado o revocado | 401 |
| Sin token o token invalido/manipulado en endpoint protegido | 401 |
| Token valido pero rol insuficiente (endpoint de otro modulo) | 403 |

## Ejemplos de request/response reales

**POST /api/auth/login** (request)
```json
{
    "email": "teresa.hernandez@contrastiq-demo.mx",
    "password": "password"
}
```
Response 200:
```json
{
    "accessToken": "eyJhbGciOiJSUzI1NiJ9....",
    "refreshToken": "6c9e1c1a-....-....-....-............",
    "expiresIn": 3600
}
```

**GET /api/auth/me** (response 200)
```json
{
    "nombreCompleto": "Teresa Hernandez",
    "email": "teresa.hernandez@contrastiq-demo.mx",
    "rol": "ADMIN"
}
```

**Error de validacion (400)** — ejemplo login con password vacio:
```json
{
    "timestamp": "2026-07-17T10:15:00.123",
    "mensaje": "Datos invalidos",
    "errores": {
        "password": "La contrasena es obligatoria"
    }
}
```

## Como ejecutar en Postman/Newman
1. Importar la coleccion completa (con este fragmento `01 - Auth` como una de sus carpetas de primer nivel) y el environment con `apiBaseUrl=http://localhost:8080`.
2. Definir un pre-request script de coleccion que genere `randomEmail` unico por corrida.
3. Levantar el backend local (`./mvnw spring-boot:run` o equivalente) en el puerto 8080 con la BD semilla cargada.
4. Ejecutar la carpeta "01 - Auth" completa, en orden (Postman respeta el orden de la carpeta salvo que se reordene manualmente).
5. Con Newman: `newman run coleccion.json -e environment.json --folder "01 - Auth"`.

## Pruebas de seguridad
- Verificar que un JWT con la firma alterada (un caracter cambiado) sea rechazado con 401 antes de llegar al controller.
- Verificar que un token valido de un rol sin privilegios (VISUALIZADOR) sea rechazado con 403, no 401, al golpear un endpoint restringido — confirma que la cadena de autenticacion/autorizacion distingue ambos casos.
- Confirmar que `/olvidar-password` no revela si el correo existe (mismo mensaje siempre).
- Confirmar que las respuestas de error de login no distinguen "email no existe" de "password incorrecta".

## Pruebas de integracion
- El flujo login → refresh → logout debe encadenarse correctamente usando variables de entorno (`refreshToken` se sobreescribe en cada paso).
- El flujo registro → login con el nuevo usuario (rol VISUALIZADOR) → `GET /me` confirma el rol asignado por defecto.
- Los 5 logins por rol alimentan directamente el resto de la suite (modulo "Usuarios, Roles y Permisos" e "Historial de Accesos").

## Limpieza de datos
- Cada corrida de "Registro - Positivo" crea un usuario nuevo (gracias a `{{randomEmail}}`) que queda persistido; no hay endpoint de auto-borrado en `AuthController`. La limpieza de estos usuarios de prueba debe hacerse manualmente o via `DELETE`/`PATCH estado` desde el modulo Usuarios (si existiera un borrado; hoy solo hay "cambiar estado" a inactivo) o directamente en BD entre corridas de CI.
- Los refresh tokens emitidos en cada corrida quedan en `tokens_refresco` (revocados o no); no afectan la logica de negocio pero acumulan filas — limpieza periodica recomendada en entornos de prueba de larga duracion.

## Resultados esperados
Todos los tests de la carpeta deben pasar en verde en una BD semilla intacta y un backend recien levantado, **excepto** que:
- Si se corre la suite completa varias veces sin reiniciar la BD, "Registro - Negativo - Email duplicado" seguira pasando (usa un email fijo de la semilla), pero si se reordena y se corre "Login - Negativo - Password incorrecto" repetidamente, puede activarse el bloqueo de cuenta y hacer fallar temporalmente los logins subsecuentes con ese email.
- "Restablecer Password - Positivo" se marca con `pm.test.skip(...)` a menos que se provea manualmente `{{tokenRecuperacionPassword}}`.

## Problemas frecuentes
- **401 inesperado en requests "protegidas" de otros modulos:** casi siempre por no haber corrido antes el login del rol correspondiente en esta carpeta.
- **400 en "Registro - Positivo" en la segunda corrida:** `randomEmail` no se regenero (revisar el pre-request script de coleccion).
- **Bloqueo de cuenta ADMIN tras pruebas fallidas repetidas:** esperar 15 minutos o limpiar `bloqueado_hasta` en BD.
- **expiresIn distinto de 3600:** revisar que `app.jwt.access-token-minutos` en `application.properties` no se haya modificado (por defecto 60).

## Limitaciones
- El caso positivo de "Restablecer Password" no es automatizable sin acceso al correo real o a la tabla `tokens_recuperacion_password`, porque `/olvidar-password` nunca expone el token en la respuesta HTTP (medida de seguridad deliberada). Se documenta y se deja con `pm.test.skip` condicional.
- No se confirmo el mecanismo de envio real de correos (`EmailService`) en este analisis; se asume que en ambiente local puede estar mockeado/loggeado en vez de enviar correo real.

## Evidencias recomendadas
- Capturas de los 5 `TokenResponse` (uno por rol) con `expiresIn=3600`.
- Captura del cuerpo de error 401 de "Login - Negativo - Password incorrecto" mostrando el mensaje generico.
- Log de Newman (`--reporters cli,json`) de la corrida completa de la carpeta como evidencia de regresion.
