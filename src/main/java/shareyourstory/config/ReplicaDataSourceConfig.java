package shareyourstory.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuración de DOS orígenes de datos para el escenario de replicación
 * (Paso 7, ver db/07-replicacion):
 *
 *  - PRIMARIO  (app_rw -> mysql-primary): lecturas y ESCRITURAS de la aplicación.
 *  - RÉPLICA   (app_ro -> mysql-replica): SOLO LECTURA (informes/estadísticas).
 *
 * Toda esta configuración está condicionada a 'app.replica.enabled=true'. Si está
 * desactivada (valor por defecto), no se declara ningún DataSource manual y Spring
 * Boot autoconfigura el primario como siempre: el arranque normal NO se ve afectado.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.replica", name = "enabled", havingValue = "true")
public class ReplicaDataSourceConfig {

    // ---- PRIMARIO (escrituras) ----------------------------------------------

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(
            @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    // ---- RÉPLICA (solo lectura) ---------------------------------------------

    @Bean
    @ConfigurationProperties("app.replica.datasource")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("app.replica.datasource.hikari")
    public DataSource replicaDataSource(
            @Qualifier("replicaDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcTemplate replicaJdbcTemplate(
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        return new JdbcTemplate(replicaDataSource);
    }
}
