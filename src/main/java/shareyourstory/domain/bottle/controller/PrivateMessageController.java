package shareyourstory.domain.bottle.controller;

import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.service.PrivateMessageService;
import shareyourstory.domain.bottle.DTO.PrivateConversationResponse;
import shareyourstory.domain.bottle.DTO.PrivateMessageResponse;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * Chat privado usuario/anonimo <-> profesional. La identidad SIEMPRE sale del
 * JWT (@AuthenticationPrincipal), nunca de un parametro del cliente (IDOR). El
 * chat es siempre usuario <-> profesional, asi que se decide que columna es el
 * userId y cual el professionalId segun el rol del autenticado.
 */
@RestController
@RequestMapping("/api/chats")
@RequestMapping("/api/chats")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    // ── Lado usuario: conversacion con un profesional ────────────────────────

    @GetMapping("/{otherId}/messages")
    public ResponseEntity<List<PrivateMessageResponse>> getMessages(
            @PathVariable Integer otherId,
            @AuthenticationPrincipal User me) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        boolean meIsProfessional = me.getRole() == UserRole.PROFESSIONAL;
        Integer userId = meIsProfessional ? otherId : me.getUserId();
        Integer professionalId = meIsProfessional ? me.getUserId() : otherId;

        List<PrivateMessageResponse> messages = privateMessageService
                .getMessages(userId, professionalId).stream()
                .map(PrivateMessageResponse::of)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{otherId}/messages")
    public ResponseEntity<PrivateMessageResponse> sendMessage(
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

        PrivateMessage saved = privateMessageService.saveMessage(userId, professionalId, text, from);
        return ResponseEntity.ok(PrivateMessageResponse.of(saved));
    }

    // ── Lado profesional: bandeja de conversaciones ──────────────────────────

    @GetMapping("/inbox")
    public ResponseEntity<List<PrivateConversationResponse>> inbox(@AuthenticationPrincipal User me) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(privateMessageService.inboxFor(me.getUserId()));
    }

    @GetMapping("/inbox/{userId}/messages")
    public ResponseEntity<List<PrivateMessageResponse>> inboxMessages(
            @PathVariable Integer userId,
            @AuthenticationPrincipal User me) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        List<PrivateMessageResponse> messages = privateMessageService
                .getMessages(userId, me.getUserId()).stream()
                .map(PrivateMessageResponse::of)
                .toList();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/inbox/{userId}/messages")
    public ResponseEntity<PrivateMessageResponse> inboxSend(
            @PathVariable Integer userId,
            @AuthenticationPrincipal User me,
            @RequestBody Map<String, Object> payload) {
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        String text = (String) payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        PrivateMessage saved = privateMessageService.saveMessage(userId, me.getUserId(), text, "professional");
        return ResponseEntity.ok(PrivateMessageResponse.of(saved));
    }
}
