package com.tradeintel.ai.controller;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import com.tradeintel.ai.service.AIStrategyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI-powered trading insights
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AIInsightsController {

    private final AIStrategyService aiStrategyService;
    private final StockRepository stockRepository;
    private final MarketDataRepository marketDataRepository;

    /**
     * Get comprehensive AI analysis for a stock
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeStock(@RequestBody AIAnalysisRequest request) {
        try {
            request.setSymbol(request.getSymbol().toUpperCase());
            log.info("Running AI analysis for {} with type {}", request.getSymbol(), request.getAnalysisType());

            // Get or create stock
            Stock stock = stockRepository.findBySymbol(request.getSymbol())
                    .orElseGet(() -> {
                        Stock newStock = new Stock();
                        newStock.setSymbol(request.getSymbol());
                        newStock.setName(request.getSymbol());
                        newStock.setExchange("NSE"); // Default to NSE
                        newStock.setIsActive(true);
                        return stockRepository.save(newStock);
                    });

            // Get market data — DESC from repo; reverse to ASC (oldest→newest) so AI sees
            // recent data last
            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData);

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available for " + request.getSymbol()));
            }

            // Determine analysis type
            String analysisType = request.getAnalysisType() != null
                    ? request.getAnalysisType()
                    : "comprehensive";

            // Run AI analysis
            TradeSignal signal = aiStrategyService.analyzeWithAI(stock, marketData, analysisType);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", request.getSymbol());
            response.put("analysisType", analysisType);
            response.put("signal", signal.getSignalType().toString());
            response.put("confidence", signal.getConfidenceScore());
            response.put("reasoning", signal.getReasoning());
            response.put("targetPrice", signal.getTargetPrice());
            response.put("stopLoss", signal.getStopLoss());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in AI analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AI analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Get market sentiment analysis
     */
    @PostMapping("/sentiment")
    public ResponseEntity<?> getMarketSentiment(@RequestBody AIAnalysisRequest request) {
        try {
            request.setSymbol(request.getSymbol().toUpperCase());
            Stock stock = stockRepository.findBySymbol(request.getSymbol())
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + request.getSymbol()));

            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData);

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available"));
            }

            String sentiment = aiStrategyService.getMarketSentiment(stock, marketData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", request.getSymbol());
            response.put("sentiment", sentiment);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting sentiment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Detect chart patterns
     */
    @PostMapping("/patterns")
    public ResponseEntity<?> detectPatterns(@RequestBody AIAnalysisRequest request) {
        try {
            request.setSymbol(request.getSymbol().toUpperCase());
            Stock stock = stockRepository.findBySymbol(request.getSymbol())
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + request.getSymbol()));

            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData);

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available"));
            }

            String patterns = aiStrategyService.detectPatterns(stock, marketData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", request.getSymbol());
            response.put("patterns", patterns);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error detecting patterns", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Detect market regime
     */
    @PostMapping("/regime")
    public ResponseEntity<?> detectMarketRegime(@RequestBody AIAnalysisRequest request) {
        try {
            request.setSymbol(request.getSymbol().toUpperCase());
            Stock stock = stockRepository.findBySymbol(request.getSymbol())
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + request.getSymbol()));

            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData);

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available"));
            }

            String regime = aiStrategyService.detectMarketRegime(stock, marketData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", request.getSymbol());
            response.put("regime", regime);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error detecting regime", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Request DTO for AI analysis
     */
    @Data
    public static class AIAnalysisRequest {
        private String symbol;
        private String analysisType; // sentiment, pattern, regime, or comprehensive
    }
}
