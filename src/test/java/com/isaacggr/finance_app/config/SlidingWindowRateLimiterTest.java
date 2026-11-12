package com.isaacggr.finance_app.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowRateLimiterTest {

    private static final class MutableClock extends Clock {
        private final AtomicLong millis;

        private MutableClock(long initialMillis) {
            this.millis = new AtomicLong(initialMillis);
        }

        void advanceSeconds(long seconds) {
            millis.addAndGet(seconds * 1000L);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public long millis() {
            return millis.get();
        }
    }

    @Test
    @DisplayName("Deve permitir até N requisições na janela e bloquear a seguinte com Retry-After calculado")
    void shouldAllowUpToLimitAndBlockOverflow() {
        MutableClock clock = new MutableClock(1_000_000L);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock, 100);
        Duration window = Duration.ofSeconds(60);

        for (int i = 1; i <= 5; i++) {
            var decision = limiter.tryConsume("AUTH_LOGIN:198.51.100.1", 5, window);
            assertTrue(decision.allowed());
            assertEquals(5, decision.limit());
            assertEquals(5 - i, decision.remaining());
            assertEquals(0L, decision.retryAfterSeconds());
        }

        var blocked = limiter.tryConsume("AUTH_LOGIN:198.51.100.1", 5, window);
        assertFalse(blocked.allowed());
        assertEquals(5, blocked.limit());
        assertEquals(0, blocked.remaining());
        assertEquals(60L, blocked.retryAfterSeconds());

        // Avança 25 segundos: continua bloqueado e Retry-After diminui para 35s
        clock.advanceSeconds(25);
        var stillBlocked = limiter.tryConsume("AUTH_LOGIN:198.51.100.1", 5, window);
        assertFalse(stillBlocked.allowed());
        assertEquals(35L, stillBlocked.retryAfterSeconds());

        // Avança mais 35 segundos (total 60s desde o primeiro request): libera exatamente 1 vaga
        clock.advanceSeconds(35);
        var allowedAgain = limiter.tryConsume("AUTH_LOGIN:198.51.100.1", 5, window);
        assertTrue(allowedAgain.allowed());
    }

    @Test
    @DisplayName("Deve aplicar janela deslizante real mantendo requisições recentes após expirar apenas as antigas")
    void shouldSlideWindowAccurately() {
        MutableClock clock = new MutableClock(1_000_000L);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock, 100);
        Duration window = Duration.ofSeconds(60);

        // 2 requisições em t=0s
        assertTrue(limiter.tryConsume("key", 3, window).allowed());
        assertTrue(limiter.tryConsume("key", 3, window).allowed());

        // 1 requisição em t=30s (atingiu 3/3)
        clock.advanceSeconds(30);
        assertTrue(limiter.tryConsume("key", 3, window).allowed());
        assertFalse(limiter.tryConsume("key", 3, window).allowed());

        // Em t=60s expiram apenas as 2 de t=0s, sobrando a de t=30s -> 2 novas vagas disponíveis
        clock.advanceSeconds(30);
        var d1 = limiter.tryConsume("key", 3, window);
        var d2 = limiter.tryConsume("key", 3, window);
        var d3 = limiter.tryConsume("key", 3, window);

        assertTrue(d1.allowed());
        assertEquals(1, d1.remaining());
        assertTrue(d2.allowed());
        assertEquals(0, d2.remaining());
        assertFalse(d3.allowed());
        assertEquals(30L, d3.retryAfterSeconds());
    }

    @Test
    @DisplayName("Deve manter contadores isolados por chave e respeitar o limite máximo de chaves em memória")
    void shouldIsolateKeysAndCapTrackedKeys() {
        MutableClock clock = new MutableClock(1_000_000L);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock, 3);
        Duration window = Duration.ofSeconds(60);

        assertTrue(limiter.tryConsume("AUTH:1.1.1.1", 1, window).allowed());
        assertFalse(limiter.tryConsume("AUTH:1.1.1.1", 1, window).allowed());
        assertTrue(limiter.tryConsume("AUTH:2.2.2.2", 1, window).allowed());

        clock.advanceSeconds(1);
        limiter.tryConsume("AUTH:3.3.3.3", 1, window);
        clock.advanceSeconds(1);
        limiter.tryConsume("AUTH:4.4.4.4", 1, window);

        assertTrue(limiter.trackedKeysCount() <= 3);
    }
}
