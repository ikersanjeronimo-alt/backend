-- =============================================================================
-- 04 - PROCEDIMIENTOS Y FUNCIONES ALMACENADAS (MySQL)
-- =============================================================================
-- Requiere que la tabla `reports` ya exista (Hibernate la crea al arrancar la
-- app, ddl-auto=update). Ejecutar como app_admin (o root):
--   docker compose -f .devcontainer/compose.yml exec mysql \
--     mysql -u app_admin -papp_admin_pwd shareYourStory < db/04-procedimientos.sql
-- =============================================================================

DELIMITER $$

-- -----------------------------------------------------------------------------
-- PROCEDIMIENTO: sp_resolve_report
-- Resuelve un reporte (RESOLVED o DISMISSED) dejando constancia del moderador
-- y la fecha. Incluye validaciones (existencia, estado y acción válida).
-- La aplicación lo invoca desde ReportRepositoryImpl (StoredProcedureQuery).
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_resolve_report $$
CREATE PROCEDURE sp_resolve_report(
    IN p_report_id    INT,
    IN p_moderator_id INT,
    IN p_action       VARCHAR(20)
)
BEGIN
    -- Normaliza la accion: la comparacion de cadenas en MySQL es case-insensitive
    -- (utf8mb4_..._ci), por lo que 'resolved' pasaria la validacion pero se
    -- guardaria en minusculas y romperia el mapeo @Enumerated(STRING) al releer la
    -- fila. Forzamos MAYUSCULAS; ademas detectamos NULL, que de otro modo evitaria
    -- el NOT IN (NULL NOT IN (...) = NULL, no TRUE).
    SET p_action = UPPER(TRIM(p_action));

    IF p_action IS NULL OR p_action NOT IN ('RESOLVED', 'DISMISSED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Accion no valida: usar RESOLVED o DISMISSED';
    END IF;

    -- Comprobar-y-actuar en UNA sentencia: el guard status = 'PENDING' dentro del
    -- propio UPDATE evita la condicion de carrera de dos moderadores resolviendo
    -- el mismo reporte a la vez (un SELECT COUNT previo no la evitaba).
    UPDATE reports
       SET status      = p_action,
           resolved_by = p_moderator_id,
           resolved_at = NOW()
     WHERE id = p_report_id AND status = 'PENDING';

    -- 0 filas afectadas => el reporte no existe o ya no estaba PENDING.
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El reporte no existe o no esta PENDING';
    END IF;
END $$

-- -----------------------------------------------------------------------------
-- FUNCIÓN: fn_count_pending_reports
-- Devuelve el número de reportes pendientes. La aplicación la invoca desde
-- ReportRepository.countPendingReportsViaFunction() (SELECT fn_...()).
-- -----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_count_pending_reports $$
CREATE FUNCTION fn_count_pending_reports()
RETURNS INT
READS SQL DATA
NOT DETERMINISTIC
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count FROM reports WHERE status = 'PENDING';
    RETURN v_count;
END $$

DELIMITER ;

-- -----------------------------------------------------------------------------
-- Verificación (descomentar)
-- -----------------------------------------------------------------------------
-- SHOW PROCEDURE STATUS WHERE Db = 'shareYourStory';
-- SHOW FUNCTION STATUS  WHERE Db = 'shareYourStory';
-- CALL sp_resolve_report(1, 1, 'RESOLVED');
-- SELECT fn_count_pending_reports();
