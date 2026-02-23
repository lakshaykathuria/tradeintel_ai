package com.tradeintel.ai.controller;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import com.tradeintel.ai.service.AIStrategyService;
import com.tradeintel.ai.service.NewsFetcherService;
import com.tradeintel.ai.service.TradingStrategyService;
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
 * REST controller for executing trading strategies on-demand
 */
@RestController
@RequestMapping("/execute")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StrategyExecutionController {

    private final TradingStrategyService strategyService;
    private final StockRepository stockRepository;
    private final MarketDataRepository marketDataRepository;
    private final AIStrategyService aiStrategyService;
    private final NewsFetcherService newsFetcherService;

    /**
     * Execute a single strategy on a stock
     */
    @PostMapping("/strategy")
    public ResponseEntity<?> executeStrategy(@RequestBody StrategyExecutionRequest request) {
        try {
            // Normalize symbol to uppercase to match stored data
            String symbol = request.getSymbol().toUpperCase().trim();
            log.info("Executing strategy {} on {}", request.getStrategyName(), symbol);

            // Get or create stock
            Stock stock = stockRepository.findBySymbol(symbol)
                    .orElseGet(() -> {
                        Stock newStock = new Stock();
                        newStock.setSymbol(symbol);
                        newStock.setName(symbol);
                        newStock.setExchange("NSE"); // Default to NSE
                        newStock.setIsActive(true);
                        return stockRepository.save(newStock);
                    });

            // Get market data — repository returns DESC (newest first),
            // but all strategy indicators assume ASC (oldest first)
            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData); // oldest → newest

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available for " + symbol
                                + ". Please use 'Fetch Historical Data' first."));
            }

            // Execute strategy
            TradeSignal signal = strategyService.executeStrategy(
                    request.getStrategyName(),
                    stock,
                    marketData);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("signal", signal.getSignalType().toString());
            response.put("confidence", signal.getConfidenceScore());
            response.put("reasoning", signal.getReasoning());
            response.put("targetPrice", signal.getTargetPrice());
            response.put("stopLoss", signal.getStopLoss());
            response.put("strategy", request.getStrategyName());
            response.put("symbol", request.getSymbol());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid strategy execution request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing strategy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute strategy: " + e.getMessage()));
        }
    }

    /**
     * Execute multiple strategies on a stock
     */
    @PostMapping("/multiple")
    public ResponseEntity<?> executeMultipleStrategies(@RequestBody MultiStrategyRequest request) {
        try {
            // Normalize symbol to uppercase to match stored data
            String symbol = request.getSymbol().toUpperCase().trim();
            log.info("Executing {} strategies on {}", request.getStrategyNames().size(), symbol);

            // Get or create stock
            Stock stock = stockRepository.findBySymbol(symbol)
                    .orElseGet(() -> {
                        Stock newStock = new Stock();
                        newStock.setSymbol(symbol);
                        newStock.setName(symbol);
                        newStock.setExchange("NSE"); // Default to NSE
                        newStock.setIsActive(true);
                        return stockRepository.save(newStock);
                    });

            // Get market data — repository returns DESC (newest first),
            // but all strategy indicators assume ASC (oldest first)
            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData); // oldest → newest

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data available for " + symbol
                                + ". Please use 'Fetch Historical Data' first."));
            }

            // Execute all strategies
            List<TradeSignal> signals = strategyService.executeMultipleStrategies(
                    request.getStrategyNames(),
                    stock,
                    marketData);

            // Build response with consensus
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", request.getSymbol());
            response.put("totalStrategies", signals.size());

            long buyCount = signals.stream()
                    .filter(s -> s.getSignalType().toString().equals("BUY"))
                    .count();
            long sellCount = signals.stream()
                    .filter(s -> s.getSignalType().toString().equals("SELL"))
                    .count();
            long holdCount = signals.stream()
                    .filter(s -> s.getSignalType().toString().equals("HOLD"))
                    .count();

            response.put("buyVotes", buyCount);
            response.put("sellVotes", sellCount);
            response.put("holdVotes", holdCount);

            // Determine consensus
            String consensus = "HOLD";
            if (buyCount > sellCount && buyCount > holdCount) {
                consensus = "BUY";
            } else if (sellCount > buyCount && sellCount > holdCount) {
                consensus = "SELL";
            }
            response.put("consensus", consensus);

            // Add individual signals
            List<Map<String, Object>> signalDetails = signals.stream()
                    .map(signal -> {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("signal", signal.getSignalType().toString());
                        detail.put("confidence", signal.getConfidenceScore());
                        detail.put("reasoning", signal.getReasoning());
                        return detail;
                    })
                    .toList();
            response.put("signals", signalDetails);

            // Get AI's own view on all the signals — enriched with news headlines
            try {
                List<String> headlines = newsFetcherService.fetchHeadlines(symbol);
                Double avgBuyPrice = request.getAvgBuyPrice();
                Map<String, Object> aiView = aiStrategyService.synthesizeStrategiesWithAI(
                        stock, marketData, signalDetails, headlines, avgBuyPrice);
                response.put("aiView", aiView);
            } catch (Exception aiEx) {
                log.warn("AI synthesis failed, continuing without it: {}", aiEx.getMessage());
                response.put("aiView", Map.of(
                        "aiSignal", "HOLD",
                        "aiConfidence", 0.0,
                        "aiReasoning", "AI analysis unavailable: " + aiEx.getMessage()));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error executing multiple strategies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute strategies: " + e.getMessage()));
        }
    }

    /**
     * Get list of available strategy implementations
     */
    @GetMapping("/available-strategies")
    public ResponseEntity<?> getAvailableStrategies() {
        Map<String, String> strategies = new HashMap<>();
        // Bean names must match exactly how Spring registers them:
        // Classes starting with 2+ uppercase letters keep their original case
        // (RSIStrategy, MACDStrategy)
        // Others get first letter lowercased (bollingerBandsStrategy,
        // movingAverageCrossoverStrategy, etc.)
        strategies.put("RSIStrategy", "RSI Strategy - Momentum-based overbought/oversold");
        strategies.put("MACDStrategy", "MACD Strategy - Trend following with crossovers");
        strategies.put("bollingerBandsStrategy", "Bollinger Bands - Volatility-based mean reversion");
        strategies.put("movingAverageCrossoverStrategy", "MA Crossover - Golden/Death cross detection");
        strategies.put("stochasticStrategy", "Stochastic Oscillator - Short-term reversals");
        strategies.put("volumeBreakoutStrategy", "Volume Breakout - High volume price moves");
        strategies.put("supportResistanceStrategy", "Support/Resistance - Key level trading");
        strategies.put("newsSentimentStrategy", "News Sentiment - AI-powered news & trend analysis");

        return ResponseEntity.ok(strategies);
    }

    /**
     * Request DTO for single strategy execution
     */
    @Data
    public static class StrategyExecutionRequest {
        private String strategyName;
        private String symbol;
    }

    /**
     * Request DTO for multiple strategy execution
     */
    @Data
    public static class MultiStrategyRequest {
        private List<String> strategyNames;
        private String symbol;
        /** Optional: user's average buy price for personalized AI P&L analysis */
        private Double avgBuyPrice;
    }
}
