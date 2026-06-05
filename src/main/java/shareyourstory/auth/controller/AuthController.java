package shareyourstory.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.AnonymousRequest;
import shareyourstory.auth.dto.AuthResponse;
import shareyourstory.auth.dto.AuthUserResponse;
import shareyourstory.auth.dto.BootstrapAdminRequest;
import shareyourstory.auth.dto.BootstrapAdminResponse;
import shareyourstory.auth.dto.Get2faQRResponse;
import shareyourstory.auth.dto.LoginModChallengeResponse;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.LoginModWith2FAResponse;
import shareyourstory.auth.dto.LoginRequest;
import shareyourstory.auth.dto.VerifyLoginRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.auth.dto.RegisterRequest;
import shareyourstory.auth.dto.UpdateUsernameRequest;
import shareyourstory.auth.dto.ValidateQRRequest;
import shareyourstory.auth.service.AuthService;
import shareyourstory.auth.service.GoogleAuthService;
import shareyourstory.domain.community.service.CommunityService;
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

    @Autowired
    CommunityService communityService;

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
    public LoginModChallengeResponse loginMod(@Valid @RequestBody LoginModRequest loginModRequest) {
        String challengeId = authService.loginMod(loginModRequest);
        return new LoginModChallengeResponse(challengeId, true);
    }

    @PostMapping("/api/auth/login/mod/2fa/code")
    public LoginModWith2FAResponse validateLoginWithCode(@RequestBody VerifyLoginRequest request) {
        int code;
        try {
            code = Integer.parseInt(request.code() == null ? "" : request.code().trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo invalido");
        }
        String token = authService.verifyModLogin(request.challengeId(), code);
        return new LoginModWith2FAResponse(token);
    }

    @PatchMapping("/api/users/me/username")
    public ResponseEntity<AuthResponse> updateUsername(@AuthenticationPrincipal User user,
            @RequestBody UpdateUsernameRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setUsername(request.username());
        user.setNickName(request.username());
        userRepository.save(user);
        return ResponseEntity.ok(authService.toAuthResponse(user));
    }

    /**
     * Devuelve el usuario de la sesion actual (a partir del JWT). El front lo usa
     * para RESTAURAR la sesion al recargar, en vez de pedir siempre identidad
     * anonima (que degradaba a cualquier usuario logueado a anonimo).
     */
    @GetMapping("/api/users/me")
    public AuthUserResponse me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return new AuthUserResponse(String.valueOf(user.getUserId()), user.getUsername(),
                user.getRole().name());
    }

    @GetMapping("/api/testJWT")
    public String testJwt() {
        return "HI YOU ARE LOGED";
    }
}
