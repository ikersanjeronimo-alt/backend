package shareyourstory.auth.config;

import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import shareyourstory.auth.controller.RateLimitService;

/**
 * Limita la frecuencia de los endpoints sensibles (login/2FA, registro, envio de
 * correo y escritura de contenido). Se limita por IP y, si hay token, tambien por
 * token: se rechaza (429) en cuanto cualquiera de los dos cubos se agota.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitService rateLimitService;

    private record Rule(String name, int capacity, Duration period) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Rule rule = ruleFor(request.getMethod(), request.getRequestURI());
        if (rule != null) {
            String ip = clientIp(request);
            boolean allowed = rateLimitService.tryConsume(
                    rule.name() + ":ip:" + ip, rule.capacity(), rule.period());

            String token = bearerToken(request);
            if (allowed && token != null) {
                allowed = rateLimitService.tryConsume(
                        rule.name() + ":tok:" + token, rule.capacity(), rule.period());
            }

            if (!allowed) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Demasiadas peticiones. Intentalo de nuevo en un minuto.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Rule ruleFor(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        switch (path) {
            case "/api/auth/login/mod/2fa/code":
                return new Rule("2fa", 5, Duration.ofMinutes(1));
            case "/api/auth/login":
            case "/api/auth/login/mod":
                return new Rule("login", 10, Duration.ofMinutes(1));
            case "/api/auth/refresh":
                return new Rule("refresh", 20, Duration.ofMinutes(1));
            case "/api/auth/register":
                return new Rule("register", 20, Duration.ofMinutes(1));
            case "/api/auth/anonymous":
                // La app crea/restaura identidad anonima en CADA carga sin token,
                // asi que el limite ha de ser holgado; 20/min provocaba 429 al
                // recargar (el front ahora ademas reintenta con backoff).
                return new Rule("anonymous", 60, Duration.ofMinutes(1));
            case "/api/timeMachine":
            case "/api/letters":
                return new Rule("mail", 5, Duration.ofMinutes(1));
            case "/api/bottles":
            case "/api/stories":
                return new Rule("content", 30, Duration.ofMinutes(1));
            default:
                if (path.endsWith("/messages")) {
                    return new Rule("content", 30, Duration.ofMinutes(1));
                }
                return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
