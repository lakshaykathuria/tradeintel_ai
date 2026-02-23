package com.tradeintel.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

import com.tradeintel.ai.model.TradingMode;

import java.math.BigDecimal;

@Configuration
@Getter
public class TradingConfig {

    @Value("${trading.mode}")
    private TradingMode tradingMode;

    @Value("${trading.paper-trading.initial-capital}")
    private BigDecimal paperTradingInitialCapital;

    @Value("${trading.paper-trading.commission-per-trade}")
    private BigDecimal commissionPerTrade;

    @Value("${trading.risk-management.max-position-size-percent}")
    private BigDecimal maxPositionSizePercent;

    @Value("${trading.risk-management.max-portfolio-risk-percent}")
    private BigDecimal maxPortfolioRiskPercent;

    @Value("${trading.risk-management.max-daily-loss-percent}")
    private BigDecimal maxDailyLossPercent;

    @Value("${trading.risk-management.max-drawdown-percent}")
    private BigDecimal maxDrawdownPercent;

    @Value("${trading.risk-management.stop-loss-percent}")
    private BigDecimal stopLossPercent;

    @Value("${trading.risk-management.take-profit-percent}")
    private BigDecimal takeProfitPercent;
}
