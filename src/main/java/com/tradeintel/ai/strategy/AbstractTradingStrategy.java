package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * Abstract base class for trading strategies with common functionality
 */
@Getter
@Setter
@Slf4j
public abstract class AbstractTradingStrategy implements TradingStrategy {

    protected String strategyName;
    protected String description;
    protected boolean enabled = true;

    /**
     * Minimum number of data points required for analysis
     */
    protected int minDataPoints = 20;

    @Override
    public boolean validate() {
        return enabled && strategyName != null && !strategyName.isEmpty();
    }

    /**
     * Check if we have enough data points for analysis
     */
    protected boolean hasEnoughData(List<MarketData> marketData) {
        return marketData != null && marketData.size() >= minDataPoints;
    }

    /**
     * Helper method to create a trade signal
     */
    protected TradeSignal createSignal(Stock stock, SignalType signalType,
            double confidenceScore, String reasoning) {
        TradeSignal signal = new TradeSignal();
        signal.setStock(stock);
        signal.setSignalType(signalType);
        signal.setConfidenceScore(BigDecimal.valueOf(confidenceScore));
        signal.setReasoning(reasoning);
        return signal;
    }
}
