package shareyourstory.websocket.service;

/**
 * Numero de usuarios realmente conectados al chat de una comunidad, difundido
 * por /topic/communities/{id}/presence cada vez que alguien entra o sale.
 */
public record PresenceDTO(String communityId, int online) {
}
