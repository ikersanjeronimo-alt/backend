package shareyourstory.domain.moderation.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estadísticas de moderación servidas desde la RÉPLICA de solo lectura (app_ro),
 * mientras las escrituras siguen yendo al primario. Demuestra el reparto
 * lectura/escritura del requisito "gestiona el acceso teniendo en cuenta la
 * replicación" (Nivel 3).
 *
 * El JdbcTemplate de la réplica solo existe si 'app.replica.enabled=true'
 * (ver ReplicaDataSourceConfig); por eso se inyecta como opcional.
 */
@RestController
@RequestMapping("/api/moderation")
public class StatsController {

    @Autowired(required = false)
    @Qualifier("replicaJdbcTemplate")
    private JdbcTemplate replicaJdbcTemplate;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        if (replicaJdbcTemplate == null) {
            return ResponseEntity.ok(Map.of(
                    "source", "primary (replica deshabilitada)",
                    "hint", "Definir app.replica.enabled=true para leer de la replica"));
        }
        Long total = replicaJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reports", Long.class);
        Long pending = replicaJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE status = 'PENDING'", Long.class);
        return ResponseEntity.ok(Map.of(
                "source", "replica (solo lectura, usuario app_ro)",
                "totalReports", total == null ? 0L : total,
                "pendingReports", pending == null ? 0L : pending));
    }
}
