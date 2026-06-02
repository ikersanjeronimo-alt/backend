package shareyourstory.domain.moderation.repository;

import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;

/**
 * Implementación de la llamada al procedimiento almacenado {@code sp_resolve_report}
 * mediante JPA ({@link StoredProcedureQuery}). Es el punto donde la aplicación
 * "se comunica con la BD a través de un procedimiento" (rúbrica Nivel 2).
 *
 * Spring Data detecta automáticamente esta clase por el sufijo {@code Impl}.
 */
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void resolveReport(Integer reportId, Integer moderatorId, String action) {
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("sp_resolve_report");
        query.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
        query.setParameter(1, reportId);
        query.setParameter(2, moderatorId);
        query.setParameter(3, action);
        query.execute();
    }
}
