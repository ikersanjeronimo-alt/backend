package shareyourstory.auth.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.auth.dto.ValidateQRRequest;
import shareyourstory.auth.service.AuthService;
import shareyourstory.auth.service.GoogleAuthService;
import shareyourstory.domain.timeMachine.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.service.UserService;

@RestController
public class AuthController {
    @Autowired
    UserService userService;

    @Autowired
    AuthService authService;

    @Autowired
    GoogleAuthService googleAuthService;

    @Autowired
    EmailService emailService;

    @Autowired
    JWTService jwtService;

    // REGISTER MOD ENDPOINTS
    @PostMapping("/api/auth/register/mod")
    public int registerMod(@RequestBody RegisterModRequest registerModRequest) {
        return authService.registerMod(registerModRequest);
    }

    @GetMapping("/api/auth/register/mod/2fa/qr")
    public String get2faQR(@RequestParam String email) {
        return authService.manageUser2FA(email);
    }

    @PostMapping("/api/auth/register/mod/2fa/qr")
    public int enableQR(@RequestBody ValidateQRRequest validateLoginRequest) {
        return authService.enableQR(validateLoginRequest.email(), validateLoginRequest.code());
    }

    // LOGIN MOD ENDPOINTS
    @PostMapping("/api/auth/login/mod")
    public int loginMod(@RequestBody LoginModRequest loginModRequest) {
        return authService.loginMod(loginModRequest);
    }

    @PostMapping("/api/auth/login/mod/2fa/code")
    public String validateLoginWithCode(@RequestBody ValidateQRRequest validateLoginRequest) {
        String tkn = null;
        User user = (User) userService.loadUserByUsername(validateLoginRequest.email());

        if (user != null) {
            if (googleAuthService.isValid(user.getSecretKey(), validateLoginRequest.code())) {
                tkn = jwtService.createToken(user);
                return tkn;
            } else {
                return tkn;
            }
        } else {
            return tkn;
        }
    }

    @GetMapping("/api/testJWT")
    public String getMethodName(@RequestParam String param) {
        System.out.println("asdasd");
        return "HI YOU ARE LOGED";
    }

    @PostMapping("/api/mailTest")
    public String mailTest(@RequestBody String entity) {
        emailService.send();
        return entity;
    }

}
