package shareyourstory.domain.bottle.DTO;

/**
 * Una conversacion en la bandeja de un profesional: el usuario con el que habla
 * y el ultimo mensaje intercambiado.
 */
public record PrivateConversationResponse(
        String userId,
        String username,
        String lastMessage,
        String lastTime) {
}
