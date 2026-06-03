package shareyourstory.domain.moderation.dto;

/**
 * Peticion para reportar contenido: una historia (storyId), un mensaje de
 * comunidad (messageId) o un mensaje privado (privateMessageId). Se envia uno.
 */
public record CreateReportRequest(Integer storyId, Long messageId, Long privateMessageId, String reason) {
}
