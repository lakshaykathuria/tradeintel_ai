package com.tradeintel.ai.service;

import com.tradeintel.ai.dto.BacktestResult;
import com.tradeintel.ai.dto.BacktestResult.EquityPoint;
import com.tradeintel.ai.dto.BacktestResult.TradeDetail;
import com.tradeintel.ai.model.*;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for backtesting trading strategies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

    private final MarketDataRepository marketDataRepository;
    private final Map<String, TradingStrategy> strategies;

    /**
     * Run backtest for a strategy
     */
    public BacktestResult runBacktest(String strategyName, Stock stock,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal initialCapital) {
        log.info("Running backtest for strategy {} on {} from {} to {}",
                strategyName, stock.getSymbol(), startDate, endDate);

        // Get strategy
        TradingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyName);
        }

        // Guard: reject strategies that make live AI/HTTP calls on every data point
        if (!strategy.isBacktestable()) {
            throw new IllegalArgumentException(
                    "'" + strategyName + "' cannot be used in backtesting — " +
                            "it makes live AI/HTTP calls per data point which would cause excessive cost. " +
                            "Please choose a technical strategy (RSI, MACD, Bollinger Bands, etc.).");
        }

        // Get historical data
        List<MarketData> historicalData = marketDataRepository
                .findByStockAndTimestampBetweenOrderByTimestampDesc(stock, startDate, endDate);

        if (historicalData.isEmpty()) {
            throw new IllegalArgumentException("No historical data found for the specified period");
        }

        // Reverse to get chronological order
        List<MarketData> chronologicalData = new ArrayList<>(historicalData);
        java.util.Collections.reverse(chronologicalData);

        // Run simulation
        BacktestSimulation simulation = new BacktestSimulation(initialCapital);
        List<TradeDetail> trades = new ArrayList<>();
        List<EquityPoint> equityCurve = new ArrayList<>();

        TradePosition currentPosition = null;

        for (int i = 0; i < chronologicalData.size(); i++) {
            MarketData currentData = chronologicalData.get(i);

            // Get historical data up to this point for strategy analysis
            List<MarketData> dataUpToNow = chronologicalData.subList(0, Math.min(i + 1, chronologicalData.size()));

            // Skip if not enough data for strategy
            if (dataUpToNow.size() < 20) {
                continue;
            }

            // Generate signal
            TradeSignal signal = strategy.analyze(stock, dataUpToNow);
            SignalType signalType = signal.getSignalType();

            // Execute trades based on signal
            if (signalType == SignalType.BUY && currentPosition == null) {
                // Open long position
                currentPosition = simulation.openPosition(
                        currentData.getClosePrice(),
                        currentData.getTimestamp(),
                        SignalType.BUY);
            } else if (signalType == SignalType.SELL && currentPosition != null) {
                // Close position
                TradeDetail trade = simulation.closePosition(
                        currentPosition,
                        currentData.getClosePrice(),
                        currentData.getTimestamp());
                trades.add(trade);
                currentPosition = null;
            }

            // Record equity point
            BigDecimal currentEquity = simulation.getCurrentEquity(
                    currentPosition,
                    currentData.getClosePrice());
            BigDecimal drawdown = simulation.calculateDrawdown(currentEquity);

            equityCurve.add(EquityPoint.builder()
                    .timestamp(currentData.getTimestamp())
                    .equity(currentEquity)
                    .drawdown(drawdown)
                    .build());
        }

        // Close any open position at the end
        if (currentPosition != null) {
            MarketData lastData = chronologicalData.get(chronologicalData.size() - 1);
            TradeDetail trade = simulation.closePosition(
                    currentPosition,
                    lastData.getClosePrice(),
                    lastData.getTimestamp());
            trades.add(trade);
        }

        // Calculate performance metrics
        return calculatePerformanceMetrics(
                strategyName,
                stock.getSymbol(),
                startDate,
                endDate,
                initialCapital,
                simulation.getCash(),
                trades,
                equityCurve);
    }

    /**
     * Calculate comprehensive performance metrics
     */
    private BacktestResult calculatePerformanceMetrics(String strategyName, String symbol,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal initialCapital, BigDecimal finalCapital,
            List<TradeDetail> trades, List<EquityPoint> equityCurve) {

        // Total return
        BigDecimal totalReturn = finalCapital.subtract(initialCapital);
        BigDecimal totalReturnPct = totalReturn.divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Trade statistics
        int totalTrades = trades.size();
        int winningTrades = (int) trades.stream()
                .filter(t -> t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .count();
        int losingTrades = totalTrades - winningTrades;

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Average win/loss
        BigDecimal avgWin = trades.stream()
                .filter(t -> t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .map(TradeDetail::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(winningTrades, 1)), 2, RoundingMode.HALF_UP);

        BigDecimal avgLoss = trades.stream()
                .filter(t -> t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
                .map(TradeDetail::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(losingTrades, 1)), 2, RoundingMode.HALF_UP);

        // Largest win/loss
        BigDecimal largestWin = trades.stream()
                .map(TradeDetail::getProfitLoss)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal largestLoss = trades.stream()
                .map(TradeDetail::getProfitLoss)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Profit factor
        BigDecimal totalWins = trades.stream()
                .filter(t -> t.getProfitLoss().compareTo(BigDecimal.ZERO) > 0)
                .map(TradeDetail::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLosses = trades.stream()
                .filter(t -> t.getProfitLoss().compareTo(BigDecimal.ZERO) < 0)
                .map(TradeDetail::getProfitLoss)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitFactor = totalLosses.compareTo(BigDecimal.ZERO) > 0
                ? totalWins.divide(totalLosses, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Sharpe and Sortino ratios
        BigDecimal sharpeRatio = calculateSharpeRatio(trades);
        BigDecimal sortinoRatio = calculateSortinoRatio(trades);

        // Max drawdown
        BigDecimal maxDrawdown = equityCurve.stream()
                .map(EquityPoint::getDrawdown)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxDrawdownPct = maxDrawdown.divide(initialCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Days in market
        int daysInMarket = (int) ChronoUnit.DAYS.between(startDate, endDate);

        return BacktestResult.builder()
                .strategyName(strategyName)
                .symbol(symbol)
                .startDate(startDate)
                .endDate(endDate)
                .totalReturn(totalReturn)
                .totalReturnPercentage(totalReturnPct)
                .sharpeRatio(sharpeRatio)
                .sortinoRatio(sortinoRatio)
                .maxDrawdown(maxDrawdown)
                .maxDrawdownPercentage(maxDrawdownPct)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .winRate(winRate)
                .profitFactor(profitFactor)
                .averageWin(avgWin)
                .averageLoss(avgLoss)
                .largestWin(largestWin)
                .largestLoss(largestLoss)
                .equityCurve(equityCurve)
                .trades(trades)
                .initialCapital(initialCapital)
                .finalCapital(finalCapital)
                .daysInMarket(daysInMarket)
                .build();
    }

    /**
     * Calculate Sharpe Ratio
     */
    private BigDecimal calculateSharpeRatio(List<TradeDetail> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        trades.forEach(t -> stats.addValue(t.getProfitLossPercentage().doubleValue()));

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();

        if (stdDev == 0) {
            return BigDecimal.ZERO;
        }

        // Annualized Sharpe Ratio (assuming 252 trading days)
        double sharpe = (mean / stdDev) * Math.sqrt(252);
        return BigDecimal.valueOf(sharpe).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Sortino Ratio (only considers downside deviation)
     */
    private BigDecimal calculateSortinoRatio(List<TradeDetail> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double[] returns = trades.stream()
                .mapToDouble(t -> t.getProfitLossPercentage().doubleValue())
                .toArray();

        double mean = 0;
        for (double ret : returns) {
            mean += ret;
        }
        mean /= returns.length;

        // Calculate downside deviation
        double downsideSum = 0;
        int downsideCount = 0;
        for (double ret : returns) {
            if (ret < 0) {
                downsideSum += ret * ret;
                downsideCount++;
            }
        }

        if (downsideCount == 0) {
            return BigDecimal.ZERO;
        }

        double downsideDeviation = Math.sqrt(downsideSum / downsideCount);

        if (downsideDeviation == 0) {
            return BigDecimal.ZERO;
        }

        // Annualized Sortino Ratio
        double sortino = (mean / downsideDeviation) * Math.sqrt(252);
        return BigDecimal.valueOf(sortino).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Inner class to manage backtest simulation state
     */
    private static class BacktestSimulation {
        private BigDecimal cash;
        private BigDecimal peakEquity;

        public BacktestSimulation(BigDecimal initialCapital) {
            this.cash = initialCapital;
            this.peakEquity = initialCapital;
        }

        public TradePosition openPosition(BigDecimal price, LocalDateTime timestamp, SignalType signalType) {
            // Use 95% of cash for position (keep 5% as buffer)
            BigDecimal positionSize = cash.multiply(BigDecimal.valueOf(0.95));
            int quantity = positionSize.divide(price, 0, RoundingMode.DOWN).intValue();
            BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));

            cash = cash.subtract(cost);

            return new TradePosition(timestamp, price, quantity, signalType);
        }

        public TradeDetail closePosition(TradePosition position, BigDecimal exitPrice, LocalDateTime exitTimestamp) {
            BigDecimal proceeds = exitPrice.multiply(BigDecimal.valueOf(position.quantity));
            cash = cash.add(proceeds);

            BigDecimal profitLoss = proceeds.subtract(
                    position.entryPrice.multiply(BigDecimal.valueOf(position.quantity)));
            BigDecimal profitLossPct = profitLoss
                    .divide(position.entryPrice.multiply(BigDecimal.valueOf(position.quantity)), 4,
                            RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            int holdingDays = (int) ChronoUnit.DAYS.between(position.entryTimestamp, exitTimestamp);

            return TradeDetail.builder()
                    .entryDate(position.entryTimestamp)
                    .exitDate(exitTimestamp)
                    .signalType(position.signalType.name())
                    .entryPrice(position.entryPrice)
                    .exitPrice(exitPrice)
                    .quantity(position.quantity)
                    .profitLoss(profitLoss)
                    .profitLossPercentage(profitLossPct)
                    .holdingPeriodDays(holdingDays)
                    .build();
        }

        public BigDecimal getCurrentEquity(TradePosition position, BigDecimal currentPrice) {
            if (position == null) {
                return cash;
            }
            BigDecimal positionValue = currentPrice.multiply(BigDecimal.valueOf(position.quantity));
            return cash.add(positionValue);
        }

        public BigDecimal calculateDrawdown(BigDecimal currentEquity) {
            if (currentEquity.compareTo(peakEquity) > 0) {
                peakEquity = currentEquity;
                return BigDecimal.ZERO;
            }
            return peakEquity.subtract(currentEquity);
        }

        public BigDecimal getCash() {
            return cash;
        }
    }

    /**
     * Inner class to track open positions
     */
    private static class TradePosition {
        LocalDateTime entryTimestamp;
        BigDecimal entryPrice;
        int quantity;
        SignalType signalType;

        public TradePosition(LocalDateTime entryTimestamp, BigDecimal entryPrice, int quantity, SignalType signalType) {
            this.entryTimestamp = entryTimestamp;
            this.entryPrice = entryPrice;
            this.quantity = quantity;
            this.signalType = signalType;
        }
    }
}
