package shareyourstory.domain.moderation.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.moderation.model.ReportAudit;

@Repository
public interface ReportAuditRepository extends JpaRepository<ReportAudit, Integer> {

    List<ReportAudit> findByReportIdOrderByCreatedAtDesc(Integer reportId);
}
