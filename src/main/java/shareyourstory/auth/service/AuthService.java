package shareyourstory.auth.service;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.apache.catalina.connector.Response;
import org.springframework.security.authentication.BadCredentialsException;
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

    public AuthService() {}

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

    public int registerMod(RegisterModRequest registerModRequest) {
        User newUserMod = new User();

        UserRole role = "ADMINISTRATOR".equals(registerModRequest.role()) ? UserRole.ADMINISTRATOR
                : UserRole.PROFESSIONAL;

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

        try {
            userRepository.save(newUserMod);
            return Response.SC_CREATED;
        } catch (Exception e) {
            System.out.println("ERROR WHILE CREATING NEW MOD USER");
            return Response.SC_CONFLICT;
        }
    }

    public int loginMod(LoginModRequest loginModRequest) {
        if (authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginModRequest.email(), loginModRequest.password())) != null) {
            User user = userRepository.findByMail(loginModRequest.email())
                    .orElseThrow(() -> new NoSuchElementException("USER NOT FOUND"));

            if (!isStaff(user)) {
                return Response.SC_FORBIDDEN;
            }

            if (!user.isTwoFactorEnabled() || user.getSecretKey() == null || user.getSecretKey().isBlank()) {
                return Response.SC_FORBIDDEN;
            }

            return Response.SC_ACCEPTED;
        } else {
            return Response.SC_NOT_ACCEPTABLE;
        }
    }

    public String manageUser2FA(String email) {
        try {
            User user = userRepository.findByMail(email).get();

            if (!user.isTwoFactorEnabled()) {
                if (user.getSecretKey() == null || user.getSecretKey().isBlank()) {
                    user.setSecretKey(googleAuthService.generateKey());
                    userRepository.save(user);
                }

                return googleAuthService.getQR(email, user.getSecretKey());
            } else {
                return "Already enabled";
            }

        } catch (NoSuchElementException e) {
            return "USER NOT FOUND";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public int enableQR(String email, int code) {
        try {
            User user = userRepository.findByMail(email).get();

            if (user.getSecretKey() != null && !user.getSecretKey().isBlank()
                    && googleAuthService.isValid(user.getSecretKey(), code)) {
                user.setTwoFactorEnabled(true);
                userRepository.save(user);
                return Response.SC_ACCEPTED;
            } else {
                return Response.SC_NOT_ACCEPTABLE;
            }
        } catch (NoSuchElementException e) {
            return Response.SC_BAD_REQUEST;
        }
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
