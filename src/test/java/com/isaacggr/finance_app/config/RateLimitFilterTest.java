package com.isaacggr.finance_app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitProperties properties;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setWindowSeconds(60);
        properties.setLoginMaxRequests(2);
        properties.setVerificationMaxRequests(2);
        properties.setEmailDispatchMaxRequests(2);

        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(
                Clock.fixed(Instant.parse("2026-11-12T03:00:00Z"), ZoneOffset.UTC),
                100
        );
        filter = new RateLimitFilter(limiter, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("Deve ignorar X-Forwarded-For falso por padrão (trustForwardedHeaders = false) evitando spoofing")
    void shouldIgnoreSpoofedForwardedHeadersByDefault() throws Exception {
        for (int i = 1; i <= 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("198.51.100.50");
            req.addHeader("X-Forwarded-For", "203.0.113." + i);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/auth/login");
        blockedReq.setRemoteAddr("198.51.100.50");
        blockedReq.addHeader("X-Forwarded-For", "203.0.113.99");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        filter.doFilter(blockedReq, blockedRes, new MockFilterChain());

        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader(HttpHeaders.RETRY_AFTER));
        assertEquals("2", blockedRes.getHeader("X-RateLimit-Limit"));
        assertEquals("0", blockedRes.getHeader("X-RateLimit-Remaining"));
        assertTrue(blockedRes.getContentAsString().contains("\"status\":429")
                || blockedRes.getContentAsString().contains("\"status\": 429"));
    }

    @Test
    @DisplayName("Deve usar primeiro IP de X-Forwarded-For quando trustForwardedHeaders = true")
    void shouldUseForwardedForWhenProxyTrusted() throws Exception {
        properties.setTrustForwardedHeaders(true);

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr("127.0.0.1");
            req.addHeader("X-Forwarded-For", "198.51.100.80, 10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }

        // Outro cliente real por trás do mesmo proxy não deve ser bloqueado
        MockHttpServletRequest anotherUserReq = new MockHttpServletRequest("POST", "/auth/login");
        anotherUserReq.setRemoteAddr("127.0.0.1");
        anotherUserReq.addHeader("X-Forwarded-For", "198.51.100.81, 10.0.0.1");
        MockHttpServletResponse anotherUserRes = new MockHttpServletResponse();
        filter.doFilter(anotherUserReq, anotherUserRes, new MockFilterChain());

        assertEquals(200, anotherUserRes.getStatus());
    }

    @Test
    @DisplayName("Deve normalizar barra final e matrix params para impedir bypass de rota em /auth/**")
    void shouldNormalizePathBypassAttempts() throws Exception {
        MockHttpServletRequest r1 = new MockHttpServletRequest("POST", "/auth/login");
        r1.setRemoteAddr("198.51.100.60");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(r1, res1, new MockFilterChain());
        assertEquals(200, res1.getStatus());

        MockHttpServletRequest r2 = new MockHttpServletRequest("POST", "/auth/login/;jsessionid=xyz");
        r2.setRemoteAddr("198.51.100.60");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(r2, res2, new MockFilterChain());
        assertEquals(200, res2.getStatus());

        MockHttpServletRequest r3 = new MockHttpServletRequest("POST", "//auth/login/");
        r3.setRemoteAddr("198.51.100.60");
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        filter.doFilter(r3, res3, new MockFilterChain());
        assertEquals(429, res3.getStatus());
    }

    @Test
    @DisplayName("Não deve aplicar rate limit estrito em OPTIONS preflight nem em rotas privadas autenticadas")
    void shouldBypassOptionsAndNonAuthRoutes() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest optionsReq = new MockHttpServletRequest("OPTIONS", "/auth/login");
            optionsReq.setRemoteAddr("198.51.100.70");
            MockHttpServletResponse optionsRes = new MockHttpServletResponse();
            filter.doFilter(optionsReq, optionsRes, new MockFilterChain());
            assertNull(optionsRes.getHeader("X-RateLimit-Limit"));

            MockHttpServletRequest privateReq = new MockHttpServletRequest("GET", "/transactions");
            privateReq.setRemoteAddr("198.51.100.70");
            MockHttpServletResponse privateRes = new MockHttpServletResponse();
            filter.doFilter(privateReq, privateRes, new MockFilterChain());
            assertNull(privateRes.getHeader("X-RateLimit-Limit"));
        }
    }
}
