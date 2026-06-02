package shareyourstory.domain.moderation.service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.model.ReportStatus;
import shareyourstory.domain.moderation.repository.ReportRepository;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;

@Service
public class ModerationService {

    private final ReportRepository reportRepository;
    private final StoryMapRepository storyMapRepository;

    public ModerationService(ReportRepository reportRepository,
            StoryMapRepository storyMapRepository) {
        this.reportRepository = reportRepository;
        this.storyMapRepository = storyMapRepository;
    }

    /** Crea un reporte sobre una historia existente. */
    public Report createReport(Integer storyId, String reason) {
        StoryMap story = storyMapRepository.findById(storyId)
                .orElseThrow(() -> new NoSuchElementException(
                        "La historia " + storyId + " no existe"));
        Report report = new Report();
        report.setStory(story);
        report.setReason(reason);
        report.setStatus(ReportStatus.PENDING);
        return reportRepository.save(report);
    }

    /** Lista los reportes pendientes. */
    public List<Report> pendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    /** Número de reportes pendientes (vía función almacenada). */
    public long pendingCount() {
        return reportRepository.countPendingReportsViaFunction();
    }

    /**
     * Resuelve un reporte llamando al procedimiento almacenado sp_resolve_report.
     * (En el Paso 5 se envuelve en una transacción que además sanea la historia.)
     */
    public void resolveReport(Integer reportId, Integer moderatorId, String action) {
        reportRepository.resolveReport(reportId, moderatorId, action);
    }
}
