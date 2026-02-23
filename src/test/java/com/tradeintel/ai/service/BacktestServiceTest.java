package com.tradeintel.ai.service;

import com.tradeintel.ai.dto.BacktestResult;
import com.tradeintel.ai.model.*;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.strategy.TradingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BacktestService
 */
@DisplayName("Backtest Service Tests")
class BacktestServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private TradingStrategy mockStrategy;

    private BacktestService backtestService;
    private Stock testStock;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Map<String, TradingStrategy> strategies = new HashMap<>();
        strategies.put("testStrategy", mockStrategy);

        backtestService = new BacktestService(marketDataRepository, strategies);

        testStock = new Stock();
        testStock.setId(1L);
        testStock.setSymbol("TEST");
        testStock.setName("Test Stock");

        startDate = LocalDateTime.now().minusMonths(3);
        endDate = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should run successful backtest with profitable trades")
    void testSuccessfulBacktest() {
        List<MarketData> historicalData = createProfitableScenario();

        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(historicalData);

        when(mockStrategy.analyze(any(), anyList()))
                .thenAnswer(invocation -> createBuySignal())
                .thenAnswer(invocation -> createSellSignal())
                .thenAnswer(invocation -> createHoldSignal());

        BacktestResult result = backtestService.runBacktest(
                "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));

        assertNotNull(result);
        assertEquals("testStrategy", result.getStrategyName());
        assertEquals("TEST", result.getSymbol());
        assertTrue(result.getTotalReturn().compareTo(BigDecimal.ZERO) >= 0);
        assertNotNull(result.getEquityCurve());
        assertNotNull(result.getTrades());
    }

    @Test
    @DisplayName("Should calculate correct performance metrics")
    void testPerformanceMetrics() {
        List<MarketData> historicalData = createProfitableScenario();

        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(historicalData);

        when(mockStrategy.analyze(any(), anyList()))
                .thenReturn(createBuySignal())
                .thenReturn(createSellSignal())
                .thenReturn(createHoldSignal());

        BacktestResult result = backtestService.runBacktest(
                "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));

        assertNotNull(result.getSharpeRatio());
        assertNotNull(result.getSortinoRatio());
        assertNotNull(result.getMaxDrawdown());
        assertNotNull(result.getWinRate());
        assertNotNull(result.getProfitFactor());

        assertTrue(result.getTotalTrades() >= 0);
        assertEquals(result.getWinningTrades() + result.getLosingTrades(),
                result.getTotalTrades());
    }

    @Test
    @DisplayName("Should handle no historical data")
    void testNoHistoricalData() {
        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> {
            backtestService.runBacktest(
                    "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));
        });
    }

    @Test
    @DisplayName("Should handle unknown strategy")
    void testUnknownStrategy() {
        assertThrows(IllegalArgumentException.class, () -> {
            backtestService.runBacktest(
                    "unknownStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));
        });
    }

    @Test
    @DisplayName("Should track equity curve correctly")
    void testEquityCurve() {
        List<MarketData> historicalData = createProfitableScenario();

        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(historicalData);

        when(mockStrategy.analyze(any(), anyList()))
                .thenReturn(createHoldSignal());

        BacktestResult result = backtestService.runBacktest(
                "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));

        assertNotNull(result.getEquityCurve());
        assertFalse(result.getEquityCurve().isEmpty());

        // First equity point should be close to initial capital
        BacktestResult.EquityPoint firstPoint = result.getEquityCurve().get(0);
        assertTrue(firstPoint.getEquity().compareTo(BigDecimal.valueOf(99000)) > 0);
    }

    @Test
    @DisplayName("Should calculate win rate correctly")
    void testWinRateCalculation() {
        List<MarketData> historicalData = createMixedScenario();

        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(historicalData);

        // Create alternating buy/sell signals for multiple trades
        when(mockStrategy.analyze(any(), anyList()))
                .thenReturn(createBuySignal())
                .thenReturn(createSellSignal())
                .thenReturn(createBuySignal())
                .thenReturn(createSellSignal())
                .thenReturn(createHoldSignal());

        BacktestResult result = backtestService.runBacktest(
                "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));

        if (result.getTotalTrades() > 0) {
            assertTrue(result.getWinRate().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(result.getWinRate().compareTo(BigDecimal.valueOf(100)) <= 0);
        }
    }

    @Test
    @DisplayName("Should handle only losing trades")
    void testOnlyLosingTrades() {
        List<MarketData> historicalData = createLosingScenario();

        when(marketDataRepository.findByStockAndTimestampBetweenOrderByTimestampDesc(
                any(), any(), any())).thenReturn(historicalData);

        when(mockStrategy.analyze(any(), anyList()))
                .thenReturn(createBuySignal())
                .thenReturn(createSellSignal())
                .thenReturn(createHoldSignal());

        BacktestResult result = backtestService.runBacktest(
                "testStrategy", testStock, startDate, endDate, BigDecimal.valueOf(100000));

        assertTrue(result.getTotalReturn().compareTo(BigDecimal.ZERO) <= 0,
                "Total return should be negative for losing trades");
        assertEquals(BigDecimal.ZERO, result.getWinRate());
    }

    // Helper methods to create test data

    private List<MarketData> createProfitableScenario() {
        List<MarketData> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            MarketData md = new MarketData();
            md.setTimestamp(startDate.plusDays(i));
            md.setClosePrice(BigDecimal.valueOf(100 + i)); // Rising prices
            md.setOpenPrice(BigDecimal.valueOf(99 + i));
            md.setHighPrice(BigDecimal.valueOf(101 + i));
            md.setLowPrice(BigDecimal.valueOf(98 + i));
            md.setVolume(1000000L);
            data.add(md);
        }
        return data;
    }

    private List<MarketData> createLosingScenario() {
        List<MarketData> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            MarketData md = new MarketData();
            md.setTimestamp(startDate.plusDays(i));
            md.setClosePrice(BigDecimal.valueOf(150 - i)); // Falling prices
            md.setOpenPrice(BigDecimal.valueOf(151 - i));
            md.setHighPrice(BigDecimal.valueOf(152 - i));
            md.setLowPrice(BigDecimal.valueOf(149 - i));
            md.setVolume(1000000L);
            data.add(md);
        }
        return data;
    }

    private List<MarketData> createMixedScenario() {
        List<MarketData> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            MarketData md = new MarketData();
            md.setTimestamp(startDate.plusDays(i));
            // Oscillating prices
            double price = 100 + (i % 2 == 0 ? i : -i);
            md.setClosePrice(BigDecimal.valueOf(price));
            md.setOpenPrice(BigDecimal.valueOf(price - 1));
            md.setHighPrice(BigDecimal.valueOf(price + 1));
            md.setLowPrice(BigDecimal.valueOf(price - 2));
            md.setVolume(1000000L);
            data.add(md);
        }
        return data;
    }

    private TradeSignal createBuySignal() {
        TradeSignal signal = new TradeSignal();
        signal.setSignalType(SignalType.BUY);
        signal.setConfidenceScore(BigDecimal.valueOf(0.8));
        signal.setReasoning("Test buy signal");
        return signal;
    }

    private TradeSignal createSellSignal() {
        TradeSignal signal = new TradeSignal();
        signal.setSignalType(SignalType.SELL);
        signal.setConfidenceScore(BigDecimal.valueOf(0.8));
        signal.setReasoning("Test sell signal");
        return signal;
    }

    private TradeSignal createHoldSignal() {
        TradeSignal signal = new TradeSignal();
        signal.setSignalType(SignalType.HOLD);
        signal.setConfidenceScore(BigDecimal.valueOf(0.5));
        signal.setReasoning("Test hold signal");
        return signal;
    }
}
