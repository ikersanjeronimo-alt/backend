package shareyourstory.domain.community.dto;

/**
 * Miembro de una comunidad para la lista de "miembros activos" del chat.
 * {@code online} indica presencia real (sesion WebSocket viva), no solo membresia.
 */
public record ChatMemberResponse(String userId, String username, String initials, boolean online) {
}
