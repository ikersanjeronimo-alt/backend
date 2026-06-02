package shareyourstory.domain.moderation.service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.model.ReportAudit;
import shareyourstory.domain.moderation.model.ReportStatus;
import shareyourstory.domain.moderation.repository.ReportAuditRepository;
import shareyourstory.domain.moderation.repository.ReportRepository;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;

@Service
public class ModerationService {

    private final ReportRepository reportRepository;
    private final StoryMapRepository storyMapRepository;
    private final ReportAuditRepository reportAuditRepository;

    public ModerationService(ReportRepository reportRepository,
            StoryMapRepository storyMapRepository,
            ReportAuditRepository reportAuditRepository) {
        this.reportRepository = reportRepository;
        this.storyMapRepository = storyMapRepository;
        this.reportAuditRepository = reportAuditRepository;
    }

    /** Historial de auditoría de un reporte (lo genera el trigger trg_reports_audit). */
    public List<ReportAudit> auditFor(Integer reportId) {
        return reportAuditRepository.findByReportIdOrderByCreatedAtDesc(reportId);
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

    /** Lista los reportes pendientes (con story y moderator ya cargados: sin N+1). */
    public List<Report> pendingReports() {
        return reportRepository.findByStatusWithRelations(ReportStatus.PENDING);
    }

    /** Número de reportes pendientes (vía función almacenada). */
    public long pendingCount() {
        return reportRepository.countPendingReportsViaFunction();
    }

    private static final String REDACTED_MESSAGE = "[eliminado por moderación]";

    /**
     * Resuelve un reporte de forma ATÓMICA (transacción que abarca dos tablas):
     *   1) tabla `reports`   -> vía el procedimiento almacenado sp_resolve_report
     *   2) tabla `storyMaps` -> si la acción es RESOLVED, se sanea el mensaje de
     *      la historia infractora.
     *
     * Argumento del uso de @Transactional: ambos cambios deben confirmarse o
     * descartarse JUNTOS. Si el saneado de la historia falla (paso 2), no puede
     * quedar un reporte marcado como RESUELTO sobre una historia que sigue
     * mostrando contenido inapropiado (paso 1). Spring revierte automáticamente
     * toda la transacción ante cualquier RuntimeException, incluido el error
     * (SIGNAL) que el propio procedimiento lanza si el reporte no es válido.
     */
    @Transactional
    public void resolveReport(Integer reportId, Integer moderatorId, String action) {
        // 1) Procedimiento almacenado: actualiza la tabla `reports`.
        reportRepository.resolveReport(reportId, moderatorId, action);

        // 2) Solo si se confirma la infracción, se sanea la historia (otra tabla).
        //    Se normaliza igual que sp_resolve_report (TRIM + case-insensitive)
        //    para que la decisión de saneado coincida con el estado que persiste
        //    el procedimiento (p. ej. " resolved " -> el SP guarda RESOLVED).
        if (action != null && "RESOLVED".equalsIgnoreCase(action.trim())) {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new NoSuchElementException(
                            "El reporte " + reportId + " no existe"));
            StoryMap story = report.getStory();
            // `story` esta gestionada dentro de esta transaccion: el cambio se
            // persiste por dirty-checking al hacer flush; no hace falta save().
            story.setMessage(REDACTED_MESSAGE);
        }
    }
}
