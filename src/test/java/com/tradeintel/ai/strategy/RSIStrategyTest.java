package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.service.indicator.TechnicalIndicatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RSI Strategy
 */
@DisplayName("RSI Strategy Tests")
class RSIStrategyTest {

    @Mock
    private TechnicalIndicatorService indicatorService;

    private RSIStrategy strategy;
    private Stock testStock;
    private List<MarketData> testData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategy = new RSIStrategy(indicatorService);

        testStock = new Stock();
        testStock.setId(1L);
        testStock.setSymbol("TEST");
        testStock.setName("Test Stock");

        testData = createTestData(30);
    }

    private List<MarketData> createTestData(int count) {
        List<MarketData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MarketData md = new MarketData();
            md.setTimestamp(LocalDateTime.now().minusDays(count - i));
            md.setClosePrice(BigDecimal.valueOf(100 + i));
            md.setOpenPrice(BigDecimal.valueOf(99 + i));
            md.setHighPrice(BigDecimal.valueOf(101 + i));
            md.setLowPrice(BigDecimal.valueOf(98 + i));
            md.setVolume(1000000L);
            data.add(md);
        }
        return data;
    }

    @Test
    @DisplayName("Should generate BUY signal when RSI is oversold")
    void testOversoldBuySignal() {
        // Mock RSI to return oversold value (< 30)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(25.0);

        TradeSignal signal = strategy.analyze(testStock, testData);

        assertNotNull(signal);
        assertEquals(SignalType.BUY, signal.getSignalType());
        assertTrue(signal.getConfidenceScore().compareTo(BigDecimal.valueOf(0.6)) > 0,
                "Confidence should be > 0.6 for oversold");
        assertTrue(signal.getReasoning().contains("oversold"));

        verify(indicatorService, times(1)).calculateRSI(anyList(), eq(14));
    }

    @Test
    @DisplayName("Should generate SELL signal when RSI is overbought")
    void testOverboughtSellSignal() {
        // Mock RSI to return overbought value (> 70)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(75.0);

        TradeSignal signal = strategy.analyze(testStock, testData);

        assertNotNull(signal);
        assertEquals(SignalType.SELL, signal.getSignalType());
        assertTrue(signal.getConfidenceScore().compareTo(BigDecimal.valueOf(0.6)) > 0,
                "Confidence should be > 0.6 for overbought");
        assertTrue(signal.getReasoning().contains("overbought"));
    }

    @Test
    @DisplayName("Should generate HOLD signal when RSI is neutral")
    void testNeutralHoldSignal() {
        // Mock RSI to return neutral value (30-70)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(50.0);

        TradeSignal signal = strategy.analyze(testStock, testData);

        assertNotNull(signal);
        assertEquals(SignalType.HOLD, signal.getSignalType());
        assertEquals(0, signal.getConfidenceScore().compareTo(BigDecimal.valueOf(0.5)),
                "Confidence should be 0.5 for neutral");
        assertTrue(signal.getReasoning().contains("neutral"));
    }

    @Test
    @DisplayName("Should handle insufficient data gracefully")
    void testInsufficientData() {
        List<MarketData> shortData = createTestData(10);

        TradeSignal signal = strategy.analyze(testStock, shortData);

        assertNotNull(signal);
        assertEquals(SignalType.HOLD, signal.getSignalType());
        assertEquals(0, signal.getConfidenceScore().compareTo(BigDecimal.ZERO));
        assertTrue(signal.getReasoning().contains("Insufficient data"));
    }

    @Test
    @DisplayName("Should handle calculation errors gracefully")
    void testCalculationError() {
        when(indicatorService.calculateRSI(anyList(), anyInt()))
                .thenThrow(new RuntimeException("Calculation error"));

        TradeSignal signal = strategy.analyze(testStock, testData);

        assertNotNull(signal);
        assertEquals(SignalType.HOLD, signal.getSignalType());
        assertEquals(0, signal.getConfidenceScore().compareTo(BigDecimal.ZERO));
        assertTrue(signal.getReasoning().contains("Error"));
    }

    @Test
    @DisplayName("Confidence should increase with more extreme RSI values")
    void testConfidenceScaling() {
        // Test very oversold (RSI = 20)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(20.0);
        TradeSignal signal1 = strategy.analyze(testStock, testData);

        // Test moderately oversold (RSI = 28)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(28.0);
        TradeSignal signal2 = strategy.analyze(testStock, testData);

        assertTrue(signal1.getConfidenceScore().compareTo(signal2.getConfidenceScore()) > 0,
                "More extreme RSI should have higher confidence");
    }

    @Test
    @DisplayName("Should validate strategy configuration")
    void testStrategyValidation() {
        assertTrue(strategy.validate(), "Strategy should be valid with default configuration");
    }

    @Test
    @DisplayName("Should have correct strategy metadata")
    void testStrategyMetadata() {
        assertEquals("RSI Strategy", strategy.getStrategyName());
        assertNotNull(strategy.getDescription());
        assertNotNull(strategy.getDescription());
    }

    @Test
    @DisplayName("Should allow RSI period configuration")
    void testRSIPeriodConfiguration() {
        strategy.setRsiPeriod(20);

        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(50.0);
        strategy.analyze(testStock, testData);

        verify(indicatorService).calculateRSI(anyList(), eq(20));
    }

    @Test
    @DisplayName("Should allow threshold configuration")
    void testThresholdConfiguration() {
        strategy.setOversoldThreshold(25.0);
        strategy.setOverboughtThreshold(75.0);

        // RSI = 26 should now be neutral (not oversold)
        when(indicatorService.calculateRSI(anyList(), anyInt())).thenReturn(26.0);
        TradeSignal signal = strategy.analyze(testStock, testData);

        assertEquals(SignalType.HOLD, signal.getSignalType());
    }
}
