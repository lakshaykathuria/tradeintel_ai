package com.tradeintel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for backtest results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult {

    private Long strategyId;
    private String strategyName;
    private String symbol;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Performance Metrics
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPercentage;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal maxDrawdown;
    private BigDecimal maxDrawdownPercentage;

    // Trade Statistics
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private BigDecimal averageWin;
    private BigDecimal averageLoss;
    private BigDecimal largestWin;
    private BigDecimal largestLoss;

    // Equity Curve
    private List<EquityPoint> equityCurve;

    // Trade Details
    private List<TradeDetail> trades;

    // Additional Metadata
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private Integer daysInMarket;
    private Map<String, Object> additionalMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityPoint {
        private LocalDateTime timestamp;
        private BigDecimal equity;
        private BigDecimal drawdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradeDetail {
        private LocalDateTime entryDate;
        private LocalDateTime exitDate;
        private String signalType; // BUY or SELL
        private BigDecimal entryPrice;
        private BigDecimal exitPrice;
        private Integer quantity;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercentage;
        private Integer holdingPeriodDays;
    }
}
