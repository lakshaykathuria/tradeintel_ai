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
 * Bollinger Bands Mean Reversion Strategy
 * Buy when price touches lower band (oversold)
 * Sell when price touches upper band (overbought)
 * Also detects band squeeze for volatility breakouts
 */
@Component
@Slf4j
public class BollingerBandsStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    private int period = 20;
    private double stdDevMultiplier = 2.0;
    private double touchThreshold = 0.02; // 2% threshold for "touching" bands

    public BollingerBandsStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "Bollinger Bands Strategy";
        this.description = "Mean reversion strategy using Bollinger Bands";
        this.minDataPoints = period + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for Bollinger Bands analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            double[] bands = indicatorService.calculateBollingerBands(marketData, period, stdDevMultiplier);
            double upperBand = bands[0];
            double middleBand = bands[1];
            double lowerBand = bands[2];

            double currentPrice = marketData.get(marketData.size() - 1).getClosePrice().doubleValue();
            double bandWidth = ((upperBand - lowerBand) / middleBand) * 100;

            log.debug("Bollinger Bands for {}: Upper={}, Middle={}, Lower={}, Price={}, Width={}%",
                    stock.getSymbol(), upperBand, middleBand, lowerBand, currentPrice, bandWidth);

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            // Calculate distance from bands
            double distanceFromLower = ((currentPrice - lowerBand) / lowerBand) * 100;
            double distanceFromUpper = ((upperBand - currentPrice) / upperBand) * 100;

            // Check if price is touching or below lower band (oversold)
            if (distanceFromLower <= touchThreshold) {
                signalType = SignalType.BUY;
                confidenceScore = calculateBandTouchConfidence(currentPrice, lowerBand, middleBand, true);
                reasoning = String.format("Price %.2f touching lower band %.2f (%.2f%% below). " +
                        "Mean reversion expected. Band width: %.2f%%",
                        currentPrice, lowerBand, Math.abs(distanceFromLower), bandWidth);
            }
            // Check if price is touching or above upper band (overbought)
            else if (distanceFromUpper <= touchThreshold) {
                signalType = SignalType.SELL;
                confidenceScore = calculateBandTouchConfidence(currentPrice, upperBand, middleBand, false);
                reasoning = String.format("Price %.2f touching upper band %.2f (%.2f%% above). " +
                        "Mean reversion expected. Band width: %.2f%%",
                        currentPrice, upperBand, Math.abs(distanceFromUpper), bandWidth);
            }
            // Check for band squeeze (low volatility, potential breakout)
            else if (bandWidth < 10) {
                signalType = SignalType.HOLD;
                confidenceScore = 0.5;
                reasoning = String.format("Band squeeze detected (width: %.2f%%). " +
                        "Low volatility - potential breakout coming. Wait for direction.",
                        bandWidth);
            }
            // Price in middle zone
            else {
                // Check position relative to middle band
                if (currentPrice > middleBand) {
                    signalType = SignalType.HOLD;
                    double percentAbove = ((currentPrice - middleBand) / middleBand) * 100;
                    confidenceScore = 0.55;
                    reasoning = String.format("Price %.2f is %.2f%% above middle band %.2f. " +
                            "Moderate bullish momentum.",
                            currentPrice, percentAbove, middleBand);
                } else {
                    signalType = SignalType.HOLD;
                    double percentBelow = ((middleBand - currentPrice) / middleBand) * 100;
                    confidenceScore = 0.45;
                    reasoning = String.format("Price %.2f is %.2f%% below middle band %.2f. " +
                            "Moderate bearish momentum.",
                            currentPrice, percentBelow, middleBand);
                }
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error calculating Bollinger Bands for {}: {}", stock.getSymbol(), e.getMessage());
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in Bollinger Bands calculation");
        }
    }

    /**
     * Calculate confidence based on how far price is from the band and middle
     */
    private double calculateBandTouchConfidence(double price, double band, double middle, boolean isBuySignal) {
        double distanceFromBand = Math.abs(price - band);
        double distanceFromMiddle = Math.abs(price - middle);
        double totalBandWidth = Math.abs(band - middle);

        // Closer to band = higher confidence for mean reversion
        double positionRatio = distanceFromBand / totalBandWidth;

        // Base confidence + bonus for being very close to band
        double baseConfidence = 0.7;
        double bonusConfidence = (1 - positionRatio) * 0.2;

        return Math.min(0.95, baseConfidence + bonusConfidence);
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
    public void setPeriod(int period) {
        this.period = period;
        this.minDataPoints = period + 5;
    }

    public void setStdDevMultiplier(double stdDevMultiplier) {
        this.stdDevMultiplier = stdDevMultiplier;
    }

    public void setTouchThreshold(double touchThreshold) {
        this.touchThreshold = touchThreshold;
    }
}
