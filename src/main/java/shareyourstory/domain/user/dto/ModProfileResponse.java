package shareyourstory.domain.user.dto;

/**
 * Ficha del moderador (datos editables en Configuracion). company es null para
 * los administradores.
 */
public record ModProfileResponse(
        String name,
        String lastName,
        String username,
        String email,
        String company) {
}
