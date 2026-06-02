# Base de datos — ShareYourStory

Esta carpeta agrupa todos los artefactos de **base de datos** del proyecto: modelo
E/R, gestión de usuarios y permisos del SGBD, copias de seguridad y restauración,
procedimientos almacenados, triggers y replicación.

El SGBD es **MySQL** (ver `.devcontainer/compose.yml`). La aplicación accede vía
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
| `04-procedimientos.sql` | Procedimientos y funciones almacenadas | 2 |
| `06-triggers.sql` | Triggers (ejecución automática / auditoría) | 3 |
| `07-replicacion/` | Replicación source→replica y acceso de solo lectura | 3 |

> El orden de los números refleja el orden recomendado de ejecución/implementación.
> Las transacciones (Nivel 2) se implementan en el código de la aplicación
> (`@Transactional`), documentadas en `05-transacciones.md`.

## Cómo aplicar los scripts

```bash
# Conectarse al contenedor MySQL del devcontainer
docker compose -f .devcontainer/compose.yml exec mysql \
  mysql -u root -ppasahitza shareYourStory < db/02-usuarios-permisos.sql
```
