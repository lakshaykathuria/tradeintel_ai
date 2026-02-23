package com.tradeintel.ai.broker.upstox;

import com.tradeintel.ai.config.UpstoxConfig;
import com.upstox.ApiClient;
import com.upstox.ApiException;
import io.swagger.client.api.LoginApi;
import io.swagger.client.api.UserApi;
import com.upstox.api.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpstoxAuthService {

    private final UpstoxConfig upstoxConfig;

    private String currentAccessToken;
    private LocalDateTime tokenExpiryTime;

    // File to persist the token across restarts
    private static final String TOKEN_FILE = System.getProperty("user.home") + "/.upstox_token";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Load persisted token on startup
     */
    @PostConstruct
    public void loadPersistedToken() {
        try {
            Path tokenPath = Paths.get(TOKEN_FILE);
            if (Files.exists(tokenPath)) {
                String[] lines = Files.readString(tokenPath).split("\n");
                if (lines.length >= 2) {
                    String token = lines[0].trim();
                    LocalDateTime expiry = LocalDateTime.parse(lines[1].trim(), FORMATTER);
                    if (LocalDateTime.now().isBefore(expiry)) {
                        currentAccessToken = token;
                        tokenExpiryTime = expiry;
                        log.info("Loaded persisted Upstox token (expires: {})", expiry);
                    } else {
                        log.info("Persisted Upstox token has expired, will need to re-authenticate");
                        Files.deleteIfExists(tokenPath);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load persisted token: {}", e.getMessage());
        }
    }

    /**
     * Persist token to file so it survives restarts
     */
    private void persistToken(String token, LocalDateTime expiry) {
        try {
            String content = token + "\n" + expiry.format(FORMATTER);
            Files.writeString(Paths.get(TOKEN_FILE), content);
            log.info("Token persisted to {}", TOKEN_FILE);
        } catch (Exception e) {
            log.warn("Could not persist token to file: {}", e.getMessage());
        }
    }

    /**
     * Generate authorization URL for user login
     */
    public String generateAuthUrl() {
        return String.format(
                "https://api.upstox.com/v2/login/authorization/dialog?client_id=%s&redirect_uri=%s&response_type=code",
                upstoxConfig.getApiKey(),
                upstoxConfig.getRedirectUrl());
    }

    /**
     * Generate access token from authorization code
     */
    public TokenResponse generateAccessToken(String authCode) throws ApiException {
        log.info("Generating access token from auth code");

        ApiClient apiClient = new ApiClient();
        LoginApi loginApi = new LoginApi(apiClient);

        TokenResponse tokenResponse = loginApi.token(
                "2.0",
                authCode,
                upstoxConfig.getApiKey(),
                upstoxConfig.getApiSecret(),
                upstoxConfig.getRedirectUrl(),
                "authorization_code");

        if (tokenResponse.getAccessToken() != null) {
            currentAccessToken = tokenResponse.getAccessToken();
            // Upstox tokens are valid until 3:30 AM next day
            tokenExpiryTime = LocalDateTime.now().withHour(3).withMinute(30).withSecond(0).plusDays(1);
            persistToken(currentAccessToken, tokenExpiryTime);
            log.info("Access token generated and persisted successfully");
        }

        return tokenResponse;
    }

    /**
     * Check if current access token is locally valid (non-null and not past local
     * expiry).
     * This is a fast check and does NOT call the Upstox API.
     */
    public boolean isTokenValid() {
        return currentAccessToken != null &&
                tokenExpiryTime != null &&
                LocalDateTime.now().isBefore(tokenExpiryTime);
    }

    /**
     * Verify access token is truly valid by making a live call to the Upstox user
     * profile API.
     * Returns true only if Upstox accepts the token.
     */
    public boolean verifyTokenWithApi() {
        if (!isTokenValid()) {
            return false;
        }
        try {
            ApiClient apiClient = new ApiClient();
            apiClient.setAccessToken(currentAccessToken);
            UserApi userApi = new UserApi(apiClient);
            userApi.getProfile("2.0");
            return true;
        } catch (Exception e) {
            log.warn("Live token verification failed — token is invalid or expired: {}", e.getMessage());
            // Invalidate the stale token so subsequent checks fail fast
            currentAccessToken = null;
            tokenExpiryTime = null;
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(TOKEN_FILE));
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    /**
     * Get current access token
     */
    public String getAccessToken() {
        if (!isTokenValid()) {
            log.warn("Access token is expired or not available");
            return null;
        }
        return currentAccessToken;
    }

    /**
     * Set access token manually (for testing or manual configuration)
     */
    public void setAccessToken(String token) {
        this.currentAccessToken = token;
        this.tokenExpiryTime = LocalDateTime.now().withHour(3).withMinute(30).withSecond(0).plusDays(1);
        persistToken(currentAccessToken, tokenExpiryTime);
        log.info("Access token set manually and persisted");
    }

    /**
     * Get configured ApiClient with access token
     */
    public ApiClient getAuthenticatedClient() {
        if (!isTokenValid()) {
            throw new IllegalStateException("Not authenticated. Please login first.");
        }

        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken(currentAccessToken);
        return apiClient;
    }

    /**
     * Logout and invalidate token
     */
    public void logout() {
        currentAccessToken = null;
        tokenExpiryTime = null;
        try {
            Files.deleteIfExists(Paths.get(TOKEN_FILE));
        } catch (Exception e) {
            log.warn("Could not delete token file: {}", e.getMessage());
        }
        log.info("User logged out, token invalidated");
    }
}
