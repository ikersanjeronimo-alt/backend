package shareyourstory.domain.user.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.model.CommunityMember;
import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.community.repository.CommunityMemberRepository;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.user.dto.DashboardMessageResponse;
import shareyourstory.domain.user.dto.ModProfileResponse;
import shareyourstory.domain.user.dto.ProfileResponse;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;

/**
 * Endpoints "de mi cuenta". Todo se resuelve a partir del usuario autenticado
 * (@AuthenticationPrincipal); ningun identificador viene del cliente.
 */
@RestController
@RequestMapping("/api/users/me")
public class UsersMeController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    CommunityMessageRepository communityMessageRepository;
    @Autowired
    CommunityMemberRepository memberRepository;
    @Autowired
    CommunityRepository communityRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal User me) {
        require(me);
        int messages = (int) communityMessageRepository.countByUserId(me.getUserId());
        int communities = (int) memberRepository.countByUserId(me.getUserId());
        return new ProfileResponse(
                me.getUsername(),
                frontendRole(me.getRole()),
                me.getCreationDate() == null ? "" : me.getCreationDate().toString(),
                new ProfileResponse.Stats(messages, communities, 0, 0, 0),
                List.of(),
                parseTopics(me.getTopics()));
    }

    @GetMapping("/mod-profile")
    public ModProfileResponse modProfile(@AuthenticationPrincipal User me) {
        require(me);
        return new ModProfileResponse(
                nz(me.getName()), nz(me.getLastName()), me.getUsername(), nz(me.getEmail()), me.getCompany());
    }

    @PatchMapping("/mod-profile")
    public ResponseEntity<Void> updateModProfile(@AuthenticationPrincipal User me,
            @RequestBody Map<String, Object> body) {
        require(me);
        if (body.containsKey("name")) me.setName(str(body.get("name")));
        if (body.containsKey("lastName")) me.setLastName(str(body.get("lastName")));
        if (body.containsKey("email")) me.setEmail(str(body.get("email")));
        if (body.containsKey("company")) me.setCompany(str(body.get("company")));
        userRepository.save(me);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User me,
            @RequestBody Map<String, Object> body) {
        require(me);
        String current = str(body.get("currentPassword"));
        String next = str(body.get("newPassword"));
        if (next == null || next.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contrasena debe tener al menos 8 caracteres");
        }
        if (current == null || !passwordEncoder.matches(current, me.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contrasena actual no es correcta");
        }
        me.setPassword(passwordEncoder.encode(next));
        userRepository.save(me);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/onboarding")
    public ResponseEntity<Void> onboarding(@AuthenticationPrincipal User me,
            @RequestBody Map<String, Object> body) {
        require(me);
        Object topics = body.get("topics");
        if (topics instanceof List<?> list) {
            me.setTopics(list.stream().map(String::valueOf).collect(Collectors.joining(",")));
            userRepository.save(me);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * El front no carga los ajustes del servidor (idioma/tema viven en
     * localStorage por dispositivo). Se acepta para no romper el UI.
     */
    @PatchMapping("/settings")
    public ResponseEntity<Void> settings(@AuthenticationPrincipal User me,
            @RequestBody(required = false) Map<String, Object> body) {
        require(me);
        return ResponseEntity.noContent().build();
    }

    /**
     * No hay pantalla que muestre el historial de estado de animo; se acepta el
     * envio sin persistir.
     */
    @PostMapping("/mood")
    public ResponseEntity<Void> mood(@AuthenticationPrincipal User me,
            @RequestBody(required = false) Map<String, Object> body) {
        require(me);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/messages")
    public List<DashboardMessageResponse> dashboardMessages(@AuthenticationPrincipal User me) {
        require(me);
        List<DashboardMessageResponse> out = new ArrayList<>();
        for (CommunityMember m : memberRepository.findByUserId(me.getUserId())) {
            Community c = communityRepository.findById(m.getCommunityId()).orElse(null);
            if (c == null) {
                continue;
            }
            CommunityMessage last = communityMessageRepository
                    .findTopByCommunityIdOrderByCreatedAtDesc(c.getId().intValue());
            if (last == null) {
                continue;
            }
            out.add(new DashboardMessageResponse(
                    String.valueOf(last.getId()), String.valueOf(c.getId()), c.getName(),
                    last.getUsername(), last.getText(),
                    last.getCreatedAt() == null ? "" : last.getCreatedAt().format(HHMM)));
        }
        return out;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void require(User me) {
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
    }

    private String frontendRole(UserRole role) {
        return switch (role) {
            case PROFESSIONAL -> "MODERATOR";
            case ADMINISTRATOR -> "ADMIN";
            default -> role.name();
        };
    }

    private List<String> parseTopics(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
