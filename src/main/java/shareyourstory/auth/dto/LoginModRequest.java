package shareyourstory.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginModRequest(@NotBlank(message = "You must send email parameter") String email,
        @NotBlank(message = "You must send the password") String password) {
}
