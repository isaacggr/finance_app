package com.isaacggr.finance_app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limitador de taxa em memória baseado em janela deslizante exata (Sliding Window Log).
 * Mantém o uso de memória limitado por {@code maxTrackedKeys} e limpeza periódica de janelas expiradas.
 */
@Component
public class SlidingWindowRateLimiter {

    public record RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds
    ) {}

    private static final class WindowBucket {
        private final ArrayDeque<Long> timestampsMillis = new ArrayDeque<>();
        private long lastAccessMillis;
        private long expiresAtMillis;
    }

    private final Clock clock;
    private final int maxTrackedKeys;
    private final ConcurrentHashMap<String, WindowBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong operationCounter = new AtomicLong(0);

    @Autowired
    public SlidingWindowRateLimiter(RateLimitProperties properties) {
        this(Clock.systemUTC(), properties.getMaxTrackedKeys());
    }

    public SlidingWindowRateLimiter(Clock clock, int maxTrackedKeys) {
        this.clock = clock;
        this.maxTrackedKeys = Math.max(1, maxTrackedKeys);
    }

    public RateLimitDecision tryConsume(String key, int maxRequests, Duration window) {
        int safeLimit = Math.max(1, maxRequests);
        long windowMillis = Math.max(1L, window.toMillis());
        long nowMillis = clock.millis();

        cleanupIfNeeded(key, nowMillis);

        final RateLimitDecision[] decisionHolder = new RateLimitDecision[1];
        buckets.compute(key, (ignored, existing) -> {
            WindowBucket bucket = existing != null ? existing : new WindowBucket();
            evictExpiredTimestamps(bucket, nowMillis - windowMillis);
            bucket.lastAccessMillis = nowMillis;
            bucket.expiresAtMillis = nowMillis + windowMillis;

            if (bucket.timestampsMillis.size() >= safeLimit) {
                long oldestMillis = bucket.timestampsMillis.peekFirst();
                long waitMillis = Math.max(1L, (oldestMillis + windowMillis) - nowMillis);
                long retryAfterSeconds = Math.max(1L, (waitMillis + 999L) / 1000L);
                decisionHolder[0] = new RateLimitDecision(false, safeLimit, 0, retryAfterSeconds);
                return bucket;
            }

            bucket.timestampsMillis.addLast(nowMillis);
            int remaining = Math.max(0, safeLimit - bucket.timestampsMillis.size());
            decisionHolder[0] = new RateLimitDecision(true, safeLimit, remaining, 0L);
            return bucket;
        });

        return decisionHolder[0];
    }

    public int trackedKeysCount() {
        return buckets.size();
    }

    private void evictExpiredTimestamps(WindowBucket bucket, long cutoffMillis) {
        while (!bucket.timestampsMillis.isEmpty() && bucket.timestampsMillis.peekFirst() <= cutoffMillis) {
            bucket.timestampsMillis.removeFirst();
        }
    }

    private void cleanupIfNeeded(String incomingKey, long nowMillis) {
        boolean periodicSweep = (operationCounter.incrementAndGet() & 0xFF) == 0;
        boolean capacityReached = buckets.size() >= maxTrackedKeys && !buckets.containsKey(incomingKey);

        if (!periodicSweep && !capacityReached) {
            return;
        }

        for (Map.Entry<String, WindowBucket> entry : buckets.entrySet()) {
            if (entry.getValue().expiresAtMillis <= nowMillis) {
                buckets.computeIfPresent(entry.getKey(), (k, bucket) ->
                        bucket.expiresAtMillis <= nowMillis ? null : bucket
                );
            }
        }

        if (buckets.size() >= maxTrackedKeys && !buckets.containsKey(incomingKey)) {
            int toRemove = (buckets.size() - maxTrackedKeys) + 1;
            buckets.entrySet().stream()
                    .sorted(Comparator.comparingLong(e -> e.getValue().lastAccessMillis))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .forEach(oldestKey -> buckets.computeIfPresent(oldestKey, (k, b) -> null));
        }
    }
}
