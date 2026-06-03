package shareyourstory.config;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de excepciones para toda la API.
 *
 * Traduce las excepciones del framework a codigos HTTP coherentes (antes
 * acababan en 500 genericos porque no habia ningun @ControllerAdvice).
 *
 * OJO: a proposito NO hay un @ExceptionHandler(Exception.class) catch-all.
 * Las excepciones de dominio se lanzan como ResponseStatusException, que Spring
 * resuelve nativamente conservando su status; un catch-all las pisaria.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, String>> handleAuth(Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Credenciales incorrectas"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El recurso ya existe"));
    }

    @ExceptionHandler({UsernameNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(Exception e) {
        String msg = e.getMessage() == null ? "Recurso no encontrado" : e.getMessage();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Datos invalidos");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
