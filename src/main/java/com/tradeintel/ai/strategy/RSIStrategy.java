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
 * RSI-based trading strategy
 * Buy when RSI < oversoldThreshold (default 30)
 * Sell when RSI > overboughtThreshold (default 70)
 */
@Component
@Slf4j
public class RSIStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    private int rsiPeriod = 14;
    private double oversoldThreshold = 40.0; // Widened from 30 for large-cap stocks
    private double overboughtThreshold = 60.0; // Widened from 70 for large-cap stocks

    public RSIStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "RSI Strategy";
        this.description = "Momentum-based strategy using Relative Strength Index";
        this.minDataPoints = rsiPeriod + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for RSI analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            double rsi = indicatorService.calculateRSI(marketData, rsiPeriod);
            log.debug("RSI for {}: {}", stock.getSymbol(), rsi);

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            if (rsi < oversoldThreshold) {
                // Oversold - potential buy signal
                signalType = SignalType.BUY;
                confidenceScore = calculateConfidence(rsi, oversoldThreshold, true);
                reasoning = String.format("RSI %.2f is below oversold threshold %.2f - potential reversal upward",
                        rsi, oversoldThreshold);
            } else if (rsi > overboughtThreshold) {
                // Overbought - potential sell signal
                signalType = SignalType.SELL;
                confidenceScore = calculateConfidence(rsi, overboughtThreshold, false);
                reasoning = String.format("RSI %.2f is above overbought threshold %.2f - potential reversal downward",
                        rsi, overboughtThreshold);
            } else {
                // Neutral zone
                signalType = SignalType.HOLD;
                confidenceScore = 0.5;
                reasoning = String.format("RSI %.2f is in neutral zone (%.2f - %.2f)",
                        rsi, oversoldThreshold, overboughtThreshold);
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error calculating RSI for {}: {}", stock.getSymbol(), e.getMessage());
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in RSI calculation");
        }
    }

    /**
     * Calculate confidence score based on how extreme the RSI value is
     */
    private double calculateConfidence(double rsi, double threshold, boolean isBuySignal) {
        double distance;
        if (isBuySignal) {
            // For buy signals, lower RSI = higher confidence
            distance = threshold - rsi;
            return Math.min(0.95, 0.6 + (distance / threshold) * 0.35);
        } else {
            // For sell signals, higher RSI = higher confidence
            distance = rsi - threshold;
            return Math.min(0.95, 0.6 + (distance / (100 - threshold)) * 0.35);
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

    // Getters and setters for configuration
    public void setRsiPeriod(int rsiPeriod) {
        this.rsiPeriod = rsiPeriod;
        this.minDataPoints = rsiPeriod + 5;
    }

    public void setOversoldThreshold(double oversoldThreshold) {
        this.oversoldThreshold = oversoldThreshold;
    }

    public void setOverboughtThreshold(double overboughtThreshold) {
        this.overboughtThreshold = overboughtThreshold;
    }
}
