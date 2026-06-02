-- =============================================================================
-- 02 - USUARIOS Y PERMISOS DEL SGBD (MySQL)
-- =============================================================================
-- Objetivo: dejar de usar 'root' en la aplicación y aplicar MÍNIMO PRIVILEGIO.
-- Ejecutar UNA vez como root:
--   docker compose -f .devcontainer/compose.yml exec mysql \
--     mysql -u root -ppasahitza shareYourStory < db/02-usuarios-permisos.sql
--
-- NOTA: las contraseñas de abajo son de DESARROLLO. En producción deben
-- cambiarse y pasarse a la aplicación mediante variables de entorno.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) app_rw : usuario de la APLICACIÓN (runtime)
-- -----------------------------------------------------------------------------
-- Permisos de datos (DML) + EXECUTE (para llamar a procedimientos almacenados).
-- Incluye DDL limitado (CREATE/ALTER/INDEX/REFERENCES) porque el proyecto usa
-- 'spring.jpa.hibernate.ddl-auto=update' y Hibernate crea/ajusta tablas al
-- arrancar. NO se conceden DROP, GRANT, ni privilegios globales.
-- (En producción: poner ddl-auto=validate y REVOCAR el DDL de este usuario.)
CREATE USER IF NOT EXISTS 'app_rw'@'%' IDENTIFIED BY 'app_rw_pwd';
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, REFERENCES,
      EXECUTE, CREATE VIEW, SHOW VIEW
  ON shareYourStory.* TO 'app_rw'@'%';

-- -----------------------------------------------------------------------------
-- 2) app_ro : usuario de SOLO LECTURA
-- -----------------------------------------------------------------------------
-- Para informes / dashboard y para conectarse a la RÉPLICA de solo lectura
-- (ver db/07-replicacion). No puede modificar datos.
CREATE USER IF NOT EXISTS 'app_ro'@'%' IDENTIFIED BY 'app_ro_pwd';
GRANT SELECT, SHOW VIEW
  ON shareYourStory.* TO 'app_ro'@'%';

-- -----------------------------------------------------------------------------
-- 3) app_admin : ADMINISTRACIÓN de la base de datos
-- -----------------------------------------------------------------------------
-- Mantenimiento del esquema, creación de procedimientos/triggers y copias de
-- seguridad (mysqldump necesita SELECT, LOCK TABLES, etc.). Todo acotado a la
-- base de datos shareYourStory: NO es un superusuario global como root.
CREATE USER IF NOT EXISTS 'app_admin'@'%' IDENTIFIED BY 'app_admin_pwd';
GRANT ALL PRIVILEGES
  ON shareYourStory.* TO 'app_admin'@'%';

FLUSH PRIVILEGES;

-- -----------------------------------------------------------------------------
-- Verificación (descomentar para comprobar)
-- -----------------------------------------------------------------------------
-- SHOW GRANTS FOR 'app_rw'@'%';
-- SHOW GRANTS FOR 'app_ro'@'%';
-- SHOW GRANTS FOR 'app_admin'@'%';
-- SELECT user, host FROM mysql.user WHERE user LIKE 'app\_%';
