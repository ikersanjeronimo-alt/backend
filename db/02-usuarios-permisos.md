# Usuarios y permisos del SGBD

Cumple los requisitos de la rúbrica *"gestiona usuarios"* (Nivel 1) y
*"gestiona el acceso de forma eficiente"* (Nivel 2/3).

## Problema que resuelve

Antes la aplicación se conectaba como **`root`** (superusuario con todos los
privilegios sobre todas las bases de datos). Si la aplicación se ve comprometida,
el atacante hereda ese poder total. Aplicamos el principio de **mínimo privilegio**:
cada usuario tiene solo los permisos que necesita.

## Usuarios definidos (`db/02-usuarios-permisos.sql`)

| Usuario | Para qué | Permisos sobre `shareYourStory` |
|---|---|---|
| **`app_rw`** | Conexión de la **aplicación** en ejecución | DML (`SELECT/INSERT/UPDATE/DELETE`) + `EXECUTE` + DDL limitado (`CREATE/ALTER/INDEX/REFERENCES`). **Sin** `DROP`, `GRANT` ni privilegios globales. |
| **`app_ro`** | Informes, dashboard y **réplica** de solo lectura | `SELECT`, `SHOW VIEW`. |
| **`app_admin`** | Mantenimiento, procedimientos/triggers y **backups** | `ALL PRIVILEGES` pero **acotado a la BD** (no global). |

> **¿Por qué `app_rw` tiene algo de DDL?** Porque el proyecto usa
> `spring.jpa.hibernate.ddl-auto=update`, y Hibernate crea/ajusta tablas al
> arrancar. En producción se recomienda `ddl-auto=validate` y revocar el DDL de
> `app_rw`, dejando la gestión del esquema a `app_admin`.

## Acceso "eficiente": dos capas complementarias

1. **Capa SGBD** (este paso): usuarios con permisos diferenciados → contención de
   daños si la app se ve comprometida.
2. **Capa aplicación** (ya existente en `SecurityConfig.java`): control de acceso
   por rol con `hasRole(...)` / `hasAnyRole(...)` / `.authenticated()` sobre los
   endpoints (p. ej. registrar moderadores solo `ADMINISTRATOR`, crear eventos
   solo `PROFESSIONAL`/`ADMINISTRATOR`). Los roles viven en `users.role`
   (`UserRole`) y se propagan como *authorities* en `User.getAuthorities()`.

## Puesta en marcha (bootstrap)

1. Levantar la BD y crear los usuarios **una vez** (como root):
   ```bash
   docker compose -f .devcontainer/compose.yml exec mysql \
     mysql -u root -ppasahitza shareYourStory < db/02-usuarios-permisos.sql
   ```
2. Arrancar la aplicación. Por defecto se conecta como `app_rw` (ver
   `application.properties`). Para sobreescribir credenciales:
   ```bash
   export DB_USERNAME=app_rw
   export DB_PASSWORD=app_rw_pwd
   export JWT_SECRET=<secreto-de-produccion>
   ```
   > Escape de transición: si aún no has creado `app_rw`, puedes arrancar con
   > `DB_USERNAME=root DB_PASSWORD=pasahitza` temporalmente.

## Nota de seguridad sobre el repositorio

- Las credenciales y el secreto JWT se han **externalizado** a variables de
  entorno (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`). Los valores por defecto
  son **solo para desarrollo**.
- Se ha eliminado del fichero la contraseña de aplicación de Gmail que estaba
  comentada en claro.
- ⚠️ El secreto JWT y la contraseña antiguos **siguen en el historial de git**.
  Para una limpieza completa habría que rotarlos y reescribir el historial
  (`git filter-repo`), fuera del alcance de este paso.
