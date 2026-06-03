package shareyourstory.domain.bottle.DTO;

import java.time.format.DateTimeFormatter;
import shareyourstory.domain.bottle.model.PrivateMessage;

/**
 * Vista de un mensaje privado para la API. `from` ("user" | "professional") es
 * el rol del emisor tal cual se guardo; el front alinea la burbuja con eso.
 */
public record PrivateMessageResponse(String id, String from, String text, String time) {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public static PrivateMessageResponse of(PrivateMessage m) {
        String time = m.getCreatedAt() == null ? "" : m.getCreatedAt().format(HHMM);
        return new PrivateMessageResponse(String.valueOf(m.getId()), m.getFrom(), m.getText(), time);
    }
}
