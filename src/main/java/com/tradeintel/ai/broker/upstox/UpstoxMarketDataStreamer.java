package com.tradeintel.ai.broker.upstox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upstox.ApiClient;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.constants.Mode;
import com.tradeintel.ai.service.MarketDataPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpstoxMarketDataStreamer {

    private final UpstoxAuthService authService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final MarketDataPersistenceService marketDataPersistenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MarketDataStreamerV3 streamer;
    private final Set<String> subscribedInstruments = new HashSet<>();

    /**
     * Connect to Upstox WebSocket
     */
    public void connect() {
        try {
            ApiClient apiClient = authService.getAuthenticatedClient();

            // Create streamer instance with auto-reconnect enabled
            streamer = new MarketDataStreamerV3(apiClient, subscribedInstruments, Mode.FULL);

            streamer.setOnMarketUpdateListener(marketUpdate -> {
                try {
                    // Convert MarketUpdateV3 to JSON for Kafka/Processing
                    String jsonUpdate = objectMapper.writeValueAsString(marketUpdate);
                    JsonNode node = objectMapper.readTree(jsonUpdate);

                    log.debug("Received market update: {}", node);

                    // Save directly to database (bypasses Kafka dependency)
                    marketDataPersistenceService.consumeMarketData(node);

                    // Also send to Kafka and WebSocket for real-time updates
                    kafkaTemplate.send("market-data-topic", node);
                    messagingTemplate.convertAndSend("/topic/market-data", node);

                } catch (Exception e) {
                    log.error("Error processing market update", e);
                }
            });

            log.info("Connecting to Upstox WebSocket...");
            streamer.connect();

        } catch (Exception e) {
            log.error("Failed to connect to Upstox WebSocket", e);
            throw new RuntimeException("WebSocket connection failed", e);
        }
    }

    /**
     * Subscribe to instruments
     * 
     * @param instrumentKeys Set of instrument keys (e.g. "NSE_EQ|INE062A01020")
     * @param mode           Data mode (LTPC or FULL)
     */
    public void subscribe(Set<String> instrumentKeys, Mode mode) {
        if (streamer == null) {
            connect();
        }

        try {
            subscribedInstruments.addAll(instrumentKeys);
            streamer.subscribe(instrumentKeys, mode);
            log.info("Subscribed to {} instruments", instrumentKeys.size());
        } catch (Exception e) {
            log.error("Failed to subscribe", e);
        }
    }

    /**
     * Unsubscribe from instruments
     */
    public void unsubscribe(Set<String> instrumentKeys) {
        if (streamer != null) {
            try {
                streamer.unsubscribe(instrumentKeys);
                subscribedInstruments.removeAll(instrumentKeys);
                log.info("Unsubscribed from {} instruments", instrumentKeys.size());
            } catch (Exception e) {
                log.error("Failed to unsubscribe", e);
            }
        }
    }

    /**
     * Disconnect WebSocket
     */
    public void disconnect() {
        if (streamer != null) {
            try {
                streamer.disconnect();
                log.info("Disconnected from WebSocket");
            } catch (Exception e) {
                log.error("Error disconnecting", e);
            }
        }
    }
}
