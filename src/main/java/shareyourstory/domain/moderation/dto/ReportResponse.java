package shareyourstory.domain.moderation.dto;

import java.time.format.DateTimeFormatter;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.model.ReportTargetType;

/**
 * Vista de un reporte para el panel. Forma alineada con el front (ApiReport):
 * incluye autor, reportado, contenido y comunidad (snapshots del reporte).
 */
public record ReportResponse(
        String id,
        String type,
        String reporter,
        String reported,
        String content,
        String reason,
        String community,
        String time,
        String status) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public static ReportResponse from(Report r) {
        return new ReportResponse(
                String.valueOf(r.getId()),
                r.getTargetType() == ReportTargetType.MESSAGE ? "message" : "story",
                nz(r.getReporterUsername()),
                nz(r.getReportedUsername()),
                nz(r.getContent()),
                r.getReason(),
                r.getCommunity(),
                r.getCreatedAt() == null ? "" : r.getCreatedAt().format(FMT),
                r.getStatus() == null ? "pending" : r.getStatus().name().toLowerCase());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
