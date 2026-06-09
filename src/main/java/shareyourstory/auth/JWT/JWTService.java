package shareyourstory.auth.JWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import shareyourstory.domain.user.model.User;


@Service
public class JWTService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    // Ventana de gracia (ms) tras caducar el token durante la cual
    // getClaimsForRefresh acepta el token recien expirado. Default 15 min.
    @Value("${security.jwt.refresh-grace:900000}")
    private long refreshGrace;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(User user) {
        return Jwts.builder().subject(user.getUsername())
                .claim("id", String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException
                | UnsupportedJwtException | IllegalArgumentException ignored) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Devuelve los claims de un token con FIRMA valida para poder renovar la
     * sesion, en dos casos:
     *   - token aun no expirado (firma valida) -> renovacion normal.
     *   - token recien expirado, dentro de la ventana de gracia (refreshGrace):
     *     jjwt verifica la firma ANTES de comprobar la expiracion y entrega los
     *     claims en ExpiredJwtException.getClaims().
     * Devuelve null si la firma es invalida, el token esta corrupto, o caduco
     * hace mas tiempo que la ventana de gracia.
     */
    public Claims getClaimsForRefresh(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            Date exp = claims.getExpiration();
            if (exp != null && exp.getTime() + refreshGrace >= System.currentTimeMillis()) {
                return claims;
            }
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

}
