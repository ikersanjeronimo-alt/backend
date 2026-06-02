-- =============================================================================
-- RÉPLICA: apuntar al primario e iniciar la replicación (GTID auto-position).
-- Ejecutar DESPUÉS de crear los usuarios en el primario:
--   docker exec -i syss-mysql-replica mysql -u root -ppasahitza \
--     < db/07-replicacion/07-setup-replica.sql
-- =============================================================================

STOP REPLICA;
RESET REPLICA ALL;

CHANGE REPLICATION SOURCE TO
    SOURCE_HOST          = 'mysql-primary',
    SOURCE_PORT          = 3306,
    SOURCE_USER          = 'repl',
    SOURCE_PASSWORD      = 'repl_pwd',
    SOURCE_AUTO_POSITION = 1,
    GET_SOURCE_PUBLIC_KEY = 1;

START REPLICA;

-- Replica_IO_Running y Replica_SQL_Running deben aparecer como 'Yes'
SHOW REPLICA STATUS\G
