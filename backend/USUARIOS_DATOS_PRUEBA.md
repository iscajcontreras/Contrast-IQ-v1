# Usuarios de prueba -- seed_completo_2anios.sql

Estos son los 40 usuarios que crea `seed_completo_2anios.sql`. Todos quedan
`activo = 1` y usan la **misma contraseña para todos**: `password`

> Nota: este seed hace `TRUNCATE TABLE usuarios`, asi que el usuario
> `admin@contrast-iq.mx` original ya no existe despues de correrlo. Usa
> cualquiera de los 4 usuarios con rol ADMIN de abajo mientras tanto.

## Administradores (rol ADMIN)

| Nombre | Email | Password |
|---|---|---|
| Teresa Hernandez | teresa.hernandez@contrastiq-demo.mx | password |
| Juan Gutierrez | juan.gutierrez@contrastiq-demo.mx | password |
| Silvia Morales | silvia.morales@contrastiq-demo.mx | password |
| Hector Gonzalez | hector.gonzalez@contrastiq-demo.mx | password |

## Resto de usuarios (por rol)

### RADIOLOGO

| Nombre | Email |
|---|---|
| Raul Lopez | raul.lopez@contrastiq-demo.mx |
| Miguel Gonzalez | miguel.gonzalez@contrastiq-demo.mx |
| Paula Hernandez | paula.hernandez@contrastiq-demo.mx |
| Carmen Ortiz | carmen.ortiz@contrastiq-demo.mx |
| Sofia Gonzalez | sofia.gonzalez@contrastiq-demo.mx |
| Miguel Gutierrez | miguel.gutierrez@contrastiq-demo.mx |
| Juan Martinez | juan.martinez@contrastiq-demo.mx |
| Juan Chavez | juan.chavez@contrastiq-demo.mx |
| Hector Martinez | hector.martinez@contrastiq-demo.mx |
| Sergio Reyes | sergio.reyes@contrastiq-demo.mx |

### BIOMEDICA

| Nombre | Email |
|---|---|
| Fernando Reyes | fernando.reyes@contrastiq-demo.mx |
| Teresa Martinez | teresa.martinez@contrastiq-demo.mx |
| Maria Lopez | maria.lopez@contrastiq-demo.mx |
| Maria Reyes | maria.reyes@contrastiq-demo.mx |

### VISUALIZADOR

| Nombre | Email |
|---|---|
| Elena Torres | elena.torres@contrastiq-demo.mx |
| Beatriz Reyes | beatriz.reyes@contrastiq-demo.mx |

### TECNICO

| Nombre | Email |
|---|---|
| Francisco Ramos | francisco.ramos@contrastiq-demo.mx |
| Laura Ortiz | laura.ortiz@contrastiq-demo.mx |
| Daniela Flores | daniela.flores@contrastiq-demo.mx |
| Maria Ramos | maria.ramos@contrastiq-demo.mx |
| Rosa Sanchez | rosa.sanchez@contrastiq-demo.mx |
| Alejandra Rivera | alejandra.rivera@contrastiq-demo.mx |
| Sergio Torres | sergio.torres@contrastiq-demo.mx |
| Juan Gonzalez | juan.gonzalez@contrastiq-demo.mx |
| Oscar Gonzalez | oscar.gonzalez@contrastiq-demo.mx |
| Paula Morales | paula.morales@contrastiq-demo.mx |
| Jose Gomez | jose.gomez@contrastiq-demo.mx |
| Silvia Martinez | silvia.martinez@contrastiq-demo.mx |
| Lucia Morales | lucia.morales@contrastiq-demo.mx |
| Maria Rivera | maria.rivera@contrastiq-demo.mx |
| Carlos Gutierrez | carlos.gutierrez@contrastiq-demo.mx |
| Oscar Reyes | oscar.reyes@contrastiq-demo.mx |
| Elena Ramirez | elena.ramirez@contrastiq-demo.mx |
| Laura Ramos | laura.ramos@contrastiq-demo.mx |
| Ivan Ortiz | ivan.ortiz@contrastiq-demo.mx |
| Diego Ortiz | diego.ortiz@contrastiq-demo.mx |

## Recrear admin@contrast-iq.mx (opcional)

Si prefieres tener de vuelta el usuario admin original en vez de usar uno
de los dummy, corre esto contra la base (password_hash es el mismo hash
bcrypt de `password` que usan los 40 usuarios de prueba, se puede cambiar
despues desde la app):

```sql
USE contrast_iq;
INSERT INTO usuarios (sede_id, nombre_completo, email, password_hash, proveedor, proveedor_id, email_verificado, rol_id, activo, creado_en)
VALUES (1, 'Administrador', 'admin@contrast-iq.mx', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'LOCAL', NULL, 1, 1, 1, NOW());
```
