package com.tradeintel.ai.controller;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import com.tradeintel.ai.service.AIStrategyService;
import com.tradeintel.ai.service.NewsFetcherService;
import com.tradeintel.ai.service.TradingStrategyService;
import com.tradeintel.ai.strategy.StrategyScoreEngine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final StrategyScoreEngine strategyScoreEngine;

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
        // Original strategies
        strategies.put("RSIStrategy", "RSI Strategy — momentum overbought/oversold");
        strategies.put("MACDStrategy", "MACD Strategy — trend following with crossovers");
        strategies.put("bollingerBandsStrategy", "Bollinger Bands — volatility mean reversion");
        strategies.put("movingAverageCrossoverStrategy", "MA Crossover — golden/death cross");
        strategies.put("stochasticStrategy", "Stochastic Oscillator — short-term reversals");
        strategies.put("volumeBreakoutStrategy", "Volume Breakout — high-volume price moves");
        strategies.put("supportResistanceStrategy", "Support/Resistance — key level trading");
        strategies.put("newsSentimentStrategy", "News Sentiment — AI-powered news analysis");
        // New strategies
        strategies.put("VWAPStrategy", "VWAP — institutional fair-value crossover");
        strategies.put("ATRVolatilityStrategy", "ATR Volatility Breakout — swing trading");
        strategies.put("supertrendStrategy", "Supertrend — NSE/Indian market trend follower");
        strategies.put("donchianBreakoutStrategy", "Donchian Breakout — Turtle Trading");
        strategies.put("ADXTrendStrategy", "ADX Trend Strength — trades only trending markets");
        strategies.put("meanReversionStrategy", "Mean Reversion — range-bound market strategy");
        strategies.put("gapTradingStrategy", "Gap Trading — gap+volume momentum signal");

        return ResponseEntity.ok(strategies);
    }

    /**
     * Run the Strategy Scoring Engine across all 14 strategies and return
     * a weighted composite signal with full per-strategy breakdown.
     */
    @PostMapping("/scored")
    public ResponseEntity<?> executeScoredStrategies(@RequestBody ScoredRequest request) {
        try {
            String symbol = request.getSymbol().toUpperCase().trim();
            log.info("Running Strategy Scoring Engine on {}", symbol);

            Stock stock = stockRepository.findBySymbol(symbol)
                    .orElseGet(() -> {
                        Stock s = new Stock();
                        s.setSymbol(symbol); s.setName(symbol);
                        s.setExchange("NSE"); s.setIsActive(true);
                        return stockRepository.save(s);
                    });

            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId());
            Collections.reverse(marketData);

            if (marketData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No market data for " + symbol + ". Fetch historical data first."));
            }

            StrategyScoreEngine.ScoredResult result = strategyScoreEngine.score(stock, marketData);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("symbol",        symbol);
            response.put("signal",        result.compositeSignal().getSignalType().toString());
            response.put("confidence",    result.compositeSignal().getConfidenceScore());
            response.put("totalScore",    result.totalScore());
            response.put("buyVotes",      result.buyVotes());
            response.put("sellVotes",     result.sellVotes());
            response.put("holdVotes",     result.holdVotes());
            response.put("summary",       result.summary());
            response.put("breakdown",     result.breakdown().stream().map(v -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("strategy",     v.strategyName());
                m.put("signal",       v.signal());
                m.put("confidence",   Math.round(v.confidence() * 100) + "%");
                m.put("weight",       v.weight());
                m.put("weightedVote", Math.round(v.weightedVote() * 100.0) / 100.0);
                m.put("reasoning",    v.reasoning());
                return m;
            }).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error running scoring engine", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Scoring engine failed: " + e.getMessage()));
        }
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

    /** Request DTO for the scoring engine endpoint. */
    @Data
    public static class ScoredRequest {
        private String symbol;
    }
}
