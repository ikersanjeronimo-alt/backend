package shareyourstory.domain.moderation.dto;

/** Petición para reportar una historia del mapa. */
public record CreateReportRequest(Integer storyId, String reason) {
}
