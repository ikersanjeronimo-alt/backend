package shareyourstory.domain.moderation.dto;

/**
 * Miembro del equipo (moderador o administrador) para la pestana de gestion
 * del panel de moderacion.
 */
public record StaffMemberResponse(
        String id,
        String name,
        String username,
        String email,
        String role,
        String company,
        String profession,
        String joined,
        boolean online) {
}
