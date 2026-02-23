package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.service.indicator.TechnicalIndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Volume Breakout Strategy
 * 
 * Identifies price breakouts confirmed by unusually high trading volume.
 * - Buy when price increases significantly on high volume
 * - Sell when price decreases significantly on high volume
 * - High volume = 2x or more of average volume
 */
@Component
@Slf4j
public class VolumeBreakoutStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    // Configuration parameters
    private int volumePeriod = 20;
    private double volumeMultiplier = 1.5; // Volume must be 1.5x average (2x is too rare for large-caps)
    private double priceChangeThreshold = 1.0; // 1% price change (2% is too rare for Nifty100 stocks)
    private double strongBreakoutThreshold = 2.5; // 2.5% for strong breakout

    public VolumeBreakoutStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "Volume Breakout Strategy";
        this.description = "Identifies significant price moves confirmed by high trading volume";
        this.minDataPoints = volumePeriod + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for Volume Breakout analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            MarketData current = marketData.get(marketData.size() - 1);
            MarketData previous = marketData.get(marketData.size() - 2);

            // Calculate average volume over the period
            double avgVolume = marketData.stream()
                    .skip(Math.max(0, marketData.size() - volumePeriod))
                    .mapToLong(MarketData::getVolume)
                    .average()
                    .orElse(0.0);

            long currentVolume = current.getVolume();
            double volumeRatio = currentVolume / avgVolume;

            // Calculate price change percentage
            double currentPrice = current.getClosePrice().doubleValue();
            double previousPrice = previous.getClosePrice().doubleValue();
            double priceChange = ((currentPrice - previousPrice) / previousPrice) * 100.0;

            // Check if it's a high volume day
            boolean highVolume = currentVolume > (avgVolume * volumeMultiplier);
            boolean significantPriceMove = Math.abs(priceChange) > priceChangeThreshold;
            boolean strongBreakout = Math.abs(priceChange) > strongBreakoutThreshold;

            log.debug("Volume Breakout for {}: Volume={} (Avg={}, Ratio={}x), PriceChange={}%",
                    stock.getSymbol(), currentVolume, (long) avgVolume,
                    String.format("%.2f", volumeRatio), String.format("%.2f", priceChange));

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            if (highVolume && significantPriceMove) {
                if (priceChange > 0) {
                    // Bullish breakout
                    signalType = SignalType.BUY;
                    confidenceScore = calculateConfidence(priceChange, volumeRatio, strongBreakout);
                    reasoning = String.format(
                            "Bullish volume breakout: Price surged %.2f%% on %.1fx average volume (%,d vs %,d avg). %s",
                            priceChange, volumeRatio, currentVolume, (long) avgVolume,
                            strongBreakout ? "STRONG breakout signal!" : "Moderate breakout.");

                    // Set target and stop loss
                    TradeSignal signal = createSignal(stock, signalType, confidenceScore, reasoning);
                    signal.setTargetPrice(BigDecimal.valueOf(currentPrice * 1.05)); // 5% target
                    signal.setStopLoss(BigDecimal.valueOf(currentPrice * 0.98)); // 2% stop loss
                    return signal;

                } else {
                    // Bearish breakout
                    signalType = SignalType.SELL;
                    confidenceScore = calculateConfidence(Math.abs(priceChange), volumeRatio, strongBreakout);
                    reasoning = String.format(
                            "Bearish volume breakout: Price dropped %.2f%% on %.1fx average volume (%,d vs %,d avg). %s",
                            Math.abs(priceChange), volumeRatio, currentVolume, (long) avgVolume,
                            strongBreakout ? "STRONG breakdown signal!" : "Moderate breakdown.");

                    // Set target and stop loss
                    TradeSignal signal = createSignal(stock, signalType, confidenceScore, reasoning);
                    signal.setTargetPrice(BigDecimal.valueOf(currentPrice * 0.95)); // 5% target
                    signal.setStopLoss(BigDecimal.valueOf(currentPrice * 1.02)); // 2% stop loss
                    return signal;
                }
            } else if (highVolume) {
                // High volume but insufficient price move
                signalType = SignalType.HOLD;
                confidenceScore = 0.6;
                reasoning = String.format(
                        "High volume (%.1fx avg) detected but price change (%.2f%%) below threshold. Watching for breakout.",
                        volumeRatio, priceChange);
            } else if (significantPriceMove) {
                // Significant price move but low volume - less reliable
                signalType = SignalType.HOLD;
                confidenceScore = 0.4;
                reasoning = String.format(
                        "Price moved %.2f%% but on low volume (%.1fx avg). Breakout not confirmed.",
                        priceChange, volumeRatio);
            } else {
                // Normal trading day
                signalType = SignalType.HOLD;
                confidenceScore = 0.5;
                reasoning = String.format(
                        "Normal trading: Volume %.1fx avg, Price %.2f%%. No breakout detected.",
                        volumeRatio, priceChange);
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error in Volume Breakout analysis for {}: {}", stock.getSymbol(), e.getMessage(), e);
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in volume analysis");
        }
    }

    /**
     * Calculate confidence score based on price change magnitude and volume ratio
     */
    private double calculateConfidence(double priceChange, double volumeRatio, boolean strongBreakout) {
        // Base confidence for confirmed breakout
        double baseConfidence = 0.70;

        // Bonus for strong price move (up to 0.15)
        double priceBonus = Math.min(0.15, (priceChange / 5.0) * 0.15);

        // Bonus for volume exceeding the multiplier threshold (up to 0.10)
        double volumeBonus = Math.min(0.10, ((volumeRatio - volumeMultiplier) / volumeMultiplier) * 0.10);

        // Extra bonus for very strong breakouts
        double strongBonus = strongBreakout ? 0.05 : 0.0;

        return Math.min(0.95, baseConfidence + priceBonus + volumeBonus + strongBonus);
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
    public void setVolumePeriod(int volumePeriod) {
        this.volumePeriod = volumePeriod;
        this.minDataPoints = volumePeriod + 5;
    }

    public void setVolumeMultiplier(double volumeMultiplier) {
        this.volumeMultiplier = volumeMultiplier;
    }

    public void setPriceChangeThreshold(double priceChangeThreshold) {
        this.priceChangeThreshold = priceChangeThreshold;
    }

    public void setStrongBreakoutThreshold(double strongBreakoutThreshold) {
        this.strongBreakoutThreshold = strongBreakoutThreshold;
    }
}
