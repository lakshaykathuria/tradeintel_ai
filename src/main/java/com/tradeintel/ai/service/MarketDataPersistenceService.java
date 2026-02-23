package com.tradeintel.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service to consume market data from Kafka and persist to database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPersistenceService {

    private final MarketDataRepository marketDataRepository;
    private final StockRepository stockRepository;
    private final InstrumentService instrumentService;

    /**
     * Listen to market data from Kafka and save to database
     */
    @KafkaListener(topics = "market-data-topic", groupId = "market-data-persistence-group")
    public void consumeMarketData(JsonNode marketUpdate) {
        try {
            // Extract feeds data
            JsonNode feeds = marketUpdate.get("feeds");
            if (feeds == null || !feeds.isObject()) {
                log.warn("No feeds data in market update");
                return;
            }

            // Process each instrument in the feed
            feeds.fields().forEachRemaining(entry -> {
                String instrumentToken = entry.getKey();
                JsonNode data = entry.getValue();

                try {
                    saveMarketData(instrumentToken, data);
                } catch (Exception e) {
                    log.error("Error saving market data for {}: {}", instrumentToken, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error processing market update", e);
        }
    }

    /**
     * Save market data for a specific instrument
     */
    private void saveMarketData(String instrumentToken, JsonNode data) {
        // Extract LTPC (Last Traded Price) data
        JsonNode ltpc = data.get("ltpc");
        if (ltpc == null) {
            log.debug("No LTPC data for {}", instrumentToken);
            return;
        }

        // Get last price
        Double lastPrice = ltpc.has("ltp") ? ltpc.get("ltp").asDouble() : null;
        if (lastPrice == null || lastPrice == 0) {
            log.debug("Invalid last price for {}", instrumentToken);
            return;
        }

        // Extract symbol from instrument token (e.g., "NSE_EQ|INE062A01020" -> symbol)
        // For now, we'll try to find stock by instrument token or create a mapping
        String symbol = extractSymbolFromToken(instrumentToken);

        // Find or create stock
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setSymbol(symbol);
                    newStock.setName(symbol);
                    newStock.setExchange("NSE"); // Default to NSE
                    newStock.setIsActive(true);
                    Stock saved = stockRepository.save(newStock);
                    log.info("Created new stock: {}", symbol);
                    return saved;
                });

        // Create market data entry
        MarketData marketData = new MarketData();
        marketData.setStock(stock);
        marketData.setTimestamp(LocalDateTime.now());

        // Set OHLC data (if available in full mode)
        JsonNode ohlc = data.get("ohlc");
        if (ohlc != null) {
            marketData.setOpenPrice(getBigDecimal(ohlc, "open"));
            marketData.setHighPrice(getBigDecimal(ohlc, "high"));
            marketData.setLowPrice(getBigDecimal(ohlc, "low"));
            marketData.setClosePrice(getBigDecimal(ohlc, "close"));
        } else {
            // If OHLC not available, use last price for all
            BigDecimal price = BigDecimal.valueOf(lastPrice);
            marketData.setOpenPrice(price);
            marketData.setHighPrice(price);
            marketData.setLowPrice(price);
            marketData.setClosePrice(price);
        }

        // Set volume
        Long volume = data.has("volume") ? data.get("volume").asLong() : 0L;
        marketData.setVolume(volume);

        // Save to database
        marketDataRepository.save(marketData);
        log.info("Saved market data for {} - Price: ₹{}, Volume: {}", symbol, lastPrice, volume);
    }

    /**
     * Extract symbol from instrument token using InstrumentService
     */
    private String extractSymbolFromToken(String instrumentToken) {
        try {
            // Search for the instrument in the cache
            var results = instrumentService.searchInstruments(instrumentToken);
            if (!results.isEmpty()) {
                return results.get(0).getSymbol();
            }
        } catch (Exception e) {
            log.debug("Could not find symbol for instrument key: {}", instrumentToken);
        }
        // Fallback: return the token as-is if not found
        return instrumentToken;
    }

    /**
     * Safely extract BigDecimal from JsonNode
     */
    private BigDecimal getBigDecimal(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return BigDecimal.valueOf(node.get(fieldName).asDouble());
        }
        return BigDecimal.ZERO;
    }
}
