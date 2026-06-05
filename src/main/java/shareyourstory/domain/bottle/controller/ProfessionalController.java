package shareyourstory.domain.bottle.controller;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.websocket.service.ProfessionalDTO;

@RestController
public class ProfessionalController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/api/professionals")
    public List<ProfessionalDTO> getProfessionals() {
        return userRepository.findByRole(UserRole.PROFESSIONAL).stream()
            .filter(user -> !isBlank(user.getProfession()) && !isBlank(user.getSpecialization()))
            .map(this::toDto)
            .toList();
    }

    private ProfessionalDTO toDto(User user) {
        List<String> tags = new ArrayList<>();
        tags.add(user.getSpecialization());
        if (!isBlank(user.getCompanyName())) {
            tags.add(user.getCompanyName());
        }

        String profession = user.getProfession();
        String specialization = user.getSpecialization();
        String bio = "Profesional de " + profession + " especializado/a en " + specialization + ".";

        return new ProfessionalDTO(
            String.valueOf(user.getUserId()),
            displayName(user),
            normalizeSpecialty(profession),
            tags,
            "now",
            null,
            bio
        );
    }

    private String displayName(User user) {
        String first = user.getName() == null ? "" : user.getName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getUsername() : full;
    }

    private String normalizeSpecialty(String profession) {
        String normalized = Normalizer.normalize(profession == null ? "" : profession, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
        if (normalized.contains("psicologo")) {
            return "psicologo";
        }
        if (normalized.contains("psiquiatra")) {
            return "psiquiatra";
        }
        if (normalized.contains("terapeuta")) {
            return "terapeuta";
        }
        return normalized.isBlank() ? "terapeuta" : normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
