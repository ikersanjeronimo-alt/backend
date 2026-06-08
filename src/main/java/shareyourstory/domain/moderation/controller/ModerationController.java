package shareyourstory.domain.moderation.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.moderation.dto.CreateReportRequest;
import shareyourstory.domain.moderation.dto.ModerationMemberResponse;
import shareyourstory.domain.moderation.dto.ReportResponse;
import shareyourstory.domain.moderation.dto.ResolveReportRequest;
import shareyourstory.domain.moderation.dto.StaffMemberResponse;
import shareyourstory.domain.moderation.dto.UpdateStaffRequest;
import shareyourstory.domain.user.model.UserRole;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import shareyourstory.domain.moderation.service.ModerationService;
import shareyourstory.domain.user.model.User;
import shareyourstory.websocket.service.WebSocketService;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationService moderationService;
    private final WebSocketService webSocketService;

    public ModerationController(ModerationService moderationService, WebSocketService webSocketService) {
        this.moderationService = moderationService;
        this.webSocketService = webSocketService;
    }

    /** Cualquier usuario autenticado puede reportar una historia o un mensaje. */
    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User reporter) {
        if (request.storyId() == null && request.messageId() == null && request.privateMessageId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storyId, messageId o privateMessageId es obligatorio"));
        }
        if (request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason es obligatorio"));
        }
        if (request.reason().length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason supera el maximo de 500 caracteres"));
        }
        try {
            ReportResponse body = ReportResponse.from(moderationService.createReport(
                    request.storyId(), request.messageId(), request.privateMessageId(), request.reason(), reporter));
            webSocketService.broadcastNewReport();
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Todos los reportes (el front filtra por estado). Solo MOD/ADMIN. */
    @GetMapping("/reports")
    public List<ReportResponse> all() {
        return moderationService.allReports().stream().map(ReportResponse::from).toList();
    }

    /** Reportes pendientes + contador (via funcion almacenada). Solo MOD/ADMIN. */
    @GetMapping("/reports/pending")
    public ResponseEntity<?> pending() {
        List<ReportResponse> reports = moderationService.pendingReports().stream()
                .map(ReportResponse::from)
                .toList();
        return ResponseEntity.ok(Map.of(
                "count", moderationService.pendingCount(),
                "reports", reports));
    }

    /** Resuelve un reporte: action = "resolve" | "warn" | "dismiss". Solo MOD/ADMIN. */
    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Integer id,
            @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal User moderator) {
        if (moderator == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            moderationService.resolveReport(id, moderator.getId(), request.action());
            return ResponseEntity.ok(Map.of(
                    "reportId", id,
                    "action", request.action(),
                    "pendingRemaining", moderationService.pendingCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", rootMessage(e)));
        }
    }

    @GetMapping("/reports/{id}/audit")
    public ResponseEntity<?> audit(@PathVariable Integer id) {
        return ResponseEntity.ok(moderationService.auditFor(id));
    }

    // ── Miembros ─────────────────────────────────────────────────────────────

    @GetMapping("/members")
    public List<ModerationMemberResponse> members() {
        return moderationService.members();
    }

    /** Equipo de moderacion: moderadores y administradores. Solo MOD/ADMIN. */
    @GetMapping("/staff")
    public List<StaffMemberResponse> staff() {
        return moderationService.staff();
    }

    /** Edita un miembro del equipo. Solo ADMIN, y no la cuenta propia. */
    @PatchMapping("/staff/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Integer id,
            @RequestBody UpdateStaffRequest request,
            @AuthenticationPrincipal User caller) {
        if (caller == null || caller.getRole() != UserRole.ADMINISTRATOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (caller.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "No puedes editar tu propia cuenta desde aqui"));
        }
        try {
            return ResponseEntity.ok(moderationService.updateStaff(id, request));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Borra un miembro del equipo. Solo ADMIN, y no la cuenta propia. */
    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Integer id,
            @AuthenticationPrincipal User caller) {
        if (caller == null || caller.getRole() != UserRole.ADMINISTRATOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (caller.getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "No puedes borrar tu propia cuenta"));
        }
        try {
            moderationService.deleteStaff(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/members/{id}/warn")
    public ModerationMemberResponse warn(@PathVariable Integer id) {
        return moderationService.warnMember(id);
    }

    @PostMapping("/members/{id}/ban")
    public ModerationMemberResponse ban(@PathVariable Integer id) {
        return moderationService.banMember(id);
    }

    private String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
