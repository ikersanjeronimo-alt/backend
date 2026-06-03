package shareyourstory.domain.community.dto;

import java.time.format.DateTimeFormatter;
import shareyourstory.domain.community.model.CommunityMessage;

/**
 * Vista de mensaje de comunidad para la API: mismo formato que el DTO del
 * websocket. Incluye `time` formateado (HH:mm) y `own` calculado respecto al
 * usuario autenticado, que es lo que el front necesita para alinear las burbujas.
 */
public record CommunityMessageResponse(
        String id,
        String username,
        String text,
        String time,
        boolean own) {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public static CommunityMessageResponse from(CommunityMessage m, Integer currentUserId) {
        String time = m.getCreatedAt() == null ? "" : m.getCreatedAt().format(HHMM);
        boolean own = currentUserId != null && currentUserId.equals(m.getUserId());
        return new CommunityMessageResponse(
                String.valueOf(m.getId()), m.getUsername(), m.getText(), time, own);
    }
}
