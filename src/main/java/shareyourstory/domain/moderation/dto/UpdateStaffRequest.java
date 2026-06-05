package shareyourstory.domain.moderation.dto;

/**
 * Campos editables de un miembro del equipo desde el panel de moderacion.
 * Todos opcionales: solo se aplican los no nulos. El rol y el username no se
 * editan aqui.
 */
public record UpdateStaffRequest(
        String name,
        String email,
        String company,
        String profession) {
}
