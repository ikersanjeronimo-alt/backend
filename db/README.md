# Base de datos — ShareYourStory

Esta carpeta agrupa todos los artefactos de **base de datos** del proyecto: modelo
E/R, gestión de usuarios y permisos del SGBD, copias de seguridad y restauración,
procedimientos almacenados, triggers y replicación.

El SGBD es **MySQL** (ver `../.devcontainer/compose.yml`). La aplicación accede vía
**Spring Data JPA / Hibernate** (`spring.jpa.hibernate.ddl-auto=update`), por lo que
Hibernate genera y mantiene las **tablas** a partir de las entidades `@Entity`.
Los objetos que Hibernate **no** gestiona (usuarios, `GRANT`, procedimientos,
triggers, replicación) se definen aquí como scripts SQL versionados.

## Contenido

| Archivo | Descripción | Nivel rúbrica |
|---|---|---|
| `01-modelo-er.md` | Modelo Entidad-Relación del esquema actual | 1 |
| `02-usuarios-permisos.sql` | Usuarios del SGBD y permisos diferenciados (mínimo privilegio) | 1 / 2 / 3 |
| `03-backup.sh` · `03-restore.sh` | Copia de seguridad y restauración (`mysqldump`) | 1 / 2 |
| `04-procedimientos.sql` | Procedimientos y funciones almacenadas (`sp_resolve_report`, `fn_count_pending_reports`) | 2 |
| `05-transacciones.md` | Transacciones (`@Transactional` en la app) | 2 |
| `06-triggers.sql` | Trigger de auditoría de reportes (`trg_reports_audit`) | 3 |
| `07-replicacion/` | Replicación source→replica y acceso de solo lectura | 3 |

> El orden de los números refleja el orden recomendado de ejecución/implementación.

## Orden de ejecución (IMPORTANTE)

Hay una dependencia temporal entre los scripts y el arranque de la aplicación, porque
`04` y `06` operan sobre tablas (`reports`, `report_audit`) que **crea Hibernate**, no estos
scripts:

1. **Primer arranque del compose** (volumen MySQL vacío): se ejecuta **solo** `02-usuarios-permisos.sql`
   automáticamente (está montado en `/docker-entrypoint-initdb.d/`). Crea `app_rw` / `app_ro` /
   `app_admin`. **`04`, `06` y `07` NO se ejecutan solos.**
2. **Arrancar la aplicación** (`mvnw spring-boot:run` o el servicio `java-app`): Hibernate
   (`ddl-auto=update`) crea/actualiza todas las tablas a partir de las entidades `@Entity`.
3. **Solo entonces**, ejecutar a mano `04-procedimientos.sql` y `06-triggers.sql` (ya existen
   las tablas que necesitan). `07-replicacion/` es opcional y va aparte.

> Si se ejecuta `04` o `06` **antes** de que la app haya creado las tablas, fallan (no existe
> `reports` / `report_audit`). El compose arranca MySQL con `--log-bin-trust-function-creators=1`
> para que `app_admin` pueda crear la función de `04` y el trigger de `06` con el binlog activo.

## Cómo aplicar los scripts

```bash
# (desde la raíz del repo backend) Conectarse al contenedor MySQL del devcontainer.
# 02 ya se aplica solo en el primer arranque; este comando sirve para re-aplicarlo
# o para correr 04 / 06 tras arrancar la app:
docker compose -f ../.devcontainer/compose.yml exec -T mysql \
  mysql -u app_admin -papp_admin_pwd shareYourStory < db/04-procedimientos.sql

docker compose -f ../.devcontainer/compose.yml exec -T mysql \
  mysql -u app_admin -papp_admin_pwd shareYourStory < db/06-triggers.sql
```
