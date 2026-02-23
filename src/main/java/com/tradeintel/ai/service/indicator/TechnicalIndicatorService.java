package com.tradeintel.ai.service.indicator;

import com.tradeintel.ai.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for calculating technical indicators
 */
@Service
@Slf4j
public class TechnicalIndicatorService {

    /**
     * Calculate RSI (Relative Strength Index)
     * 
     * @param marketData Historical price data
     * @param period     RSI period (typically 14)
     * @return RSI value (0-100)
     */
    public double calculateRSI(List<MarketData> marketData, int period) {
        if (marketData.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data points for RSI calculation");
        }

        double avgGain = 0;
        double avgLoss = 0;

        // Calculate initial average gain and loss
        for (int i = 1; i <= period; i++) {
            double change = marketData.get(i).getClosePrice().doubleValue() -
                    marketData.get(i - 1).getClosePrice().doubleValue();
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }

        avgGain /= period;
        avgLoss /= period;

        // Calculate RSI using smoothed averages
        for (int i = period + 1; i < marketData.size(); i++) {
            double change = marketData.get(i).getClosePrice().doubleValue() -
                    marketData.get(i - 1).getClosePrice().doubleValue();

            double currentGain = change > 0 ? change : 0;
            double currentLoss = change < 0 ? Math.abs(change) : 0;

            avgGain = ((avgGain * (period - 1)) + currentGain) / period;
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period;
        }

        if (avgLoss == 0) {
            return 100;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * Calculate SMA (Simple Moving Average)
     */
    public double calculateSMA(List<MarketData> marketData, int period) {
        if (marketData.size() < period) {
            throw new IllegalArgumentException("Not enough data points for SMA calculation");
        }

        double sum = 0;
        for (int i = marketData.size() - period; i < marketData.size(); i++) {
            sum += marketData.get(i).getClosePrice().doubleValue();
        }

        return sum / period;
    }

    /**
     * Calculate EMA (Exponential Moving Average)
     */
    public double calculateEMA(List<MarketData> marketData, int period) {
        if (marketData.size() < period) {
            throw new IllegalArgumentException("Not enough data points for EMA calculation");
        }

        double multiplier = 2.0 / (period + 1);

        // Start with SMA for the first EMA value
        double ema = calculateSMA(marketData.subList(0, period), period);

        // Calculate EMA for remaining data points
        for (int i = period; i < marketData.size(); i++) {
            double close = marketData.get(i).getClosePrice().doubleValue();
            ema = ((close - ema) * multiplier) + ema;
        }

        return ema;
    }

    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     * 
     * @return Array: [MACD line, Signal line, Histogram]
     */
    public double[] calculateMACD(List<MarketData> marketData) {
        return calculateMACD(marketData, 12, 26, 9);
    }

    public double[] calculateMACD(List<MarketData> marketData, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (marketData.size() < slowPeriod + signalPeriod) {
            throw new IllegalArgumentException("Not enough data points for MACD calculation");
        }

        // Calculate MACD line values for each of the last signalPeriod bars
        List<Double> macdValues = new ArrayList<>();
        for (int i = marketData.size() - signalPeriod; i <= marketData.size(); i++) {
            if (i < slowPeriod)
                continue;
            List<MarketData> slice = marketData.subList(0, i);
            double fast = calculateEMA(slice, fastPeriod);
            double slow = calculateEMA(slice, slowPeriod);
            macdValues.add(fast - slow);
        }

        double macdLine = macdValues.isEmpty() ? 0 : macdValues.get(macdValues.size() - 1);

        // Signal line = EMA of MACD values over signalPeriod
        double signalLine = macdLine;
        if (macdValues.size() >= signalPeriod) {
            double multiplier = 2.0 / (signalPeriod + 1);
            signalLine = macdValues.subList(0, signalPeriod).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(macdLine);
            for (int i = signalPeriod; i < macdValues.size(); i++) {
                signalLine = ((macdValues.get(i) - signalLine) * multiplier) + signalLine;
            }
        }

        double histogram = macdLine - signalLine;
        return new double[] { macdLine, signalLine, histogram };
    }

    /**
     * Calculate Bollinger Bands
     * 
     * @return Array: [Upper Band, Middle Band (SMA), Lower Band]
     */
    public double[] calculateBollingerBands(List<MarketData> marketData, int period, double stdDevMultiplier) {
        if (marketData.size() < period) {
            throw new IllegalArgumentException("Not enough data points for Bollinger Bands calculation");
        }

        // Calculate middle band (SMA)
        double middleBand = calculateSMA(marketData, period);

        // Calculate standard deviation
        double sumSquaredDiff = 0;
        for (int i = marketData.size() - period; i < marketData.size(); i++) {
            double diff = marketData.get(i).getClosePrice().doubleValue() - middleBand;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / period);

        // Calculate upper and lower bands
        double upperBand = middleBand + (stdDevMultiplier * stdDev);
        double lowerBand = middleBand - (stdDevMultiplier * stdDev);

        return new double[] { upperBand, middleBand, lowerBand };
    }

    /**
     * Calculate ATR (Average True Range)
     */
    public double calculateATR(List<MarketData> marketData, int period) {
        if (marketData.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data points for ATR calculation");
        }

        List<Double> trueRanges = new ArrayList<>();

        for (int i = 1; i < marketData.size(); i++) {
            double high = marketData.get(i).getHighPrice().doubleValue();
            double low = marketData.get(i).getLowPrice().doubleValue();
            double prevClose = marketData.get(i - 1).getClosePrice().doubleValue();

            double tr1 = high - low;
            double tr2 = Math.abs(high - prevClose);
            double tr3 = Math.abs(low - prevClose);

            double trueRange = Math.max(tr1, Math.max(tr2, tr3));
            trueRanges.add(trueRange);
        }

        // Calculate average of true ranges
        double sum = 0;
        for (int i = trueRanges.size() - period; i < trueRanges.size(); i++) {
            sum += trueRanges.get(i);
        }

        return sum / period;
    }

    /**
     * Calculate Stochastic Oscillator
     * 
     * @return Array: [%K, %D]
     */
    public double[] calculateStochastic(List<MarketData> marketData, int period) {
        if (marketData.size() < period) {
            throw new IllegalArgumentException("Not enough data points for Stochastic calculation");
        }

        // Compute %K for last 3 bars (needed for %D = 3-period SMA of %K)
        int dPeriod = 3;
        List<Double> kValues = new ArrayList<>();
        int startIdx = Math.max(0, marketData.size() - (period + dPeriod - 1));

        for (int end = startIdx + period; end <= marketData.size(); end++) {
            List<MarketData> window = marketData.subList(end - period, end);
            double highestHigh = window.stream()
                    .mapToDouble(d -> d.getHighPrice().doubleValue()).max().orElse(0);
            double lowestLow = window.stream()
                    .mapToDouble(d -> d.getLowPrice().doubleValue()).min().orElse(0);
            double close = window.get(window.size() - 1).getClosePrice().doubleValue();

            double range = highestHigh - lowestLow;
            double k = range == 0 ? 50.0 : ((close - lowestLow) / range) * 100;
            kValues.add(k);
        }

        double percentK = kValues.isEmpty() ? 50.0 : kValues.get(kValues.size() - 1);

        // %D = simple average of last min(3, available) %K values
        double percentD = kValues.stream()
                .skip(Math.max(0, kValues.size() - dPeriod))
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(percentK);

        return new double[] { percentK, percentD };
    }

    /**
     * Detect if price is in an uptrend
     */
    public boolean isUptrend(List<MarketData> marketData, int shortPeriod, int longPeriod) {
        if (marketData.size() < longPeriod) {
            return false;
        }

        double shortMA = calculateSMA(marketData, shortPeriod);
        double longMA = calculateSMA(marketData, longPeriod);

        return shortMA > longMA;
    }

    /**
     * Detect if price is in a downtrend
     */
    public boolean isDowntrend(List<MarketData> marketData, int shortPeriod, int longPeriod) {
        if (marketData.size() < longPeriod) {
            return false;
        }

        double shortMA = calculateSMA(marketData, shortPeriod);
        double longMA = calculateSMA(marketData, longPeriod);

        return shortMA < longMA;
    }

    /**
     * Calculate volume trend (comparing current volume to average)
     */
    public double calculateVolumeRatio(List<MarketData> marketData, int period) {
        if (marketData.size() < period + 1) {
            throw new IllegalArgumentException("Not enough data points for volume ratio calculation");
        }

        // Calculate average volume
        long sumVolume = 0;
        for (int i = marketData.size() - period - 1; i < marketData.size() - 1; i++) {
            sumVolume += marketData.get(i).getVolume();
        }
        double avgVolume = (double) sumVolume / period;

        // Current volume
        long currentVolume = marketData.get(marketData.size() - 1).getVolume();

        return currentVolume / avgVolume;
    }
}
