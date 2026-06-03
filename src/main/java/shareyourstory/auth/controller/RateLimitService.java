package shareyourstory.auth.controller;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Rate limiting en memoria con bucket4j. Mantiene un bucket por clave
 * (tipicamente "regla:IP" o "regla:token"); cada bucket recarga su capacidad de
 * forma continua en la ventana indicada.
 *
 * En memoria: se pierde al reiniciar y no se comparte entre instancias. Suficiente
 * para el alcance; en produccion iria a Redis (bucket4j lo soporta).
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Intenta consumir 1 token del bucket identificado por {@code key}. */
    public boolean tryConsume(String key, int capacity, Duration period) {
        String bucketKey = key + "|" + capacity + "|" + period.toMillis();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(capacity, period));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period));
        return Bucket.builder().addLimit(limit).build();
    }
}
