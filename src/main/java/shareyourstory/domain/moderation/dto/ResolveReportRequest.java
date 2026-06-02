package shareyourstory.domain.moderation.dto;

/** Petición para resolver un reporte. action = "RESOLVED" | "DISMISSED". */
public record ResolveReportRequest(String action) {
}
