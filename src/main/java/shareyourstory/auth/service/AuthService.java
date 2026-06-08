package shareyourstory.auth.service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.AnonymousRequest;
import shareyourstory.auth.dto.AuthResponse;
import shareyourstory.auth.dto.AuthUserResponse;
import shareyourstory.auth.dto.BootstrapAdminRequest;
import shareyourstory.auth.dto.BootstrapAdminResponse;
import shareyourstory.auth.dto.LoginRequest;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.RegisterRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.model.UserRole;
import shareyourstory.domain.user.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JWTService jwtService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    GoogleAuthService googleAuthService;

    // Challenges efimeros del login mod (paso 1 password -> paso 2 TOTP).
    // En memoria con TTL; se pierden al reiniciar (aceptable para el alcance).
    private static final long CHALLENGE_TTL_MS = 5 * 60 * 1000L;
    private final Map<String, ChallengeData> loginChallenges = new ConcurrentHashMap<>();

    private record ChallengeData(String email, long expiresAt) {}

    public AuthResponse anonymous(AnonymousRequest anonymousRequest) {
        if (anonymousRequest != null && anonymousRequest.anonToken() != null
                && jwtService.validateJwtToken(anonymousRequest.anonToken())) {
            String username = jwtService.getUsernameFromToken(anonymousRequest.anonToken());
            User existingUser = userRepository.findByUserName(username).orElse(null);
            if (existingUser != null && existingUser.getRole() == UserRole.ANON) {
                return toAuthResponse(existingUser);
            }
        }

        User anonymousUser = new User();
        String suffix = UUID.randomUUID().toString();
        anonymousUser.setUsername("anon-" + suffix);
        anonymousUser.setNickName("Anonimo-" + suffix.substring(0, 8));
        anonymousUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        anonymousUser.setRole(UserRole.ANON);

        userRepository.save(anonymousUser);
        return toAuthResponse(anonymousUser);
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Si hay anonToken valido, promovemos esa fila en vez de crear una nueva.
        String anonToken = registerRequest.anonToken();
        if (anonToken != null && jwtService.validateJwtToken(anonToken)) {
            String anonUsername = jwtService.getUsernameFromToken(anonToken);
            User existing = userRepository.findByUserName(anonUsername).orElse(null);
            if (existing != null && existing.getRole() == UserRole.ANON) {
                existing.setUsername(registerRequest.username());
                existing.setNickName(registerRequest.username());
                existing.setPassword(passwordEncoder.encode(registerRequest.password()));
                existing.setRole(UserRole.USER);
                userRepository.save(existing);
                return toAuthResponse(existing);
            }
        }

        User user = new User();
        user.setUsername(registerRequest.username());
        user.setNickName(registerRequest.username());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRole(UserRole.USER);

        userRepository.save(user);
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.username(), loginRequest.password()));

        User user = userRepository.findByUserName(loginRequest.username())
                .orElseThrow(() -> new NoSuchElementException("USER NOT FOUND"));

        if (isStaff(user)) {
            throw new BadCredentialsException("STAFF MUST USE LOGINMOD");
        }

        return toAuthResponse(user);
    }

    public boolean hasAdministrator() {
        return userRepository.existsByRole(UserRole.ADMINISTRATOR);
    }

    public BootstrapAdminResponse bootstrapAdministrator(BootstrapAdminRequest request) {
        if (hasAdministrator()) {
            throw new IllegalStateException("ADMIN_ALREADY_EXISTS");
        }

        User admin = new User();
        admin.setName(request.name());
        admin.setLastName(request.lastName());
        admin.setUsername(request.username());
        admin.setNickName(request.username());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setEmail(request.email());
        admin.setRole(UserRole.ADMINISTRATOR);
        admin.setSecretKey(googleAuthService.generateKey());
        admin.setTwoFactorEnabled(true);

        userRepository.save(admin);
        return new BootstrapAdminResponse(admin.getEmail(),
                googleAuthService.getQR(admin.getEmail(), admin.getSecretKey()));
    }

    public void registerMod(RegisterModRequest registerModRequest) {
        UserRole role = "ADMINISTRATOR".equals(registerModRequest.role()) ? UserRole.ADMINISTRATOR
                : UserRole.PROFESSIONAL;

        User newUserMod = new User();
        newUserMod.setName(registerModRequest.name());
        newUserMod.setLastName(registerModRequest.lastName());
        newUserMod.setUsername(registerModRequest.username());
        newUserMod.setNickName(registerModRequest.username());
        newUserMod.setPassword(passwordEncoder.encode(registerModRequest.password()));
        newUserMod.setEmail(registerModRequest.email());
        newUserMod.setCompany(registerModRequest.company());
        newUserMod.setProfession(registerModRequest.profession());
        newUserMod.setSpecialization(registerModRequest.specialization());
        newUserMod.setRole(role);
        newUserMod.setSecretKey(googleAuthService.generateKey());

        userRepository.save(newUserMod);
    }

    public String loginMod(LoginModRequest loginModRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginModRequest.email(), loginModRequest.password()));

        User user = userRepository.findByMail(loginModRequest.email())
                .orElseThrow(() -> new NoSuchElementException("USER NOT FOUND"));

        if (!isStaff(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sin permisos de moderacion");
        }
        if (!user.isTwoFactorEnabled() || user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "2FA no activado para este usuario");
        }

        String challengeId = UUID.randomUUID().toString();
        loginChallenges.put(challengeId,
                new ChallengeData(user.getEmail(), System.currentTimeMillis() + CHALLENGE_TTL_MS));
        return challengeId;
    }

    public String verifyModLogin(String challengeId, int code) {
        ChallengeData data = challengeId == null ? null : loginChallenges.get(challengeId);
        if (data == null || data.expiresAt() < System.currentTimeMillis()) {
            loginChallenges.remove(challengeId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Desafio de login invalido o expirado");
        }

        User user = userRepository.findByMail(data.email())
                .orElseThrow(() -> new NoSuchElementException("USER NOT FOUND"));

        if (user.getSecretKey() == null || !googleAuthService.isValid(user.getSecretKey(), code)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Codigo invalido");
        }

        loginChallenges.remove(challengeId);
        return jwtService.createToken(user);
    }

    public String manageUser2FA(String email) {
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        if (user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            user.setSecretKey(googleAuthService.generateKey());
            userRepository.save(user);
        }
        return googleAuthService.getQR(email, user.getSecretKey());
    }

    public void enableQR(String email, int code) {
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
        if (user.getSecretKey() == null || user.getSecretKey().isBlank()
                || !googleAuthService.isValid(user.getSecretKey(), code)) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Codigo invalido");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    public AuthResponse toAuthResponse(User user) {
        String token = jwtService.createToken(user);
        return new AuthResponse(token, new AuthUserResponse(String.valueOf(user.getUserId()),
                user.getUsername(), user.getRole().name()));
    }

    public boolean isStaff(User user) {
        return user.getRole() == UserRole.PROFESSIONAL || user.getRole() == UserRole.ADMINISTRATOR;
    }
}
