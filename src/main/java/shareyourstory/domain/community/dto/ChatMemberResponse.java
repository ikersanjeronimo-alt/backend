package shareyourstory.domain.community.dto;

/**
 * Miembro de una comunidad para la lista de "miembros activos" del chat.
 */
public record ChatMemberResponse(String userId, String username, String initials) {
}
