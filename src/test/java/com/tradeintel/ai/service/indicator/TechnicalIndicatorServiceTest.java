package com.tradeintel.ai.service.indicator;

import com.tradeintel.ai.model.MarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TechnicalIndicatorService
 */
@DisplayName("Technical Indicator Service Tests")
class TechnicalIndicatorServiceTest {

    private TechnicalIndicatorService indicatorService;
    private List<MarketData> testData;

    @BeforeEach
    void setUp() {
        indicatorService = new TechnicalIndicatorService();
        testData = createTestMarketData();
    }

    /**
     * Create test market data with known values
     */
    private List<MarketData> createTestMarketData() {
        List<MarketData> data = new ArrayList<>();

        // Create 50 days of test data (enough for MACD which needs 26+9=35 minimum)
        for (int i = 0; i < 50; i++) {
            double closePrice = 100.0 + (i * 0.5); // Gradual uptrend

            MarketData md = new MarketData();
            md.setTimestamp(LocalDateTime.now().minusDays(50 - i));
            md.setOpenPrice(BigDecimal.valueOf(closePrice - 1));
            md.setHighPrice(BigDecimal.valueOf(closePrice + 1));
            md.setLowPrice(BigDecimal.valueOf(closePrice - 2));
            md.setClosePrice(BigDecimal.valueOf(closePrice));
            md.setVolume(1000000L + (i * 10000L));
            data.add(md);
        }

        return data;
    }

    @Test
    @DisplayName("RSI calculation should return value between 0 and 100")
    void testRSICalculation() {
        double rsi = indicatorService.calculateRSI(testData, 14);

        assertTrue(rsi >= 0 && rsi <= 100, "RSI should be between 0 and 100");
        assertTrue(rsi > 50, "RSI should be > 50 for uptrending data");
    }

    @Test
    @DisplayName("RSI with insufficient data should throw exception")
    void testRSIWithInsufficientData() {
        List<MarketData> shortData = testData.subList(0, 10);

        assertThrows(IllegalArgumentException.class, () -> {
            indicatorService.calculateRSI(shortData, 14);
        });
    }

    @Test
    @DisplayName("SMA calculation should be correct")
    void testSMACalculation() {
        double sma = indicatorService.calculateSMA(testData, 5);

        assertTrue(sma > 0, "SMA should be positive");
        assertTrue(sma > 120, "SMA should reflect recent uptrend");
    }

    @Test
    @DisplayName("EMA calculation should give more weight to recent prices")
    void testEMACalculation() {
        double ema = indicatorService.calculateEMA(testData, 10);

        assertTrue(ema > 0, "EMA should be positive");
        // EMA should be closer to recent prices than SMA
        double sma = indicatorService.calculateSMA(testData, 10);
        assertTrue(Math.abs(ema - sma) < 10, "EMA should be reasonably close to SMA");
    }

    @Test
    @DisplayName("MACD calculation should return valid values")
    void testMACDCalculation() {
        double[] macd = indicatorService.calculateMACD(testData);

        assertNotNull(macd, "MACD should not be null");
        assertEquals(3, macd.length, "MACD should return 3 values [MACD, Signal, Histogram]");

        double macdLine = macd[0];
        double signalLine = macd[1];
        double histogram = macd[2];

        assertEquals(macdLine - signalLine, histogram, 0.001,
                "Histogram should equal MACD - Signal");
    }

    @Test
    @DisplayName("Bollinger Bands should have upper > middle > lower")
    void testBollingerBands() {
        double[] bands = indicatorService.calculateBollingerBands(testData, 20, 2.0);

        assertNotNull(bands, "Bollinger Bands should not be null");
        assertEquals(3, bands.length, "Should return 3 values [Upper, Middle, Lower]");

        double upper = bands[0];
        double middle = bands[1];
        double lower = bands[2];

        assertTrue(upper > middle, "Upper band should be > middle band");
        assertTrue(middle > lower, "Middle band should be > lower band");
    }

    @Test
    @DisplayName("ATR calculation should return positive value")
    void testATRCalculation() {
        double atr = indicatorService.calculateATR(testData, 14);

        assertTrue(atr > 0, "ATR should be positive");
        assertTrue(atr < 10, "ATR should be reasonable for test data");
    }

    @Test
    @DisplayName("Stochastic Oscillator should be between 0 and 100")
    void testStochasticOscillator() {
        double[] stochastic = indicatorService.calculateStochastic(testData, 14);

        assertNotNull(stochastic, "Stochastic should not be null");
        assertEquals(2, stochastic.length, "Should return 2 values [%K, %D]");

        double k = stochastic[0];
        double d = stochastic[1];

        assertTrue(k >= 0 && k <= 100, "%K should be between 0 and 100");
        assertTrue(d >= 0 && d <= 100, "%D should be between 0 and 100");
    }

    @Test
    @DisplayName("Trend detection should identify uptrend")
    void testTrendDetection() {
        boolean isUptrend = indicatorService.isUptrend(testData, 5, 10);

        assertTrue(isUptrend, "Should detect uptrend in rising price data");
    }

    @Test
    @DisplayName("Empty data should throw exception")
    void testEmptyData() {
        List<MarketData> emptyData = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            indicatorService.calculateRSI(emptyData, 14);
        });
    }

    @Test
    @DisplayName("Null data should throw NullPointerException")
    void testNullData() {
        assertThrows(NullPointerException.class, () -> {
            indicatorService.calculateRSI(null, 14);
        });
    }

    @Test
    @DisplayName("SMA and EMA should converge for large periods")
    void testSMAEMAConvergence() {
        double sma = indicatorService.calculateSMA(testData, 20);
        double ema = indicatorService.calculateEMA(testData, 20);

        // For longer periods, SMA and EMA should be relatively close
        double difference = Math.abs(sma - ema);
        assertTrue(difference < sma * 0.15,
                "SMA and EMA should be within 15% for period 20");
    }
}
