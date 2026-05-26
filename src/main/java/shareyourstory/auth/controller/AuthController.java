package shareyourstory.auth.controller;

import org.springframework.web.bind.annotation.RestController;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.auth.dto.ValidateLoginRequest;
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
        return googleAuthService.getQR(email);
    }

    @PostMapping("/api/auth/register/mod/2fa/qr")
    public int validateQR(@RequestBody String email, int code) {
        User user = (User) userService.loadUserByUsername(email);
        if (user != null) {
            if (googleAuthService.isValid(user.getSecretKey(), code)) {
                return Response.SC_ACCEPTED;
            } else {
                return Response.SC_NOT_ACCEPTABLE;
            }
        } else {
            return Response.SC_NOT_FOUND;
        }
    }


    // LOGIN MOD ENDPOINTS
    @PostMapping("/api/auth/login/mod")
    public int loginMod(@RequestBody LoginModRequest loginModRequest) {
        return authService.loginMod(loginModRequest);
    }

    @PostMapping("/api/auth/login/mod/2fa/code")
    public String validateLoginWithCode(@RequestBody ValidateLoginRequest validateLoginRequest) {
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

    // @PostMapping("/api/auth/login")
    // public String loginMod(@RequestBody LoginModRequest loginRequest) {
    // return authService.login(loginRequest);
    // }

    // @PostMapping("/api/auth/login/A2F")
    // public String doubleFactorAuthenticator(@RequestBody LoginRequest loginRequest) {

    // if (googleAuthService.isValid(googleAuthService.getSecretKey(loginRequest.email()),
    // Integer.valueOf(loginRequest.password()))) {
    // return "tkn";

    // } else {
    // return "false";
    // }
    // }


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
