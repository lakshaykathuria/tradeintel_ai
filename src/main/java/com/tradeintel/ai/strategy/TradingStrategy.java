package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.model.MarketData;

import java.util.List;

/**
 * Base interface for all trading strategies
 */
public interface TradingStrategy {

    /**
     * Analyze market data and generate trade signal
     * 
     * @param stock      The stock to analyze
     * @param marketData Recent market data for the stock
     * @return Trade signal with recommendation (BUY, SELL, HOLD)
     */
    TradeSignal analyze(Stock stock, List<MarketData> marketData);

    /**
     * Get the name of this strategy
     */
    String getStrategyName();

    /**
     * Get the description of this strategy
     */
    String getDescription();

    /**
     * Validate if the strategy can be executed with given parameters
     */
    boolean validate();

    /**
     * Whether this strategy can be used in backtesting.
     * AI/news-based strategies that make external API calls should return false,
     * as they would be called hundreds of times and incur excessive cost.
     */
    default boolean isBacktestable() {
        return true;
    }
}
