package shareyourstory.domain.event.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.event.DTO.EventFormDTO;
import shareyourstory.domain.event.model.FormKind;
import shareyourstory.domain.event.service.EventFormService;
import shareyourstory.domain.user.model.User;

/**
 * Cuestionario embebido en un evento. La identidad sale del JWT
 * (@AuthenticationPrincipal). Crear/borrar es solo profesional/admin (gestionado
 * en SecurityConfig); ver/votar/responder lo puede cualquier autenticado, anonimo
 * incluido.
 */
@RestController
@RequestMapping("/api/events/{eventId}/form")
public class EventFormController {

    @Autowired
    private EventFormService eventFormService;

    @GetMapping
    public ResponseEntity<EventFormDTO> getForm(@PathVariable Integer eventId,
            @AuthenticationPrincipal User me) {
        EventFormDTO dto = eventFormService.getForm(eventId, me);
        return dto == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<EventFormDTO> createForm(@PathVariable Integer eventId,
            @AuthenticationPrincipal User me, @RequestBody Map<String, Object> body) {
        FormKind kind = parseKind(body.get("kind"));
        String question = str(body.get("question"));
        List<String> options = strList(body.get("options"));
        return ResponseEntity.ok(eventFormService.createForm(eventId, kind, question, options, me));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteForm(@PathVariable Integer eventId) {
        eventFormService.deleteForm(eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vote")
    public ResponseEntity<EventFormDTO> vote(@PathVariable Integer eventId,
            @AuthenticationPrincipal User me, @RequestBody Map<String, Object> body) {
        Object idx = body.get("optionIndex");
        if (!(idx instanceof Number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "optionIndex invalido");
        }
        return ResponseEntity.ok(eventFormService.vote(eventId, ((Number) idx).intValue(), me));
    }

    @PostMapping("/response")
    public ResponseEntity<EventFormDTO> respond(@PathVariable Integer eventId,
            @AuthenticationPrincipal User me, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(eventFormService.respond(eventId, str(body.get("text")), me));
    }

    // ── Helpers de parseo del body ─────────────────────────────────────────────

    private FormKind parseKind(Object raw) {
        String k = str(raw);
        if (k == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el tipo de formulario");
        }
        return switch (k.trim().toLowerCase()) {
            case "choice" -> FormKind.CHOICE;
            case "text" -> FormKind.TEXT;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de formulario invalido");
        };
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private List<String> strList(Object o) {
        if (!(o instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return out;
    }
}
