package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.service.indicator.TechnicalIndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MACD-based trading strategy
 * Buy when MACD line crosses above signal line
 * Sell when MACD line crosses below signal line
 */
@Component
@Slf4j
public class MACDStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    private int fastPeriod = 12;
    private int slowPeriod = 26;
    private int signalPeriod = 9;

    public MACDStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "MACD Strategy";
        this.description = "Trend-following momentum strategy using MACD crossovers";
        this.minDataPoints = slowPeriod + signalPeriod + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for MACD analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            // Calculate current MACD
            double[] currentMACD = indicatorService.calculateMACD(marketData, fastPeriod, slowPeriod, signalPeriod);
            double macdLine = currentMACD[0];
            double signalLine = currentMACD[1];
            double histogram = currentMACD[2];

            // Calculate previous MACD for crossover detection
            List<MarketData> previousData = marketData.subList(0, marketData.size() - 1);
            double[] previousMACD = indicatorService.calculateMACD(previousData, fastPeriod, slowPeriod, signalPeriod);
            double prevMacdLine = previousMACD[0];
            double prevSignalLine = previousMACD[1];

            log.debug("MACD for {}: Line={}, Signal={}, Histogram={}",
                    stock.getSymbol(), macdLine, signalLine, histogram);

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            // Detect crossovers
            boolean bullishCrossover = prevMacdLine <= prevSignalLine && macdLine > signalLine;
            boolean bearishCrossover = prevMacdLine >= prevSignalLine && macdLine < signalLine;

            if (bullishCrossover) {
                // MACD crossed above signal line - buy signal
                signalType = SignalType.BUY;
                confidenceScore = calculateCrossoverConfidence(histogram, true);
                reasoning = String.format("Bullish MACD crossover detected. MACD: %.4f, Signal: %.4f, Histogram: %.4f",
                        macdLine, signalLine, histogram);
            } else if (bearishCrossover) {
                // MACD crossed below signal line - sell signal
                signalType = SignalType.SELL;
                confidenceScore = calculateCrossoverConfidence(histogram, false);
                reasoning = String.format("Bearish MACD crossover detected. MACD: %.4f, Signal: %.4f, Histogram: %.4f",
                        macdLine, signalLine, histogram);
            } else {
                // No crossover - check histogram for momentum
                if (histogram > 0 && macdLine > signalLine) {
                    signalType = SignalType.HOLD;
                    confidenceScore = 0.6;
                    reasoning = String.format("Bullish momentum continuing. Histogram: %.4f", histogram);
                } else if (histogram < 0 && macdLine < signalLine) {
                    signalType = SignalType.HOLD;
                    confidenceScore = 0.4;
                    reasoning = String.format("Bearish momentum continuing. Histogram: %.4f", histogram);
                } else {
                    signalType = SignalType.HOLD;
                    confidenceScore = 0.5;
                    reasoning = "No clear MACD signal";
                }
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error calculating MACD for {}: {}", stock.getSymbol(), e.getMessage());
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in MACD calculation");
        }
    }

    /**
     * Calculate confidence based on histogram strength
     */
    private double calculateCrossoverConfidence(double histogram, boolean isBullish) {
        double absHistogram = Math.abs(histogram);

        // Higher histogram magnitude = stronger signal
        if (isBullish && histogram > 0) {
            return Math.min(0.9, 0.65 + (absHistogram * 10)); // Scale histogram to confidence
        } else if (!isBullish && histogram < 0) {
            return Math.min(0.9, 0.65 + (absHistogram * 10));
        }

        return 0.6; // Base confidence for crossover
    }

    @Override
    public String getStrategyName() {
        return strategyName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    // Configuration setters
    public void setFastPeriod(int fastPeriod) {
        this.fastPeriod = fastPeriod;
        this.minDataPoints = slowPeriod + signalPeriod + 5;
    }

    public void setSlowPeriod(int slowPeriod) {
        this.slowPeriod = slowPeriod;
        this.minDataPoints = slowPeriod + signalPeriod + 5;
    }

    public void setSignalPeriod(int signalPeriod) {
        this.signalPeriod = signalPeriod;
        this.minDataPoints = slowPeriod + signalPeriod + 5;
    }
}
