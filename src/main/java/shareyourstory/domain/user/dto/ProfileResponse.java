package shareyourstory.domain.user.dto;

import java.util.List;

/**
 * Perfil del usuario para la API. Stats acotadas a lo calculable de verdad
 * (mensajes y comunidades); el resto a 0 hasta que haya datos por usuario. El
 * feed de actividad se entrega vacio (no hay modelo de actividad).
 */
public record ProfileResponse(
        String username,
        String role,
        String joinedAt,
        Stats stats,
        List<Object> activity,
        List<String> topics) {

    public record Stats(int messages, int communities, int events, int stories, int bottles) {
    }
}
