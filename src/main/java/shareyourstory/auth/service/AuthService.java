package shareyourstory.auth.service;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import shareyourstory.auth.JWT.JWTService;
import shareyourstory.auth.dto.LoginModRequest;
import shareyourstory.auth.dto.RegisterModRequest;
import shareyourstory.domain.user.model.User;
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

    public int registerMod(RegisterModRequest registerModRequest) {
        User newUserMod = new User();

        newUserMod.setUsername(registerModRequest.username());
        newUserMod.setPassword(passwordEncoder.encode(registerModRequest.password()));
        newUserMod.setEmail(registerModRequest.email());
        newUserMod.setCompany(registerModRequest.company());
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
            return Response.SC_OK;
        } else {
            return Response.SC_NOT_ACCEPTABLE;
        }
    }

    public String login(LoginModRequest loginRequest) {
        if (authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.email(), loginRequest.password())) == null) {
            return "ERROR";
        }

        return "tkn";
    }
}
