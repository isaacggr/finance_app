package com.isaacggr.finance_app.config;

import com.isaacggr.finance_app.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

/**
 * Filtro HTTP que aplica Rate Limiting por IP e política de endpoint nas rotas públicas
 * de autenticação ({@code /auth/**}) e cadastro ({@code POST /users}).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record RateLimitPolicy(String scope, int maxRequests) {}

    private final SlidingWindowRateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            SlidingWindowRateLimiter rateLimiter,
            RateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = extractNormalizedPath(request);
        RateLimitPolicy policy = resolvePolicy(request.getMethod(), path);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = policy.scope() + ":" + clientIp;
        Duration window = Duration.ofSeconds(properties.getWindowSeconds());

        SlidingWindowRateLimiter.RateLimitDecision decision =
                rateLimiter.tryConsume(key, policy.maxRequests(), window);

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            long retryAfter = decision.retryAfterSeconds();
            String unit = retryAfter == 1 ? "segundo" : "segundos";
            String message = "Muitas tentativas em sequência. Aguarde " + retryAfter + " " + unit + " antes de tentar novamente.";

            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorResponse body = new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), message);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(String method, String path) {
        if ("/users".equals(path) && HttpMethod.POST.matches(method)) {
            return new RateLimitPolicy("USER_REGISTER", properties.getEmailDispatchMaxRequests());
        }

        if (!"/auth".equals(path) && !path.startsWith("/auth/")) {
            return null;
        }

        if ("/auth/login".equals(path) && HttpMethod.POST.matches(method)) {
            return new RateLimitPolicy("AUTH_LOGIN", properties.getLoginMaxRequests());
        }
        if (("/auth/verify-email".equals(path) || "/auth/reset-password".equals(path))
                && HttpMethod.POST.matches(method)) {
            return new RateLimitPolicy("AUTH_VERIFICATION", properties.getVerificationMaxRequests());
        }
        if (("/auth/resend-code".equals(path) || "/auth/forgot-password".equals(path))
                && HttpMethod.POST.matches(method)) {
            return new RateLimitPolicy("AUTH_EMAIL_DISPATCH", properties.getEmailDispatchMaxRequests());
        }

        return new RateLimitPolicy("AUTH_GENERAL", properties.getLoginMaxRequests());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstIp = forwardedFor.split(",")[0].trim();
                if (!firstIp.isEmpty()) {
                    return firstIp;
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr.trim();
    }

    private String extractNormalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        // Remove matrix/path parameters (ex: /auth/login;jsessionid=...) para impedir bypass de rota
        int semicolonIdx = uri.indexOf(';');
        if (semicolonIdx >= 0) {
            uri = uri.substring(0, semicolonIdx);
        }
        uri = uri.replaceAll("/{2,}", "/");
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri.isEmpty() ? "/" : uri;
    }
}
