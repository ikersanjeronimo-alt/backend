package shareyourstory.auth.controller;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

// Rate limiting en memoria; se pierde al reiniciar (aceptable para PBL).
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacity, Duration period) {
        String bucketKey = key + "|" + capacity + "|" + period.toMillis();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(capacity, period));
        return bucket.tryConsume(1);
    }

    private Bucket newBucket(int capacity, Duration period) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
