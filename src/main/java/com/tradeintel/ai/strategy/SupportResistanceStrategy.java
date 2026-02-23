package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.service.indicator.TechnicalIndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Support and Resistance Strategy
 * 
 * Identifies key support and resistance levels based on historical price
 * action.
 * - Buy when price bounces off support level
 * - Sell when price hits resistance level
 * - Uses recent swing highs/lows to identify levels
 */
@Component
@Slf4j
public class SupportResistanceStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    // Configuration parameters
    private int lookbackPeriod = 20;
    private double bounceThreshold = 1.5; // 1.5% proximity (was 0.5% — too tight for daily bars)
    private int minTouches = 1; // 1 touch to confirm (2 too restrictive with 30-bar lookback)

    public SupportResistanceStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "Support & Resistance Strategy";
        this.description = "Trades based on key support and resistance levels from price action";
        this.minDataPoints = lookbackPeriod + 10;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for Support/Resistance analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            MarketData current = marketData.get(marketData.size() - 1);
            double currentPrice = current.getClosePrice().doubleValue();

            // Identify support and resistance levels
            List<Double> supportLevels = findSupportLevels(marketData);
            List<Double> resistanceLevels = findResistanceLevels(marketData);

            log.debug("Support/Resistance for {}: Current={}, Supports={}, Resistances={}",
                    stock.getSymbol(), String.format("%.2f", currentPrice),
                    supportLevels.size(), resistanceLevels.size());

            // Find nearest support and resistance
            Double nearestSupport = findNearestLevel(currentPrice, supportLevels, true);
            Double nearestResistance = findNearestLevel(currentPrice, resistanceLevels, false);

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            // Check if price is near support (potential buy)
            if (nearestSupport != null && isNearLevel(currentPrice, nearestSupport)) {
                // Check if price is bouncing (current close > previous close)
                MarketData previous = marketData.get(marketData.size() - 2);
                boolean bouncing = currentPrice > previous.getClosePrice().doubleValue();

                if (bouncing) {
                    signalType = SignalType.BUY;
                    confidenceScore = calculateSupportConfidence(currentPrice, nearestSupport, marketData);
                    reasoning = String.format(
                            "Price bouncing off support at ₹%.2f (current: ₹%.2f). Strong support level with %d historical touches.",
                            nearestSupport, currentPrice, countTouches(marketData, nearestSupport));

                    TradeSignal signal = createSignal(stock, signalType, confidenceScore, reasoning);
                    if (nearestResistance != null) {
                        signal.setTargetPrice(BigDecimal.valueOf(nearestResistance * 0.99)); // Just below resistance
                    }
                    signal.setStopLoss(BigDecimal.valueOf(nearestSupport * 0.98)); // Below support
                    return signal;
                } else {
                    signalType = SignalType.HOLD;
                    confidenceScore = 0.6;
                    reasoning = String.format(
                            "Price testing support at ₹%.2f (current: ₹%.2f). Waiting for bounce confirmation.",
                            nearestSupport, currentPrice);
                }
            }
            // Check if price is near resistance (potential sell)
            else if (nearestResistance != null && isNearLevel(currentPrice, nearestResistance)) {
                signalType = SignalType.SELL;
                confidenceScore = calculateResistanceConfidence(currentPrice, nearestResistance, marketData);
                reasoning = String.format(
                        "Price approaching resistance at ₹%.2f (current: ₹%.2f). Strong resistance level with %d historical touches.",
                        nearestResistance, currentPrice, countTouches(marketData, nearestResistance));

                TradeSignal signal = createSignal(stock, signalType, confidenceScore, reasoning);
                if (nearestSupport != null) {
                    signal.setTargetPrice(BigDecimal.valueOf(nearestSupport * 1.01)); // Just above support
                }
                signal.setStopLoss(BigDecimal.valueOf(nearestResistance * 1.02)); // Above resistance
                return signal;
            }
            // Price in between levels
            else {
                signalType = SignalType.HOLD;
                confidenceScore = 0.5;

                String supportInfo = nearestSupport != null ? String.format("Support: ₹%.2f", nearestSupport)
                        : "No nearby support";
                String resistanceInfo = nearestResistance != null
                        ? String.format("Resistance: ₹%.2f", nearestResistance)
                        : "No nearby resistance";

                reasoning = String.format(
                        "Price at ₹%.2f between levels. %s | %s. Waiting for price to reach key level.",
                        currentPrice, supportInfo, resistanceInfo);
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error in Support/Resistance analysis for {}: {}", stock.getSymbol(), e.getMessage(), e);
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in level calculation");
        }
    }

    /**
     * Find support levels from swing lows
     */
    private List<Double> findSupportLevels(List<MarketData> marketData) {
        List<Double> supports = new ArrayList<>();
        int dataSize = marketData.size();

        for (int i = 2; i < Math.min(dataSize - 2, lookbackPeriod); i++) {
            MarketData current = marketData.get(dataSize - 1 - i);
            MarketData prev1 = marketData.get(dataSize - i);
            MarketData prev2 = marketData.get(dataSize - i + 1);
            MarketData next1 = marketData.get(dataSize - 2 - i);
            MarketData next2 = marketData.get(dataSize - 3 - i);

            double currentLow = current.getLowPrice().doubleValue();

            // Check if this is a swing low (lower than surrounding lows)
            if (currentLow < prev1.getLowPrice().doubleValue() &&
                    currentLow < prev2.getLowPrice().doubleValue() &&
                    currentLow < next1.getLowPrice().doubleValue() &&
                    currentLow < next2.getLowPrice().doubleValue()) {
                supports.add(currentLow);
            }
        }

        return consolidateLevels(supports);
    }

    /**
     * Find resistance levels from swing highs
     */
    private List<Double> findResistanceLevels(List<MarketData> marketData) {
        List<Double> resistances = new ArrayList<>();
        int dataSize = marketData.size();

        for (int i = 2; i < Math.min(dataSize - 2, lookbackPeriod); i++) {
            MarketData current = marketData.get(dataSize - 1 - i);
            MarketData prev1 = marketData.get(dataSize - i);
            MarketData prev2 = marketData.get(dataSize - i + 1);
            MarketData next1 = marketData.get(dataSize - 2 - i);
            MarketData next2 = marketData.get(dataSize - 3 - i);

            double currentHigh = current.getHighPrice().doubleValue();

            // Check if this is a swing high (higher than surrounding highs)
            if (currentHigh > prev1.getHighPrice().doubleValue() &&
                    currentHigh > prev2.getHighPrice().doubleValue() &&
                    currentHigh > next1.getHighPrice().doubleValue() &&
                    currentHigh > next2.getHighPrice().doubleValue()) {
                resistances.add(currentHigh);
            }
        }

        return consolidateLevels(resistances);
    }

    /**
     * Consolidate nearby levels into single levels
     */
    private List<Double> consolidateLevels(List<Double> levels) {
        if (levels.isEmpty())
            return levels;

        List<Double> consolidated = new ArrayList<>();
        levels.sort(Double::compareTo);

        double currentLevel = levels.get(0);
        int count = 1;

        for (int i = 1; i < levels.size(); i++) {
            if (Math.abs(levels.get(i) - currentLevel) / currentLevel < 0.02) { // Within 2%
                currentLevel = (currentLevel * count + levels.get(i)) / (count + 1);
                count++;
            } else {
                if (count >= minTouches) {
                    consolidated.add(currentLevel);
                }
                currentLevel = levels.get(i);
                count = 1;
            }
        }

        if (count >= minTouches) {
            consolidated.add(currentLevel);
        }

        return consolidated;
    }

    /**
     * Find nearest level to current price
     */
    private Double findNearestLevel(double currentPrice, List<Double> levels, boolean isSupport) {
        return levels.stream()
                .filter(level -> isSupport ? level <= currentPrice : level >= currentPrice)
                .min((a, b) -> Double.compare(
                        Math.abs(currentPrice - a),
                        Math.abs(currentPrice - b)))
                .orElse(null);
    }

    /**
     * Check if price is near a level
     */
    private boolean isNearLevel(double price, double level) {
        double percentDiff = Math.abs((price - level) / level) * 100.0;
        return percentDiff <= bounceThreshold;
    }

    /**
     * Count how many times price touched this level
     */
    private int countTouches(List<MarketData> marketData, double level) {
        return (int) marketData.stream()
                .filter(data -> {
                    double low = data.getLowPrice().doubleValue();
                    double high = data.getHighPrice().doubleValue();
                    return isNearLevel(low, level) || isNearLevel(high, level);
                })
                .count();
    }

    /**
     * Calculate confidence for support bounce
     */
    private double calculateSupportConfidence(double currentPrice, double support, List<MarketData> marketData) {
        int touches = countTouches(
                marketData.subList(Math.max(0, marketData.size() - lookbackPeriod), marketData.size()), support);

        double baseConfidence = 0.65;
        double touchBonus = Math.min(0.20, touches * 0.05);
        double proximityBonus = 0.10 * (1 - Math.abs(currentPrice - support) / support / bounceThreshold);

        return Math.min(0.90, baseConfidence + touchBonus + proximityBonus);
    }

    /**
     * Calculate confidence for resistance rejection
     */
    private double calculateResistanceConfidence(double currentPrice, double resistance, List<MarketData> marketData) {
        int touches = countTouches(
                marketData.subList(Math.max(0, marketData.size() - lookbackPeriod), marketData.size()), resistance);

        double baseConfidence = 0.65;
        double touchBonus = Math.min(0.20, touches * 0.05);
        double proximityBonus = 0.10 * (1 - Math.abs(currentPrice - resistance) / resistance / bounceThreshold);

        return Math.min(0.90, baseConfidence + touchBonus + proximityBonus);
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
    public void setLookbackPeriod(int lookbackPeriod) {
        this.lookbackPeriod = lookbackPeriod;
        this.minDataPoints = lookbackPeriod + 10;
    }

    public void setBounceThreshold(double bounceThreshold) {
        this.bounceThreshold = bounceThreshold;
    }

    public void setMinTouches(int minTouches) {
        this.minTouches = minTouches;
    }
}
