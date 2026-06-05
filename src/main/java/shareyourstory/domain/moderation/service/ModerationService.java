package shareyourstory.domain.moderation.service;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shareyourstory.domain.community.model.Community;
import shareyourstory.domain.community.model.CommunityMessage;
import shareyourstory.domain.bottle.model.PrivateMessage;
import shareyourstory.domain.bottle.repository.PrivateMessageRepository;
import shareyourstory.domain.community.repository.CommunityMessageRepository;
import shareyourstory.domain.community.repository.CommunityRepository;
import shareyourstory.domain.moderation.dto.ModerationMemberResponse;
import shareyourstory.domain.moderation.dto.StaffMemberResponse;
import shareyourstory.domain.moderation.dto.UpdateStaffRequest;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.model.ReportAudit;
import shareyourstory.domain.moderation.model.ReportStatus;
import shareyourstory.domain.moderation.model.ReportTargetType;
import shareyourstory.domain.moderation.repository.ReportAuditRepository;
import shareyourstory.domain.moderation.repository.ReportRepository;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;

@Service
public class ModerationService {

    @Autowired
    ReportRepository reportRepository;
    @Autowired
    StoryMapRepository storyMapRepository;
    @Autowired
    ReportAuditRepository reportAuditRepository;
    @Autowired
    CommunityMessageRepository communityMessageRepository;
    @Autowired
    CommunityRepository communityRepository;
    @Autowired
    PrivateMessageRepository privateMessageRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    shareyourstory.websocket.service.WebSocketService webSocketService;
    @Autowired
    shareyourstory.domain.community.service.CommunityMessageService communityMessageService;
    @Autowired
    shareyourstory.domain.bottle.service.PrivateMessageService privateMessageService;
    @Autowired
    shareyourstory.websocket.service.UserPresenceService userPresenceService;

    public List<ReportAudit> auditFor(Integer reportId) {
        return reportAuditRepository.findByReportIdOrderByCreatedAtDesc(reportId);
    }

    /** Crea un reporte sobre una historia, un mensaje de comunidad o uno privado. */
    public Report createReport(Integer storyId, Long messageId, Long privateMessageId, String reason, User reporter) {
        Report report = new Report();
        report.setReason(reason);
        report.setStatus(ReportStatus.PENDING);
        if (reporter != null) {
            report.setReporterId(reporter.getUserId());
            report.setReporterUsername(reporter.getUsername());
        }

        if (privateMessageId != null) {
            PrivateMessage pm = privateMessageRepository.findById(privateMessageId)
                    .orElseThrow(() -> new NoSuchElementException("El mensaje privado " + privateMessageId + " no existe"));
            report.setTargetType(ReportTargetType.PRIVATE_MESSAGE);
            report.setMessageId(privateMessageId);
            report.setContent(pm.getText());
            Integer authorId = "professional".equals(pm.getFrom()) ? pm.getProfessionalId() : pm.getUserId();
            report.setReportedUsername(userRepository.findById(authorId)
                    .map(u -> u.getUsername()).orElse("usuario"));
            report.setCommunity("Chat privado");
        } else if (storyId != null) {
            StoryMap story = storyMapRepository.findById(storyId)
                    .orElseThrow(() -> new NoSuchElementException("La historia " + storyId + " no existe"));
            report.setTargetType(ReportTargetType.STORY);
            report.setStory(story);
            report.setContent(story.getMessage());
            report.setReportedUsername("anonimo");
            report.setCommunity("Mapa");
        } else if (messageId != null) {
            CommunityMessage msg = communityMessageRepository.findById(messageId)
                    .orElseThrow(() -> new NoSuchElementException("El mensaje " + messageId + " no existe"));
            report.setTargetType(ReportTargetType.MESSAGE);
            report.setMessageId(messageId);
            report.setContent(msg.getText());
            report.setReportedUsername(msg.getUsername());
            Community c = communityRepository.findById(msg.getCommunityId().longValue()).orElse(null);
            report.setCommunity(c != null ? c.getName() : null);
        } else {
            throw new IllegalArgumentException("Debe indicarse storyId o messageId");
        }

        return reportRepository.save(report);
    }

    public List<Report> allReports() {
        return reportRepository.findAll();
    }

    public List<Report> pendingReports() {
        return reportRepository.findByStatusWithRelations(ReportStatus.PENDING);
    }

    public long pendingCount() {
        return reportRepository.countPendingReportsViaFunction();
    }

