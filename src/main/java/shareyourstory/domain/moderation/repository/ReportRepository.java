package shareyourstory.domain.moderation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.model.ReportStatus;

@Repository
public interface ReportRepository
        extends JpaRepository<Report, Integer>, ReportRepositoryCustom {

    List<Report> findByStatus(ReportStatus status);

    /**
     * Llama a la FUNCIÓN almacenada fn_count_pending_reports() (ver
     * db/04-procedimientos.sql). Demuestra el uso de una función del SGBD desde
     * la aplicación.
     */
    @Query(value = "SELECT fn_count_pending_reports()", nativeQuery = true)
    long countPendingReportsViaFunction();
}
