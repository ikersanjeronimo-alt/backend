package shareyourstory.auth.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import shareyourstory.auth.JWT.AuthTokenFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Origenes permitidos por CORS, separados por coma. Configurable por entorno
    // (CORS_ALLOWED_ORIGINS) para apuntar al dominio real del frontend en produccion.
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public AuthTokenFilter authTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        return new GoogleAuthenticator();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register/mod/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/auth/**").permitAll()
                        // Cuestionario de evento: ver/votar/responder = cualquier autenticado
                        // (anonimo incluido); crear/borrar = profesional/admin. DEBE ir antes
                        // del permitAll de GET /api/events/** para que gane el match mas especifico.
                        .requestMatchers(HttpMethod.GET, "/api/events/*/form").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/form/vote").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/form/response").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/form")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/form")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/interest").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/interest").authenticated()
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/professionals/**").authenticated()
                        .requestMatchers("/api/chats/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/communities")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/communities/*")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/communities/*")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/communities/*/pinned-note")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/communities/*/chat-closed")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/communities/*/members/*")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/communities/*/messages/*")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/events")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**")
                        .hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/moderation/reports")
                        .authenticated()
                        .requestMatchers("/api/moderation/**").hasAnyRole("PROFESSIONAL", "ADMINISTRATOR")
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(authTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // setAllowedOriginPatterns (en vez de setAllowedOrigins) permite comodines
        // como https://*.trycloudflare.com y es compatible con allowCredentials(true).
        configuration.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
