package shareyourstory.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUsernameRequest(@NotBlank String username) {
}
