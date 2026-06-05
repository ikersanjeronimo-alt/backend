package shareyourstory.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

@Service
public class GoogleAuthService {

    @Autowired
    GoogleAuthenticator googleAuthenticator;

    public String generateKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String getQR(String email, String key) {
        String issuer = "ShareYourStory";

        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, email, key,
                issuer);
    }

    public boolean isValid(String secretKey, int code) {
        return googleAuthenticator.authorize(secretKey, code);
    }

}
