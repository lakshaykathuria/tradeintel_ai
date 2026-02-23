-- V2__strategy_enhancements.sql
-- Database migration for Phase 3: Trading Strategy Engine

-- Create backtest_results table
CREATE TABLE IF NOT EXISTS backtest_results (
    id BIGSERIAL PRIMARY KEY,
    strategy_id BIGINT REFERENCES trading_strategies(id) ON DELETE CASCADE,
    stock_id BIGINT REFERENCES stocks(id) ON DELETE CASCADE,
    
    -- Time period
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    
    -- Performance metrics
    total_return DECIMAL(15, 2) NOT NULL,
    total_return_percentage DECIMAL(10, 4) NOT NULL,
    sharpe_ratio DECIMAL(10, 4),
    sortino_ratio DECIMAL(10, 4),
    max_drawdown DECIMAL(15, 2),
    max_drawdown_percentage DECIMAL(10, 4),
    
    -- Trade statistics
    total_trades INTEGER NOT NULL DEFAULT 0,
    winning_trades INTEGER NOT NULL DEFAULT 0,
    losing_trades INTEGER NOT NULL DEFAULT 0,
    win_rate DECIMAL(5, 2),
    profit_factor DECIMAL(10, 4),
    average_win DECIMAL(15, 2),
    average_loss DECIMAL(15, 2),
    largest_win DECIMAL(15, 2),
    largest_loss DECIMAL(15, 2),
    
    -- Capital
    initial_capital DECIMAL(15, 2) NOT NULL,
    final_capital DECIMAL(15, 2) NOT NULL,
    
    -- Additional data (stored as JSONB)
    equity_curve JSONB,
    trade_details JSONB,
    additional_metrics JSONB,
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_date_range CHECK (end_date > start_date),
    CONSTRAINT valid_capital CHECK (initial_capital > 0)
);

-- Create indexes for backtest_results
CREATE INDEX idx_backtest_strategy ON backtest_results(strategy_id);
CREATE INDEX idx_backtest_stock ON backtest_results(stock_id);
CREATE INDEX idx_backtest_dates ON backtest_results(start_date, end_date);
CREATE INDEX idx_backtest_performance ON backtest_results(total_return_percentage DESC);
CREATE INDEX idx_backtest_created ON backtest_results(created_at DESC);

-- Create indicator_cache table for caching technical indicator calculations
CREATE TABLE IF NOT EXISTS indicator_cache (
    id BIGSERIAL PRIMARY KEY,
    stock_id BIGINT REFERENCES stocks(id) ON DELETE CASCADE,
    
    -- Indicator details
    indicator_type VARCHAR(50) NOT NULL, -- RSI, MACD, SMA, EMA, etc.
    period INTEGER NOT NULL,
    
    -- Calculated value
    indicator_value DECIMAL(15, 6) NOT NULL,
    
    -- Additional parameters (for complex indicators)
    parameters JSONB,
    
    -- Timestamp for the calculation
    calculation_timestamp TIMESTAMP NOT NULL,
    
    -- Cache metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    CONSTRAINT unique_indicator_cache UNIQUE (stock_id, indicator_type, period, calculation_timestamp)
);

-- Create indexes for indicator_cache
CREATE INDEX idx_indicator_stock ON indicator_cache(stock_id);
CREATE INDEX idx_indicator_type ON indicator_cache(indicator_type);
CREATE INDEX idx_indicator_timestamp ON indicator_cache(calculation_timestamp DESC);
CREATE INDEX idx_indicator_expires ON indicator_cache(expires_at);

-- Add comments for documentation
COMMENT ON TABLE backtest_results IS 'Stores results from strategy backtesting';
COMMENT ON TABLE indicator_cache IS 'Caches calculated technical indicators to improve performance';

COMMENT ON COLUMN backtest_results.sharpe_ratio IS 'Risk-adjusted return metric (annualized)';
COMMENT ON COLUMN backtest_results.sortino_ratio IS 'Downside risk-adjusted return metric (annualized)';
COMMENT ON COLUMN backtest_results.max_drawdown IS 'Maximum peak-to-trough decline in absolute terms';
COMMENT ON COLUMN backtest_results.profit_factor IS 'Ratio of gross profit to gross loss';
COMMENT ON COLUMN backtest_results.equity_curve IS 'Time series of portfolio equity values (JSONB array)';
COMMENT ON COLUMN backtest_results.trade_details IS 'Detailed information about each trade (JSONB array)';

COMMENT ON COLUMN indicator_cache.indicator_type IS 'Type of technical indicator (RSI, MACD, SMA, EMA, BB, ATR, etc.)';
COMMENT ON COLUMN indicator_cache.period IS 'Period used for indicator calculation (e.g., 14 for RSI-14)';
COMMENT ON COLUMN indicator_cache.parameters IS 'Additional parameters for complex indicators (JSONB)';
COMMENT ON COLUMN indicator_cache.expires_at IS 'Expiration time for cached value (NULL = no expiration)';
