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
 * Stochastic Oscillator Strategy
 * 
 * The Stochastic Oscillator compares a stock's closing price to its price range
 * over a period.
 * - Buy when oversold (%K < 20) and %K crosses above %D (bullish crossover)
 * - Sell when overbought (%K > 80) and %K crosses below %D (bearish crossover)
 */
@Component
@Slf4j
public class StochasticStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    // Configuration parameters
    private int kPeriod = 14;
    private int dPeriod = 3;
    private double oversoldLevel = 25.0; // Widened from 20 for large-caps
    private double overboughtLevel = 75.0; // Widened from 80 for large-caps

    public StochasticStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "Stochastic Oscillator Strategy";
        this.description = "Momentum indicator comparing closing price to price range over time";
        this.minDataPoints = kPeriod + dPeriod + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for Stochastic analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            // Calculate current %K and %D
            double[] currentStochastic = indicatorService.calculateStochastic(marketData, kPeriod);
            double percentK = currentStochastic[0];
            double percentD = currentStochastic[1];

            // Calculate previous %K and %D for crossover detection
            List<MarketData> previousData = marketData.subList(0, marketData.size() - 1);
            double[] previousStochastic = indicatorService.calculateStochastic(previousData, kPeriod);
            double prevPercentK = previousStochastic[0];
            double prevPercentD = previousStochastic[1];

            log.debug("Stochastic for {}: %K={}, %D={}",
                    stock.getSymbol(), String.format("%.2f", percentK), String.format("%.2f", percentD));

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            // Detect crossovers (used to boost confidence, not as hard requirement)
            boolean bullishCrossover = prevPercentK <= prevPercentD && percentK > percentD;
            boolean bearishCrossover = prevPercentK >= prevPercentD && percentK < percentD;

            if (percentK < oversoldLevel) {
                // Oversold zone — BUY signal (crossover gives higher confidence)
                signalType = SignalType.BUY;
                confidenceScore = calculateConfidence(percentK, oversoldLevel, true);
                if (bullishCrossover)
                    confidenceScore = Math.min(0.95, confidenceScore + 0.10);
                reasoning = String.format(
                        "Stochastic oversold (%%K=%.2f < %.0f)%s. Reversal signal.",
                        percentK, oversoldLevel,
                        bullishCrossover ? " with bullish crossover" : "");
            } else if (percentK > overboughtLevel) {
                // Overbought zone — SELL signal (crossover gives higher confidence)
                signalType = SignalType.SELL;
                confidenceScore = calculateConfidence(percentK, overboughtLevel, false);
                if (bearishCrossover)
                    confidenceScore = Math.min(0.95, confidenceScore + 0.10);
                reasoning = String.format(
                        "Stochastic overbought (%%K=%.2f > %.0f)%s. Reversal signal.",
                        percentK, overboughtLevel,
                        bearishCrossover ? " with bearish crossover" : "");
            } else {
                // Neutral zone
                signalType = SignalType.HOLD;
                confidenceScore = 0.5;
                reasoning = String.format(
                        "Stochastic neutral (%%K=%.2f, %%D=%.2f) — no clear signal.",
                        percentK, percentD);
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error calculating Stochastic for {}: {}", stock.getSymbol(), e.getMessage(), e);
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in Stochastic calculation");
        }
    }

    /**
     * Calculate confidence score based on how extreme the stochastic value is
     */
    private double calculateConfidence(double percentK, double threshold, boolean isBuySignal) {
        if (isBuySignal) {
            // For buy signals, lower %K = higher confidence
            double distance = threshold - percentK;
            return Math.min(0.95, 0.7 + (distance / threshold) * 0.25);
        } else {
            // For sell signals, higher %K = higher confidence
            double distance = percentK - threshold;
            return Math.min(0.95, 0.7 + (distance / (100 - threshold)) * 0.25);
        }
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
    public void setKPeriod(int kPeriod) {
        this.kPeriod = kPeriod;
        this.minDataPoints = kPeriod + dPeriod + 5;
    }

    public void setDPeriod(int dPeriod) {
        this.dPeriod = dPeriod;
        this.minDataPoints = kPeriod + dPeriod + 5;
    }

    public void setOversoldLevel(double oversoldLevel) {
        this.oversoldLevel = oversoldLevel;
    }

    public void setOverboughtLevel(double overboughtLevel) {
        this.overboughtLevel = overboughtLevel;
    }
}
