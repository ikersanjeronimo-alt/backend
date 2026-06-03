package shareyourstory.domain.user.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.user.dto.ProfessionalResponse;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;

/**
 * Listado publico de profesionales (usuarios con rol PROFESSIONAL) para el front.
 * Mapea a un DTO seguro: no expone password ni secretKey.
 */
@RestController
public class ProfessionalController {

    @Autowired
    UserRepository userRepository;

    @GetMapping("/api/professionals")
    public List<ProfessionalResponse> getProfessionals() {
        return userRepository.findByRole(UserRole.PROFESSIONAL).stream()
                .map(this::toDto)
                .toList();
    }

    private ProfessionalResponse toDto(User u) {
        String specialty = mapSpecialty(u.getProfession());
        List<String> tags = (u.getSpecialization() == null || u.getSpecialization().isBlank())
                ? List.of()
                : List.of(u.getSpecialization());
        return new ProfessionalResponse(
                String.valueOf(u.getUserId()),
                buildName(u),
                specialty,
                tags,
                "today",
                null,
                null);
    }

    private String buildName(User u) {
        if (u.getName() != null && !u.getName().isBlank()) {
            String last = u.getLastName() == null ? "" : (" " + u.getLastName());
            return (u.getName() + last).trim();
        }
        return u.getUsername();
    }

    private String mapSpecialty(String profession) {
        if (profession == null) {
            return "psicologo";
        }
        String p = profession.toLowerCase();
        if (p.startsWith("terapeuta")) {
            return "terapeuta";
        }
        if (p.startsWith("psiquiatra")) {
            return "psiquiatra";
        }
        return "psicologo";
    }
}
