package shareyourstory.domain.bottle.controller;

import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.service.PrivateMessageService;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professionals")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    /**
     * Mensajes entre el usuario AUTENTICADO y el otro participante ({@code otherId}).
     * El usuario se toma del JWT, NUNCA de un parametro del cliente (evita el IDOR).
     * El chat privado es siempre usuario/anonimo <-> profesional, asi que se decide
     * que columna es el userId y cual el professionalId segun el rol del autenticado.
     */
    @GetMapping("/{otherId}/messages")
    public ResponseEntity<List<PrivateMessage>> getMessages(
            @PathVariable Integer otherId,
            @AuthenticationPrincipal User me) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        boolean meIsProfessional = me.getRole() == UserRole.PROFESSIONAL;
        Integer userId = meIsProfessional ? otherId : me.getUserId();
        Integer professionalId = meIsProfessional ? me.getUserId() : otherId;

        List<PrivateMessage> messages = privateMessageService.getMessages(userId, professionalId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Envia un mensaje en la conversacion del usuario autenticado con {@code otherId}.
     * El emisor (userId) y el rol ('from') se derivan del principal, no del body.
     */
    @PostMapping("/{otherId}/messages")
    public ResponseEntity<PrivateMessage> sendMessage(
            @PathVariable Integer otherId,
            @AuthenticationPrincipal User me,
            @RequestBody Map<String, Object> payload) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }

        String text = (String) payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        boolean meIsProfessional = me.getRole() == UserRole.PROFESSIONAL;
        Integer userId = meIsProfessional ? otherId : me.getUserId();
        Integer professionalId = meIsProfessional ? me.getUserId() : otherId;
        String from = meIsProfessional ? "professional" : "user";

        PrivateMessage message = privateMessageService.saveMessage(userId, professionalId, text, from);
        return ResponseEntity.ok(message);
    }
}
