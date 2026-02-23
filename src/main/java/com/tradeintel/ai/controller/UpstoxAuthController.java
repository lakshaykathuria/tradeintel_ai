package com.tradeintel.ai.controller;

import com.tradeintel.ai.broker.upstox.UpstoxAuthService;
import com.upstox.api.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upstox")
@RequiredArgsConstructor
@Slf4j
public class UpstoxAuthController {

    private final UpstoxAuthService authService;

    /**
     * Get authorization URL for Upstox login
     */
    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String authUrl = authService.generateAuthUrl();
        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        response.put("message", "Please visit this URL to authorize the application");
        return ResponseEntity.ok(response);
    }

    /**
     * Callback endpoint for Upstox OAuth
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        log.info("Received auth callback with code: {}", code);

        try {
            TokenResponse tokenResponse = authService.generateAccessToken(code);

            Map<String, String> response = new HashMap<>();
            if (tokenResponse.getAccessToken() != null) {
                response.put("status", "success");
                response.put("message", "Authentication successful");
                response.put("userName", tokenResponse.getUserName());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to get access token");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (com.upstox.ApiException e) {
            log.error("Upstox API Error: Code={}, Body={}", e.getCode(), e.getResponseBody(), e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Upstox API Error: " + e.getMessage());
            response.put("details", e.getResponseBody());
            return ResponseEntity.status(e.getCode()).body(response);
        } catch (Exception e) {
            log.error("Error during authentication", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Internal Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Check authentication status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        Map<String, Object> response = new HashMap<>();
        // hasToken: fast local check — token exists and hasn't passed local expiry
        boolean hasToken = authService.isTokenValid();
        // authenticated: live API call to confirm Upstox still accepts the token
        boolean authenticated = hasToken && authService.verifyTokenWithApi();
        response.put("authenticated", authenticated);
        response.put("hasToken", hasToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Manual token setup (for testing)
     */
    @PostMapping("/set-token")
    public ResponseEntity<Map<String, String>> setToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        authService.setAccessToken(token);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Token set successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        authService.logout();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }
}
