-- =============================================================================
-- PRIMARIO: base de datos, usuario de replicación y usuarios de aplicación.
-- Ejecutar tras arrancar el compose:
--   docker exec -i syss-mysql-primary mysql -u root -ppasahitza \
--     < db/07-replicacion/07-primary-users.sql
-- Estas sentencias se ejecutan con el servidor ya arrancado, por lo que quedan
-- registradas en el binlog (GTID) y se replican automáticamente a la réplica.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS shareYourStory;

-- Usuario que usa la RÉPLICA para conectarse al PRIMARIO
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl_pwd';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';

-- Usuarios de aplicación (mismos roles que db/02-usuarios-permisos.sql)
CREATE USER IF NOT EXISTS 'app_rw'@'%' IDENTIFIED BY 'app_rw_pwd';
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, REFERENCES,
      EXECUTE, CREATE VIEW, SHOW VIEW
  ON shareYourStory.* TO 'app_rw'@'%';

CREATE USER IF NOT EXISTS 'app_ro'@'%' IDENTIFIED BY 'app_ro_pwd';
GRANT SELECT, SHOW VIEW ON shareYourStory.* TO 'app_ro'@'%';

CREATE USER IF NOT EXISTS 'app_admin'@'%' IDENTIFIED BY 'app_admin_pwd';
GRANT ALL PRIVILEGES ON shareYourStory.* TO 'app_admin'@'%';
-- RELOAD global: necesario para las copias con mysqldump --single-transaction
-- (ejecuta FLUSH TABLES). Ver db/02-usuarios-permisos.sql y db/03-backup.sh.
GRANT RELOAD ON *.* TO 'app_admin'@'%';

FLUSH PRIVILEGES;
