package com.tradeintel.ai.controller;

import com.tradeintel.ai.service.indicator.TechnicalIndicatorService;
import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for technical indicator calculations
 */
@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
@Slf4j
public class TechnicalIndicatorController {

    private final TechnicalIndicatorService indicatorService;
    private final MarketDataRepository marketDataRepository;

    /**
     * Calculate RSI for a symbol
     */
    @GetMapping("/rsi")
    public ResponseEntity<?> calculateRSI(@RequestParam String symbol,
            @RequestParam(defaultValue = "14") int period) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double rsi = indicatorService.calculateRSI(marketData, period);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "RSI");
            response.put("period", period);
            response.put("value", rsi);
            response.put("interpretation", getRSIInterpretation(rsi));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error calculating RSI for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate RSI"));
        }
    }

    /**
     * Calculate MACD for a symbol
     */
    @GetMapping("/macd")
    public ResponseEntity<?> calculateMACD(@RequestParam String symbol) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double[] macd = indicatorService.calculateMACD(marketData);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "MACD");
            response.put("macdLine", macd[0]);
            response.put("signalLine", macd[1]);
            response.put("histogram", macd[2]);
            response.put("interpretation", getMACDInterpretation(macd[0], macd[1]));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating MACD for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate MACD"));
        }
    }

    /**
     * Calculate SMA for a symbol
     */
    @GetMapping("/sma")
    public ResponseEntity<?> calculateSMA(@RequestParam String symbol,
            @RequestParam(defaultValue = "20") int period) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double sma = indicatorService.calculateSMA(marketData, period);
            double currentPrice = marketData.get(marketData.size() - 1).getClosePrice().doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "SMA");
            response.put("period", period);
            response.put("value", sma);
            response.put("currentPrice", currentPrice);
            response.put("priceVsSMA", currentPrice > sma ? "Above SMA" : "Below SMA");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating SMA for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate SMA"));
        }
    }

    /**
     * Calculate EMA for a symbol
     */
    @GetMapping("/ema")
    public ResponseEntity<?> calculateEMA(@RequestParam String symbol,
            @RequestParam(defaultValue = "20") int period) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double ema = indicatorService.calculateEMA(marketData, period);
            double currentPrice = marketData.get(marketData.size() - 1).getClosePrice().doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "EMA");
            response.put("period", period);
            response.put("value", ema);
            response.put("currentPrice", currentPrice);
            response.put("priceVsEMA", currentPrice > ema ? "Above EMA" : "Below EMA");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating EMA for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate EMA"));
        }
    }

    /**
     * Calculate Bollinger Bands for a symbol
     */
    @GetMapping("/bollinger")
    public ResponseEntity<?> calculateBollingerBands(@RequestParam String symbol,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(defaultValue = "2.0") double stdDev) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double[] bands = indicatorService.calculateBollingerBands(marketData, period, stdDev);
            double currentPrice = marketData.get(marketData.size() - 1).getClosePrice().doubleValue();

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "Bollinger Bands");
            response.put("period", period);
            response.put("stdDev", stdDev);
            response.put("upperBand", bands[0]);
            response.put("middleBand", bands[1]);
            response.put("lowerBand", bands[2]);
            response.put("currentPrice", currentPrice);
            response.put("bandWidth", ((bands[0] - bands[2]) / bands[1]) * 100);
            response.put("interpretation", getBollingerInterpretation(currentPrice, bands));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating Bollinger Bands for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate Bollinger Bands"));
        }
    }

    /**
     * Calculate ATR for a symbol
     */
    @GetMapping("/atr")
    public ResponseEntity<?> calculateATR(@RequestParam String symbol,
            @RequestParam(defaultValue = "14") int period) {
        try {
            List<MarketData> marketData = marketDataRepository.findByStockSymbolOrderByTimestampDesc(symbol);

            if (marketData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No market data found for symbol: " + symbol));
            }

            double atr = indicatorService.calculateATR(marketData, period);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("indicator", "ATR");
            response.put("period", period);
            response.put("value", atr);
            response.put("interpretation", "Volatility measure - higher values indicate higher volatility");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating ATR for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to calculate ATR"));
        }
    }

    // Helper methods for interpretations
    private String getRSIInterpretation(double rsi) {
        if (rsi < 30)
            return "Oversold - potential buy signal";
        if (rsi > 70)
            return "Overbought - potential sell signal";
        return "Neutral";
    }

    private String getMACDInterpretation(double macdLine, double signalLine) {
        if (macdLine > signalLine)
            return "Bullish - MACD above signal line";
        if (macdLine < signalLine)
            return "Bearish - MACD below signal line";
        return "Neutral";
    }

    private String getBollingerInterpretation(double price, double[] bands) {
        if (price <= bands[2])
            return "Price at/below lower band - oversold";
        if (price >= bands[0])
            return "Price at/above upper band - overbought";
        if (price > bands[1])
            return "Price above middle band - bullish";
        return "Price below middle band - bearish";
    }
}
