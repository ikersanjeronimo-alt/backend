package shareyourstory.domain.moderation.repository;

/**
 * Operaciones de repositorio que no son consultas derivadas: aquí, la llamada
 * al procedimiento almacenado {@code sp_resolve_report} (ver db/04-procedimientos.sql).
 */
public interface ReportRepositoryCustom {

    void resolveReport(Integer reportId, Integer moderatorId, String action);
}
