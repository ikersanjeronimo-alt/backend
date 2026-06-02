-- NOTA: las contraseñas de abajo son de DESARROLLO. En producción deben
-- cambiarse y pasarse a la aplicación mediante variables de entorno.
-- -----------------------------------------------------------------------------

-- app_rw
-- NO se conceden DROP, GRANT, ni privilegios globales.
-- (En producción: poner ddl-auto=validate y REVOCAR el DDL de este usuario.)
CREATE USER IF NOT EXISTS 'app_rw'@'%' IDENTIFIED BY 'app_rw_pwd';
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, REFERENCES,
      EXECUTE, CREATE VIEW, SHOW VIEW
  ON shareYourStory.* TO 'app_rw'@'%';

-- -----------------------------------------------------------------------------
-- app_ro
-- Solo lectura
CREATE USER IF NOT EXISTS 'app_ro'@'%' IDENTIFIED BY 'app_ro_pwd';
GRANT SELECT, SHOW VIEW
  ON shareYourStory.* TO 'app_ro'@'%';

-- -----------------------------------------------------------------------------
-- app_admin 
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
