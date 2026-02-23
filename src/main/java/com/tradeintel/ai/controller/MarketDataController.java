package com.tradeintel.ai.controller;

import com.tradeintel.ai.broker.upstox.UpstoxApiClient;
import com.tradeintel.ai.service.HistoricalDataService;
import com.tradeintel.ai.service.InstrumentService;
import com.upstox.api.GetFullMarketQuoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/market-data")
@Slf4j
public class MarketDataController {

    private final UpstoxApiClient upstoxApiClient;
    private final InstrumentService instrumentService;
    private final HistoricalDataService historicalDataService;

    @Autowired(required = false)
    public MarketDataController(UpstoxApiClient upstoxApiClient,
            InstrumentService instrumentService,
            HistoricalDataService historicalDataService) {
        this.upstoxApiClient = upstoxApiClient;
        this.instrumentService = instrumentService;
        this.historicalDataService = historicalDataService;
    }

    /**
     * Search for instruments by symbol or name
     *
     * @param query Search query (e.g., "TATAPOWER", "SBI", "Reliance")
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchInstruments(@RequestParam String query) {
        try {
            List<InstrumentService.InstrumentInfo> results = instrumentService.searchInstruments(query);

            List<Map<String, String>> response = results.stream()
                    .map(info -> {
                        Map<String, String> item = new HashMap<>();
                        item.put("instrumentKey", info.getInstrumentKey());
                        item.put("symbol", info.getSymbol());
                        item.put("name", info.getName());
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching instruments for query: {}", query, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get quote for a symbol
     *
     * @param symbol Instrument key or trading symbol (e.g., "NSE_EQ|INE062A01020"
     *               or "TATAPOWER")
     */
    @GetMapping("/quote")
    public ResponseEntity<?> getQuote(@RequestParam String symbol) {
        if (upstoxApiClient == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Upstox API not configured");
            error.put("message", "Please configure UPSTOX_API_KEY and UPSTOX_API_SECRET in environment variables");
            return ResponseEntity.status(503).body(error);
        }

        try {
            String instrumentKey = instrumentService.resolveToInstrumentKey(symbol);
            log.info("Resolved '{}' to instrument key: {}", symbol, instrumentKey);

            GetFullMarketQuoteResponse quote = upstoxApiClient.getQuote(instrumentKey);
            return ResponseEntity.ok(quote.getData());
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid symbol");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            error.put("message", e.getMessage());
            return ResponseEntity.status(401).body(error);
        } catch (Exception e) {
            log.error("Error fetching quote for {}", symbol, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch quote");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Fetch historical OHLCV data from Upstox and save to database.
     * Call this before running strategies so they have enough historical data.
     *
     * @param symbol   Trading symbol (e.g. "TATAPOWER")
     * @param fromDate Start date YYYY-MM-DD (optional, defaults to 1 year ago)
     * @param toDate   End date YYYY-MM-DD (optional, defaults to today)
     * @param interval Candle interval: day, week, month, 1minute, 30minute
     *                 (default: day)
     */
    @PostMapping("/fetch-historical")
    public ResponseEntity<?> fetchHistoricalData(
            @RequestParam String symbol,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false, defaultValue = "day") String interval) {

        try {
            int saved = historicalDataService.fetchAndSaveHistoricalData(symbol, fromDate, toDate, interval);
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("interval", interval);
            response.put("candlesSaved", saved);
            response.put("message", saved > 0
                    ? "Successfully fetched " + saved + " candles for " + symbol
                    : "No new candles to save (data may already be up to date)");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Not authenticated");
            error.put("message", e.getMessage());
            return ResponseEntity.status(401).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid symbol");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error fetching historical data for {}", symbol, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch historical data");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Market Data Service");
        response.put("provider", "Upstox API");
        response.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
