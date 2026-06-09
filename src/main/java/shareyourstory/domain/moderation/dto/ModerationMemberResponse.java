package shareyourstory.domain.moderation.dto;

/**
 * Miembro para la pestana de gestion del panel de moderacion.
 */
public record ModerationMemberResponse(
        String id,
        String username,
        String community,
        String joined,
        int reports,
        int warnings,
        boolean banned) {
}
