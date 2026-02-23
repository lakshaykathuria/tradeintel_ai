package com.tradeintel.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "upstox.api")
@Data
public class UpstoxConfig {

    private String apiKey;
    private String apiSecret;
    private String redirectUrl;
    private String baseUrl;
}
