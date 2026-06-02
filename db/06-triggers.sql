-- =============================================================================
-- 06 - TRIGGERS (MySQL)
-- =============================================================================
-- Requiere que las tablas `reports` y `report_audit` ya existan (Hibernate las
-- crea al arrancar la app, ddl-auto=update). Ejecutar como app_admin (o root):
--   docker compose -f .devcontainer/compose.yml exec mysql \
--     mysql -u app_admin -papp_admin_pwd shareYourStory < db/06-triggers.sql
-- =============================================================================

DELIMITER $$

-- -----------------------------------------------------------------------------
-- TRIGGER: trg_reports_audit
-- Audita AUTOMÁTICAMENTE cada cambio de estado de un reporte, registrando en
-- report_audit el estado anterior, el nuevo, el moderador y la fecha.
-- Se dispara venga el cambio de donde venga (procedimiento, ORM o SQL manual).
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_reports_audit $$
CREATE TRIGGER trg_reports_audit
AFTER UPDATE ON reports
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO report_audit
            (report_id, moderator_id, old_status, new_status, action, created_at)
        VALUES
            (NEW.id, NEW.resolved_by, OLD.status, NEW.status,
             CONCAT('Estado ', OLD.status, ' -> ', NEW.status), NOW());
    END IF;
END $$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- Verificación (descomentar)
-- -----------------------------------------------------------------------------
-- SHOW TRIGGERS FROM shareYourStory;
-- CALL sp_resolve_report(1, 1, 'RESOLVED');
-- SELECT * FROM report_audit WHERE report_id = 1 ORDER BY created_at DESC;
