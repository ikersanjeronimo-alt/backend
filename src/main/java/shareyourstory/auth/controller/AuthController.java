package shareyourstory.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.AnonymousRequest;
import shareyourstory.auth.dto.AuthResponse;
import shareyourstory.auth.dto.BootstrapAdminRequest;
import shareyourstory.auth.dto.BootstrapAdminResponse;
import shareyourstory.auth.dto.Get2faQRResponse;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.LoginModWith2FAResponse;
import shareyourstory.auth.dto.LoginRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.auth.dto.RegisterRequest;
import shareyourstory.auth.dto.UpdateUsernameRequest;
import shareyourstory.auth.dto.ValidateQRRequest;
import shareyourstory.auth.service.AuthService;
import shareyourstory.auth.service.GoogleAuthService;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.repository.UserRepository;
import shareyourstory.domain.user.service.UserService;

@RestController
public class AuthController {
    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @Autowired
    GoogleAuthService googleAuthService;

    @Autowired
    JWTService jwtService;

    @PostMapping("/api/auth/anonymous")
    public AuthResponse anonymous(@RequestBody(required = false) AnonymousRequest anonymousRequest) {
        return authService.anonymous(anonymousRequest);
    }

    @PostMapping("/api/auth/register")
    public AuthResponse register(@RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/api/auth/register/mod")
    public ResponseEntity<Void> registerMod(@AuthenticationPrincipal User user,
            @RequestBody RegisterModRequest registerModRequest) {
        if (user == null || user.getRole() != shareyourstory.domain.user.model.UserRole.ADMINISTRATOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        int status = authService.registerMod(registerModRequest);
        return ResponseEntity.status(status).build();
    }

    @PostMapping("/api/auth/register/admin/bootstrap")
    public ResponseEntity<?> bootstrapAdministrator(@RequestBody BootstrapAdminRequest request) {
        if (authService.hasAdministrator()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("ADMIN_ALREADY_EXISTS");
        }

        try {
            BootstrapAdminResponse response = authService.bootstrapAdministrator(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/api/auth/register/mod/2fa/qr")
    public Get2faQRResponse get2faQR(@RequestParam String email) {
        String otpauthUri = authService.manageUser2FA(email);
        return new Get2faQRResponse(otpauthUri);
    }

    @PostMapping("/api/auth/register/mod/2fa/qr")
    public ResponseEntity<Void> enableQR(@RequestBody ValidateQRRequest validateLoginRequest) {
        int status = authService.enableQR(validateLoginRequest.email(), validateLoginRequest.code());
        return ResponseEntity.status(status).build();
    }

    @PostMapping("/api/auth/login/mod")
    public ResponseEntity<Void> loginMod(@RequestBody LoginModRequest loginModRequest) {
        int status = authService.loginMod(loginModRequest);
        if (status == HttpStatus.ACCEPTED.value()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(status).build();
    }

    @PostMapping("/api/auth/login/mod/2fa/code")
    public ResponseEntity<?> validateLoginWithCode(@RequestBody ValidateQRRequest validateLoginRequest) {
        User user = (User) userService.loadUserByUsername(validateLoginRequest.email());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Usuario no encontrado");
        }

        if (!authService.isStaff(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario sin permisos de moderacion");
        }

        if (!user.isTwoFactorEnabled() || user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("2FA no activado para este usuario");
        }

        if (googleAuthService.isValid(user.getSecretKey(), validateLoginRequest.code())) {
            String token = jwtService.createToken(user);
            return ResponseEntity.ok(new LoginModWith2FAResponse(token));
        }

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Codigo invalido");
    }

    @PatchMapping("/api/users/me/username")
    public ResponseEntity<Void> updateUsername(@AuthenticationPrincipal User user,
            @RequestBody UpdateUsernameRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setUsername(request.username());
        user.setNickName(request.username());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/testJWT")
    public String testJwt() {
        return "HI YOU ARE LOGED";
    }
}
