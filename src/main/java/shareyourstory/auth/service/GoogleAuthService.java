package shareyourstory.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import shareyourstory.domain.user.model.User;
import shareyourstory.domain.user.repository.UserRepository;

@Service
public class GoogleAuthService {

    @Autowired
    GoogleAuthenticator googleAuthenticator;

    @Autowired
    GoogleAuthenticatorQRGenerator googleAuthenticatorQRGenerator;

    @Autowired
    UserRepository userRepository;

    public String generateKey(String email) {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();

        return key.getKey();
    }

    public String getQR(String email) {
        User user = null;
        String issuer = "ShareYourStory";

        try {
            user = userRepository.findByEmail(email).get();

            return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, email,
                    user.getSecretKey(), issuer);
        } catch (Exception e) {
            return "NOT USER FOUND";
        }
    }

    public boolean isValid(String secretKey, int code) {
        return googleAuthenticator.authorize(secretKey, code);
    }

}
