package shareyourstory.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterModRequest(@NotBlank(message = "Name is mandatory") String name,
        @NotBlank(message = "Lastname is mandatory") String lastName,
        @NotBlank(message = "Username is mandatory") String username,
        @NotBlank(message = "Password is mandataory") @Size(min = 8) String password,
        @Email @NotBlank(message = "Email is mandatory") String email, String company) {
}
