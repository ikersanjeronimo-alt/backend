package shareyourstory.domain.user.dto;

import java.util.List;

/**
 * Vista publica de un profesional (usuario con rol PROFESSIONAL) para el
 * listado del front. No expone password/secretKey ni datos sensibles.
 */
public record ProfessionalResponse(
        String id,
        String name,
        String specialty,
        List<String> tags,
        String availability,
        String availableAt,
        String bio) {
}
