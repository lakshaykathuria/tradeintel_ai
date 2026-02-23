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
 * Moving Average Crossover Strategy
 * Golden Cross: Short MA crosses above Long MA (bullish)
 * Death Cross: Short MA crosses below Long MA (bearish)
 */
@Component
@Slf4j
public class MovingAverageCrossoverStrategy extends AbstractTradingStrategy {

    private final TechnicalIndicatorService indicatorService;

    private int shortPeriod = 10;
    private int longPeriod = 20;
    private boolean useEMA = false; // Use SMA by default

    public MovingAverageCrossoverStrategy(TechnicalIndicatorService indicatorService) {
        this.indicatorService = indicatorService;
        this.strategyName = "MA Crossover Strategy";
        this.description = "Moving average crossover strategy — 10/20 SMA (short-term trend)";
        this.minDataPoints = longPeriod + 5;
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        if (!hasEnoughData(marketData)) {
            log.warn("Not enough data for MA Crossover analysis. Need at least {} data points", minDataPoints);
            return createSignal(stock, SignalType.HOLD, 0.0, "Insufficient data");
        }

        try {
            // Calculate current moving averages
            double shortMA = useEMA ? indicatorService.calculateEMA(marketData, shortPeriod)
                    : indicatorService.calculateSMA(marketData, shortPeriod);

            double longMA = useEMA ? indicatorService.calculateEMA(marketData, longPeriod)
                    : indicatorService.calculateSMA(marketData, longPeriod);

            // Calculate previous moving averages for crossover detection
            List<MarketData> previousData = marketData.subList(0, marketData.size() - 1);
            double prevShortMA = useEMA ? indicatorService.calculateEMA(previousData, shortPeriod)
                    : indicatorService.calculateSMA(previousData, shortPeriod);

            double prevLongMA = useEMA ? indicatorService.calculateEMA(previousData, longPeriod)
                    : indicatorService.calculateSMA(previousData, longPeriod);

            String maType = useEMA ? "EMA" : "SMA";
            log.debug("{} for {}: Short({})={}, Long({})={}",
                    maType, stock.getSymbol(), shortPeriod, shortMA, longPeriod, longMA);

            // Detect crossovers for reasoning
            boolean goldenCross = prevShortMA <= prevLongMA && shortMA > longMA;
            boolean deathCross = prevShortMA >= prevLongMA && shortMA < longMA;

            SignalType signalType;
            double confidenceScore;
            String reasoning;

            if (shortMA > longMA) {
                // Short MA above Long MA: uptrend — BUY / hold long
                signalType = SignalType.BUY;
                confidenceScore = calculateCrossoverConfidence(shortMA, longMA, true);
                double spread = ((shortMA - longMA) / longMA) * 100;
                if (goldenCross) {
                    reasoning = String.format(
                            "Golden Cross! %s(%d)=%.2f crossed ABOVE %s(%d)=%.2f — entering uptrend.",
                            maType, shortPeriod, shortMA, maType, longPeriod, longMA);
                } else {
                    reasoning = String.format(
                            "Uptrend: %s(%d)=%.2f is %.2f%% above %s(%d)=%.2f",
                            maType, shortPeriod, shortMA, spread, maType, longPeriod, longMA);
                }
            } else {
                // Short MA below Long MA: downtrend — SELL / stay flat
                signalType = SignalType.SELL;
                confidenceScore = calculateCrossoverConfidence(shortMA, longMA, false);
                double spread = ((longMA - shortMA) / longMA) * 100;
                if (deathCross) {
                    reasoning = String.format(
                            "Death Cross! %s(%d)=%.2f crossed BELOW %s(%d)=%.2f — exiting to flat.",
                            maType, shortPeriod, shortMA, maType, longPeriod, longMA);
                } else {
                    reasoning = String.format(
                            "Downtrend: %s(%d)=%.2f is %.2f%% below %s(%d)=%.2f",
                            maType, shortPeriod, shortMA, spread, maType, longPeriod, longMA);
                }
            }

            return createSignal(stock, signalType, confidenceScore, reasoning);

        } catch (Exception e) {
            log.error("Error calculating MA Crossover for {}: {}", stock.getSymbol(), e.getMessage());
            return createSignal(stock, SignalType.HOLD, 0.0, "Error in MA calculation");
        }
    }

    /**
     * Calculate confidence based on the strength of the crossover
     */
    private double calculateCrossoverConfidence(double shortMA, double longMA, boolean isBullish) {
        double percentDifference = Math.abs((shortMA - longMA) / longMA) * 100;

        // Larger difference = stronger signal (up to a point)
        double baseConfidence = 0.75;
        double bonusConfidence = Math.min(0.15, percentDifference * 0.05);

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
    public void setShortPeriod(int shortPeriod) {
        this.shortPeriod = shortPeriod;
        this.minDataPoints = longPeriod + 5;
    }

    public void setLongPeriod(int longPeriod) {
        this.longPeriod = longPeriod;
        this.minDataPoints = longPeriod + 5;
    }

    public void setUseEMA(boolean useEMA) {
        this.useEMA = useEMA;
        this.strategyName = useEMA ? "EMA Crossover Strategy" : "SMA Crossover Strategy";
    }
}
