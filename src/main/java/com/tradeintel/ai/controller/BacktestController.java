package com.tradeintel.ai.controller;

import com.tradeintel.ai.dto.BacktestResult;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.repository.StockRepository;
import com.tradeintel.ai.service.BacktestService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for backtesting operations
 */
@RestController
@RequestMapping("/backtest")
@RequiredArgsConstructor
@Slf4j
public class BacktestController {

        private final BacktestService backtestService;
        private final StockRepository stockRepository;

        /**
         * Run a backtest
         */
        @PostMapping
        public ResponseEntity<?> runBacktest(@RequestBody BacktestRequest request) {
                try {
                        // Validate request
                        if (request.getStrategyName() == null || request.getSymbol() == null) {
                                return ResponseEntity.badRequest()
                                                .body(Map.of("error", "Strategy name and symbol are required"));
                        }

                        // Get stock
                        Stock stock = stockRepository.findBySymbol(request.getSymbol())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Stock not found: " + request.getSymbol()));

                        // Set defaults — parse String dates (frontend sends ISO-8601 with 'Z' suffix)
                        LocalDateTime startDate = parseDate(request.getStartDate(),
                                        LocalDateTime.now().minusMonths(6));
                        LocalDateTime endDate = parseDate(request.getEndDate(),
                                        LocalDateTime.now());

                        BigDecimal initialCapital = request.getInitialCapital() != null
                                        ? request.getInitialCapital()
                                        : BigDecimal.valueOf(100000);

                        // Run backtest
                        log.info("Running backtest for strategy {} on {} from {} to {}",
                                        request.getStrategyName(), request.getSymbol(), startDate, endDate);

                        BacktestResult result = backtestService.runBacktest(
                                        request.getStrategyName(),
                                        stock,
                                        startDate,
                                        endDate,
                                        initialCapital);

                        return ResponseEntity.ok(result);

                } catch (IllegalArgumentException e) {
                        log.error("Invalid backtest request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(Map.of("error", e.getMessage()));
                } catch (Exception e) {
                        log.error("Error running backtest", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", "Failed to run backtest: " + e.getMessage()));
                }
        }

        /**
         * Get backtest summary (quick metrics without full equity curve)
         */
        @PostMapping("/summary")
        public ResponseEntity<?> getBacktestSummary(@RequestBody BacktestRequest request) {
                try {
                        BacktestResult fullResult = backtestService.runBacktest(
                                        request.getStrategyName(),
                                        stockRepository.findBySymbol(request.getSymbol())
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "Stock not found")),
                                        parseDate(request.getStartDate(), LocalDateTime.now().minusMonths(6)),
                                        parseDate(request.getEndDate(), LocalDateTime.now()),
                                        request.getInitialCapital() != null ? request.getInitialCapital()
                                                        : BigDecimal.valueOf(100000));

                        // Return summary without equity curve and trade details
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("strategyName", fullResult.getStrategyName());
                        summary.put("symbol", fullResult.getSymbol());
                        summary.put("totalReturn", fullResult.getTotalReturn());
                        summary.put("totalReturnPercentage", fullResult.getTotalReturnPercentage());
                        summary.put("sharpeRatio", fullResult.getSharpeRatio());
                        summary.put("maxDrawdownPercentage", fullResult.getMaxDrawdownPercentage());
                        summary.put("totalTrades", fullResult.getTotalTrades());
                        summary.put("winRate", fullResult.getWinRate());
                        summary.put("profitFactor", fullResult.getProfitFactor());

                        return ResponseEntity.ok(summary);

                } catch (Exception e) {
                        log.error("Error getting backtest summary", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("error", e.getMessage()));
                }
        }

        /**
         * Parse an ISO-8601 date string to LocalDateTime.
         * Handles strings with 'Z' suffix (UTC) as sent by JavaScript's toISOString().
         */
        private LocalDateTime parseDate(String date, LocalDateTime defaultValue) {
                if (date == null || date.isBlank())
                        return defaultValue;
                try {
                        // Handle "2025-08-22T00:00:00.000Z" style (JavaScript toISOString)
                        return OffsetDateTime.parse(date).toLocalDateTime();
                } catch (Exception e1) {
                        try {
                                return LocalDateTime.parse(date);
                        } catch (Exception e2) {
                                log.warn("Could not parse date '{}': {}", date, e2.getMessage());
                                return defaultValue;
                        }
                }
        }

        /**
         * Request DTO for backtest
         */
        @Data
        public static class BacktestRequest {
                private String strategyName;
                private String symbol;
                private String startDate; // ISO-8601 string from frontend
                private String endDate; // ISO-8601 string from frontend
                private BigDecimal initialCapital;
        }
}
