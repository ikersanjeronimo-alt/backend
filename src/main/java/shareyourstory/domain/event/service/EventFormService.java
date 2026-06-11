package shareyourstory.domain.event.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.event.DTO.EventFormDTO;
import shareyourstory.domain.event.model.EventForm;
import shareyourstory.domain.event.model.EventFormReply;
import shareyourstory.domain.event.model.EventFormVote;
import shareyourstory.domain.event.model.FormKind;
import shareyourstory.domain.event.repository.EventFormReplyRepository;
import shareyourstory.domain.event.repository.EventFormRepository;
import shareyourstory.domain.event.repository.EventFormVoteRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.WebSocketService;

/**
 * Cuestionarios embebidos en eventos. Un cuestionario por evento; lo crea/borra un
 * profesional/admin (el control de rol vive en SecurityConfig) y lo ven/votan
 * todos los autenticados, incluidos los anonimos. Cada cambio se difunde por
 * WebSocket en su version "publica" (sin datos por-usuario) para refrescar a quien
 * tenga el evento abierto en vivo.
 */
@Service
public class EventFormService {

    private static final int MAX_QUESTION = 140;
    private static final int MAX_OPTION = 80;
    private static final int MAX_OPTIONS = 10;
    private static final int MAX_REPLY = 500;

    @Autowired
    private EventFormRepository formRepo;

    @Autowired
    private EventFormVoteRepository voteRepo;

    @Autowired
    private EventFormReplyRepository replyRepo;

    @Autowired
    private WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public EventFormDTO getForm(Integer eventId, User me) {
        return formRepo.findByEventId(eventId)
                .map(f -> toDTO(f, userId(me), false))
                .orElse(null);
    }

    @Transactional
    public EventFormDTO createForm(Integer eventId, FormKind kind, String question,
            List<String> options, User me) {
        if (kind == null) {
            throw bad("Tipo de formulario invalido");
        }
        if (question == null || question.isBlank()) {
            throw bad("La pregunta es obligatoria");
        }
        if (question.length() > MAX_QUESTION) {
            throw bad("La pregunta supera los " + MAX_QUESTION + " caracteres");
        }
        List<String> cleaned = List.of();
        if (kind == FormKind.CHOICE) {
            cleaned = options == null ? List.of()
                    : options.stream().filter(Objects::nonNull)
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (cleaned.size() < 2) {
                throw bad("Un formulario de opciones necesita al menos 2 opciones");
            }
            if (cleaned.size() > MAX_OPTIONS) {
                throw bad("Demasiadas opciones (maximo " + MAX_OPTIONS + ")");
            }
            if (cleaned.stream().anyMatch(o -> o.length() > MAX_OPTION)) {
                throw bad("Una opcion supera los " + MAX_OPTION + " caracteres");
            }
        }
        // Un cuestionario por evento: reemplaza el anterior (y sus votos/respuestas).
        purge(eventId);
        EventForm form = formRepo.save(new EventForm(eventId, kind, question.trim(), cleaned));
        return broadcastAndReturn(form, userId(me));
    }

    @Transactional
    public void deleteForm(Integer eventId) {
        purge(eventId);
        // Senal de "ya no hay cuestionario" a los suscriptores.
        webSocketService.broadcastEventForm(eventId, null);
    }

    @Transactional
    public EventFormDTO vote(Integer eventId, int optionIndex, User me) {
        Integer uid = requireUser(me);
        EventForm form = formRepo.findByEventId(eventId).orElseThrow(this::notFound);
        if (form.getKind() != FormKind.CHOICE) {
            throw bad("Este cuestionario no admite votos");
        }
        if (optionIndex < 0 || optionIndex >= form.getOptions().size()) {
            throw bad("Opcion fuera de rango");
        }
        if (voteRepo.existsByFormIdAndUserId(form.getId(), uid)) {
            throw conflict("Ya has votado en este cuestionario");
        }
        voteRepo.save(new EventFormVote(form.getId(), uid, optionIndex));
        return broadcastAndReturn(form, uid);
    }

    @Transactional
    public EventFormDTO respond(Integer eventId, String text, User me) {
        Integer uid = requireUser(me);
        EventForm form = formRepo.findByEventId(eventId).orElseThrow(this::notFound);
        if (form.getKind() != FormKind.TEXT) {
            throw bad("Este cuestionario no admite respuestas de texto");
        }
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            throw bad("La respuesta no puede estar vacia");
        }
        if (value.length() > MAX_REPLY) {
            throw bad("La respuesta supera los " + MAX_REPLY + " caracteres");
        }
        if (replyRepo.existsByFormIdAndUserId(form.getId(), uid)) {
            throw conflict("Ya has respondido a este cuestionario");
        }
        replyRepo.save(new EventFormReply(form.getId(), uid, value));
        return broadcastAndReturn(form, uid);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Difunde la vista publica (sin datos por-usuario) y devuelve la del llamante. */
    private EventFormDTO broadcastAndReturn(EventForm form, Integer uid) {
        webSocketService.broadcastEventForm(form.getEventId(), toDTO(form, null, true));
        return toDTO(form, uid, false);
    }

    /** Borra el cuestionario del evento y sus votos/respuestas. La entidad se borra
     *  por id (no bulk) para que la cascada limpie tambien event_form_options. */
    private void purge(Integer eventId) {
        formRepo.findByEventId(eventId).ifPresent(f -> {
            voteRepo.deleteByFormId(f.getId());
            replyRepo.deleteByFormId(f.getId());
            formRepo.delete(f);
            formRepo.flush();
        });
    }

    private EventFormDTO toDTO(EventForm form, Integer uid, boolean publicView) {
        boolean choice = form.getKind() == FormKind.CHOICE;
        List<String> options = choice ? List.copyOf(form.getOptions()) : List.of();

        List<Integer> counts = List.of();
        int total = 0;
        Integer myVote = null;
        if (choice) {
            int[] c = new int[options.size()];
            List<EventFormVote> votes = voteRepo.findByFormId(form.getId());
            for (EventFormVote v : votes) {
                if (v.getOptionIndex() >= 0 && v.getOptionIndex() < c.length) {
                    c[v.getOptionIndex()]++;
                }
                if (!publicView && uid != null && uid.equals(v.getUserId())) {
                    myVote = v.getOptionIndex();
                }
            }
            counts = Arrays.stream(c).boxed().toList();
            total = votes.size();
        }

        int responseCount = (int) replyRepo.countByFormId(form.getId());
        boolean myResponded = !publicView && uid != null
                && replyRepo.existsByFormIdAndUserId(form.getId(), uid);
        // El listado de respuestas de texto solo a quien ya respondio (parita con el UX
        // y evita filtrar textos ajenos a quien aun no participa); nunca en broadcast.
        List<String> responses = (!choice && myResponded)
                ? replyRepo.findByFormIdOrderByCreatedAtAsc(form.getId()).stream()
                        .map(EventFormReply::getText).toList()
                : List.of();

        return new EventFormDTO(
                choice ? "choice" : "text",
                form.getQuestion(),
                options,
                counts,
                total,
                myVote,
                responses,
                responseCount,
                myResponded);
    }

    private Integer userId(User me) {
        return me == null ? null : me.getUserId();
    }

    private Integer requireUser(User me) {
        if (me == null || me.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return me.getUserId();
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "El evento no tiene cuestionario");
    }
}
