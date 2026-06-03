package shareyourstory.domain.user.dto;

/**
 * Mensaje reciente para el widget del dashboard: el ultimo de cada comunidad a
 * la que pertenece el usuario.
 */
public record DashboardMessageResponse(
        String id,
        String communityId,
        String community,
        String username,
        String text,
        String time) {
}
