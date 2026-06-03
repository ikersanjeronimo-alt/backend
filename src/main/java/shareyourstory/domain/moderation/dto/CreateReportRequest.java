package shareyourstory.domain.moderation.dto;

/**
 * Peticion para reportar contenido: una historia del mapa (storyId) o un mensaje
 * de comunidad (messageId). Se envia uno u otro.
 */
public record CreateReportRequest(Integer storyId, Long messageId, String reason) {
}
