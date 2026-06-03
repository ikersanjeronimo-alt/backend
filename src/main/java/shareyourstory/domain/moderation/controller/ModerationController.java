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
import shareyourstory.domain.moderation.service.ModerationService;
import shareyourstory.domain.user.model.User;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /** Cualquier usuario autenticado puede reportar una historia o un mensaje. */
    @PostMapping("/reports")
    public ResponseEntity<?> createReport(@RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User reporter) {
        if (request.storyId() == null && request.messageId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "storyId o messageId es obligatorio"));
        }
        if (request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason es obligatorio"));
        }
        if (request.reason().length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason supera el maximo de 500 caracteres"));
        }
        try {
            ReportResponse body = ReportResponse.from(moderationService.createReport(
                    request.storyId(), request.messageId(), request.reason(), reporter));
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