    /**
     * Resuelve un reporte de forma atomica. action: "resolve" | "warn" | "dismiss".
     * resolve y warn marcan el reporte como RESOLVED (via el procedimiento
     * almacenado) y sanean el contenido; warn ademas incrementa los avisos del
     * autor. dismiss solo cambia el estado a DISMISSED.
     */
    @Transactional
    public void resolveReport(Integer reportId, Integer moderatorId, String action) {
        String act = action == null ? "" : action.trim().toLowerCase();
        String spAction = act.equals("dismiss") ? "DISMISSED" : "RESOLVED";

        // 1) Procedimiento almacenado: actualiza la tabla `reports`.
        reportRepository.resolveReport(reportId, moderatorId, spAction);

        if (!spAction.equals("RESOLVED")) {
            return;
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("El reporte " + reportId + " no existe"));

        // 2) Sanear el contenido infractor.
        //    Historia del mapa: se BORRA por completo (no se enmascara). El
        //    snapshot `content` del reporte conserva el texto original para el
        //    panel. Antes hay que desligar todos los reportes que apuntan a esa
        //    historia (FK story_id) para no violar la restriccion al borrarla.
        if (report.getTargetType() == ReportTargetType.STORY && report.getStory() != null) {
            StoryMap story = report.getStory();
            Integer storyId = story.getId();

            List<Report> referencing = reportRepository.findByStory_Id(storyId);
            referencing.forEach(r -> r.setStory(null));
            reportRepository.saveAll(referencing);
            reportRepository.flush();

            storyMapRepository.delete(story);
            storyMapRepository.flush();

            webSocketService.broadcastDeletedStoryMap(storyId);
        } else if (report.getTargetType() == ReportTargetType.MESSAGE && report.getMessageId() != null) {
            // Borra el mensaje por completo (igual que el delete del moderador) y
            // difunde el DELETE por WS para que desaparezca en vivo en todas las
            // sesiones del chat. El snapshot `content` del reporte conserva el texto.
            communityMessageRepository.findById(report.getMessageId()).ifPresent(m ->
                    communityMessageService.deleteMessage(m.getCommunityId(), report.getMessageId()));
        } else if (report.getTargetType() == ReportTargetType.PRIVATE_MESSAGE && report.getMessageId() != null) {
            // Borra el mensaje privado y difunde el DELETE a ambos participantes.
            privateMessageService.deleteMessage(report.getMessageId());
        }

        // 3) "Avisar": incrementa el contador de avisos del autor reportado.
        if (act.equals("warn") && report.getReportedUsername() != null) {
            userRepository.findByUserName(report.getReportedUsername()).ifPresent(u -> {
                u.setWarnings(u.getWarnings() + 1);
                userRepository.save(u);
            });
        }
    }

    // ── Miembros ─────────────────────────────────────────────────────────────

    public List<ModerationMemberResponse> members() {
        return userRepository.findByRole(UserRole.USER).stream()
                .map(this::toMember)
                .toList();
    }

    /** Todo el equipo: moderadores (PROFESSIONAL) y administradores (ADMINISTRATOR). */
    public List<StaffMemberResponse> staff() {
        return userRepository.findByRoleIn(List.of(UserRole.PROFESSIONAL, UserRole.ADMINISTRATOR)).stream()
                .map(this::toStaff)
                .toList();
    }

    /** Edita campos basicos de un miembro del equipo (mod/admin). */
    public StaffMemberResponse updateStaff(Integer userId, UpdateStaffRequest req) {
        User u = requireStaff(userId);
        if (req.name() != null) {
            u.setName(req.name().trim());
        }
        if (req.email() != null) {
            u.setMail(req.email().trim());
        }
        if (req.company() != null) {
            u.setCompanyName(req.company().trim());
        }
        if (req.profession() != null) {
            u.setProfession(req.profession().trim());
        }
        userRepository.save(u);
        return toStaff(u);
    }

    /** Borra un miembro del equipo (mod/admin). */
    public void deleteStaff(Integer userId) {
        User u = requireStaff(userId);
        userRepository.delete(u);
    }

    private User requireStaff(Integer userId) {
        User u = requireUser(userId);
        if (u.getRole() != UserRole.PROFESSIONAL && u.getRole() != UserRole.ADMINISTRATOR) {
            throw new NoSuchElementException("El usuario " + userId + " no es moderador ni administrador");
        }
        return u;
    }

    private StaffMemberResponse toStaff(User u) {
        return new StaffMemberResponse(
                String.valueOf(u.getUserId()),
                u.getName(),
                u.getUsername(),
                u.getMail(),
                u.getRole().name(),
                u.getCompanyName(),
                u.getProfession(),
                u.getCreationDate() == null ? "" : u.getCreationDate().toString(),
                userPresenceService.isOnline(u.getUsername()));
    }

    public ModerationMemberResponse warnMember(Integer userId) {
        User u = requireUser(userId);
        u.setWarnings(u.getWarnings() + 1);
        userRepository.save(u);
        return toMember(u);
    }

    public ModerationMemberResponse banMember(Integer userId) {
        User u = requireUser(userId);
        u.setBanned(true);
        userRepository.save(u);
        return toMember(u);
    }

    private User requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Usuario " + userId + " no encontrado"));
    }

    private ModerationMemberResponse toMember(User u) {
        return new ModerationMemberResponse(
                String.valueOf(u.getUserId()),
                u.getUsername(),
                "",
                u.getCreationDate() == null ? "" : u.getCreationDate().toString(),
                (int) reportRepository.countByReportedUsername(u.getUsername()),
                u.isBanned());
    }
}
