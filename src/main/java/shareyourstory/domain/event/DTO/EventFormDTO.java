package shareyourstory.domain.event.DTO;

import java.util.List;

/**
 * Estado de un cuestionario de evento que viaja al front. Tiene dos sabores:
 *  - Vista por-usuario (GET / POST propios): {@code myVote}/{@code myResponded}
 *    reflejan al usuario autenticado y {@code responses} solo se rellena si ya
 *    respondio (parita con el UX: ves las respuestas tras enviar la tuya).
 *  - Vista publica (broadcast WebSocket a todos los suscriptores): sin datos
 *    por-usuario ({@code myVote=null}, {@code myResponded=false}) y, en TEXT, sin
 *    el listado de respuestas (solo el contador) para no filtrar textos ajenos.
 *
 * {@code kind} se serializa en minusculas ("choice"/"text") para casar con el front.
 */
public record EventFormDTO(
        String kind,
        String question,
        List<String> options,
        List<Integer> counts,
        int totalVotes,
        Integer myVote,
        List<String> responses,
        int responseCount,
        boolean myResponded) {
}
