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

    // ─────────────────────────────────────────────────────────────────
    // NEW INDICATORS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calculate VWAP (Volume-Weighted Average Price).
     * Uses all candles in the provided list (typically one trading session).
     * Formula: Σ(TypicalPrice × Volume) / ΣVolume
     * TypicalPrice = (High + Low + Close) / 3
     *
     * @return VWAP value; returns the simple SMA if total volume is 0
     */
    public double calculateVWAP(List<MarketData> marketData) {
        if (marketData == null || marketData.isEmpty()) {
            throw new IllegalArgumentException("No data for VWAP calculation");
        }
        double cumulativePV = 0;
        long cumulativeVolume = 0;
        for (MarketData d : marketData) {
            double typicalPrice = (d.getHighPrice().doubleValue()
                    + d.getLowPrice().doubleValue()
                    + d.getClosePrice().doubleValue()) / 3.0;
            long vol = d.getVolume();
            cumulativePV += typicalPrice * vol;
            cumulativeVolume += vol;
        }
        if (cumulativeVolume == 0) {
            return calculateSMA(marketData, marketData.size());
        }
        return cumulativePV / cumulativeVolume;
    }

    /**
     * Calculate ADX (Average Directional Index) with +DI and -DI.
     *
     * @param marketData market data (oldest first)
     * @param period     smoothing period, typically 14
     * @return double[] { ADX, +DI, -DI }
     */
    public double[] calculateADX(List<MarketData> marketData, int period) {
        int n = marketData.size();
        if (n < period * 2) {
            throw new IllegalArgumentException("Not enough data for ADX (need " + (period * 2) + ")");
        }

        List<Double> plusDM  = new ArrayList<>();
        List<Double> minusDM = new ArrayList<>();
        List<Double> tr      = new ArrayList<>();

        for (int i = 1; i < n; i++) {
            double high     = marketData.get(i).getHighPrice().doubleValue();
            double low      = marketData.get(i).getLowPrice().doubleValue();
            double prevHigh = marketData.get(i - 1).getHighPrice().doubleValue();
            double prevLow  = marketData.get(i - 1).getLowPrice().doubleValue();
            double prevClose= marketData.get(i - 1).getClosePrice().doubleValue();

            double upMove   = high - prevHigh;
            double downMove = prevLow - low;

            plusDM.add(upMove > downMove && upMove > 0 ? upMove : 0);
            minusDM.add(downMove > upMove && downMove > 0 ? downMove : 0);

            double t1 = high - low;
            double t2 = Math.abs(high - prevClose);
            double t3 = Math.abs(low  - prevClose);
            tr.add(Math.max(t1, Math.max(t2, t3)));
        }

        // Wilder smoothed sums over `period`
        double atr14  = sumList(tr,      0, period);
        double pdi14  = sumList(plusDM,  0, period);
        double mdi14  = sumList(minusDM, 0, period);

        List<Double> dx = new ArrayList<>();
        for (int i = period; i < tr.size(); i++) {
            atr14 = atr14 - (atr14 / period) + tr.get(i);
            pdi14 = pdi14 - (pdi14 / period) + plusDM.get(i);
            mdi14 = mdi14 - (mdi14 / period) + minusDM.get(i);

            double positiveDI = (atr14 == 0) ? 0 : (pdi14 / atr14) * 100;
            double negativeDI = (atr14 == 0) ? 0 : (mdi14 / atr14) * 100;
            double diSum = positiveDI + negativeDI;
            dx.add(diSum == 0 ? 0 : Math.abs(positiveDI - negativeDI) / diSum * 100);
        }

        // ADX = Wilder smoothed DX
        double adx = dx.isEmpty() ? 0 : dx.get(0);
        for (int i = 1; i < dx.size(); i++) {
            adx = (adx * (period - 1) + dx.get(i)) / period;
        }

        // Final +DI / -DI using the last ATR/DM smoothed values
        double positiveDI = (atr14 == 0) ? 0 : (pdi14 / atr14) * 100;
        double negativeDI = (atr14 == 0) ? 0 : (mdi14 / atr14) * 100;

        return new double[]{ adx, positiveDI, negativeDI };
    }

    /** Helper: sum elements [from, from+count) in a list. */
    private double sumList(List<Double> list, int from, int count) {
        double sum = 0;
        for (int i = from; i < from + count && i < list.size(); i++) sum += list.get(i);
        return sum;
    }

    /**
     * Calculate Supertrend indicator.
     * Uses ATR × multiplier above / below the (High+Low)/2 midpoint.
     *
     * @param marketData market data (oldest first)
     * @param period     ATR period (typically 10)
     * @param multiplier ATR multiplier (typically 3.0)
     * @return double[] { currentSupertrend, prevSupertrend, isBullish (1/0) }
     *         where isBullish=1 means price > Supertrend (uptrend).
     */
    public double[] calculateSupertrend(List<MarketData> marketData, int period, double multiplier) {
        int n = marketData.size();
        if (n < period + 2) {
            throw new IllegalArgumentException("Not enough data for Supertrend");
        }

        // Compute TR values
        List<Double> trList = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            double high      = marketData.get(i).getHighPrice().doubleValue();
            double low       = marketData.get(i).getLowPrice().doubleValue();
            double prevClose = marketData.get(i - 1).getClosePrice().doubleValue();
            trList.add(Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose))));
        }

        // Wilder ATR
        double atr = 0;
        for (int i = 0; i < period; i++) atr += trList.get(i);
        atr /= period;

        double prevUpperBand = 0, prevLowerBand = 0;
        double prevSupertrend = 0;
        boolean prevBullish = true;

        double curSupertrend = 0;
        boolean curBullish = true;

        for (int i = period; i < trList.size(); i++) {
            atr = (atr * (period - 1) + trList.get(i)) / period;

            double midPrice = (marketData.get(i + 1).getHighPrice().doubleValue()
                    + marketData.get(i + 1).getLowPrice().doubleValue()) / 2.0;
            double close    = marketData.get(i + 1).getClosePrice().doubleValue();

            double upperBand = midPrice + multiplier * atr;
            double lowerBand = midPrice - multiplier * atr;

            // Band adjustment (don't let bands widen unnecessarily)
            upperBand = (upperBand < prevUpperBand || marketData.get(i).getClosePrice().doubleValue() > prevUpperBand)
                    ? upperBand : prevUpperBand;
            lowerBand = (lowerBand > prevLowerBand || marketData.get(i).getClosePrice().doubleValue() < prevLowerBand)
                    ? lowerBand : prevLowerBand;

            curBullish = close > (prevBullish ? lowerBand : upperBand);
            curSupertrend = curBullish ? lowerBand : upperBand;

            prevSupertrend = (i == period) ? curSupertrend : prevSupertrend;
            if (i > period) prevSupertrend = prevSupertrend; // keep last iteration's value

            prevUpperBand = upperBand;
            prevLowerBand = lowerBand;
            prevBullish   = curBullish;

            if (i == trList.size() - 2) prevSupertrend = curSupertrend;
        }

        return new double[]{ curSupertrend, prevSupertrend, curBullish ? 1.0 : 0.0 };
    }

    /**
     * Calculate Donchian Channel.
     *
     * @param marketData market data (oldest first)
     * @param period     look-back period (e.g. 20)
     * @return double[] { upperChannel (highest high), lowerChannel (lowest low), midChannel }
     */
    public double[] calculateDonchianChannels(List<MarketData> marketData, int period) {
        if (marketData.size() < period) {
            throw new IllegalArgumentException("Not enough data for Donchian Channel");
        }
        List<MarketData> window = marketData.subList(marketData.size() - period, marketData.size());
        double highest = window.stream().mapToDouble(d -> d.getHighPrice().doubleValue()).max().orElse(0);
        double lowest  = window.stream().mapToDouble(d -> d.getLowPrice().doubleValue()).min().orElse(0);
        return new double[]{ highest, lowest, (highest + lowest) / 2.0 };
    }
}

