package shareyourstory.auth.dto;

public record AuthResponse(String token, AuthUserResponse user) {
}
