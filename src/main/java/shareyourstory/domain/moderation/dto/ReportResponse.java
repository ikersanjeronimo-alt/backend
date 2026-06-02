package shareyourstory.domain.moderation.dto;

import java.time.LocalDateTime;
import shareyourstory.domain.moderation.model.Report;

/**
 * Vista segura de un reporte para la API: NO expone la entidad User (evita
 * filtrar password/secretKey); solo el id del moderador.
 */
public record ReportResponse(
        Integer id,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        Integer storyId,
        Integer moderatorId) {

    public static ReportResponse from(Report r) {
        return new ReportResponse(
                r.getId(),
                r.getReason(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getCreatedAt(),
                r.getResolvedAt(),
                r.getStory() != null ? r.getStory().getId() : null,
                r.getModerator() != null ? r.getModerator().getId() : null);
    }
}
