package com.isaacggr.finance_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades configuráveis de Rate Limiting para rotas públicas sensíveis
 * ({@code /auth/**} e {@code POST /users}).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean trustForwardedHeaders = false;
    private int maxTrackedKeys = 10_000;
    private long windowSeconds = 60;
    private int loginMaxRequests = 10;
    private int verificationMaxRequests = 10;
    private int emailDispatchMaxRequests = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public int getMaxTrackedKeys() {
        return maxTrackedKeys;
    }

    public void setMaxTrackedKeys(int maxTrackedKeys) {
        this.maxTrackedKeys = Math.max(100, maxTrackedKeys);
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = Math.max(1L, windowSeconds);
    }

    public int getLoginMaxRequests() {
        return loginMaxRequests;
    }

    public void setLoginMaxRequests(int loginMaxRequests) {
        this.loginMaxRequests = Math.max(1, loginMaxRequests);
    }

    public int getVerificationMaxRequests() {
        return verificationMaxRequests;
    }

    public void setVerificationMaxRequests(int verificationMaxRequests) {
        this.verificationMaxRequests = Math.max(1, verificationMaxRequests);
    }

    public int getEmailDispatchMaxRequests() {
        return emailDispatchMaxRequests;
    }

    public void setEmailDispatchMaxRequests(int emailDispatchMaxRequests) {
        this.emailDispatchMaxRequests = Math.max(1, emailDispatchMaxRequests);
    }
}
