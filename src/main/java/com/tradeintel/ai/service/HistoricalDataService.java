package com.tradeintel.ai.service;

import com.tradeintel.ai.broker.upstox.UpstoxApiClient;
import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import com.upstox.api.GetHistoricalCandleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to fetch historical market data from Upstox and persist to database.
 * This is needed so trading strategies have enough historical OHLCV data to
 * work with.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataService {

    private final UpstoxApiClient upstoxApiClient;
    private final InstrumentService instrumentService;
    private final StockRepository stockRepository;
    private final MarketDataRepository marketDataRepository;

    /**
     * Fetch historical daily candles for a symbol and save to database.
     *
     * @param symbol   Trading symbol (e.g. "TATAPOWER") or instrument key
     * @param fromDate Start date (YYYY-MM-DD), defaults to 1 year ago
     * @param toDate   End date (YYYY-MM-DD), defaults to today
     * @param interval Candle interval: "day", "week", "month", "1minute",
     *                 "30minute"
     * @return number of candles saved
     */
    @Transactional
    public int fetchAndSaveHistoricalData(String symbol, String fromDate, String toDate, String interval) {
        try {
            // Resolve symbol to instrument key
            String instrumentKey = instrumentService.resolveToInstrumentKey(symbol);
            log.info("Fetching historical data for {} ({}), interval={}, from={} to={}",
                    symbol, instrumentKey, interval, fromDate, toDate);

            // Default dates if not provided
            if (toDate == null || toDate.isBlank()) {
                toDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (fromDate == null || fromDate.isBlank()) {
                fromDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (interval == null || interval.isBlank()) {
                interval = "day";
            }

            // Fetch from Upstox
            GetHistoricalCandleResponse response = upstoxApiClient.getHistoricalData(
                    instrumentKey, interval, toDate, fromDate);

            if (response == null || response.getData() == null) {
                log.warn("No historical data returned for {}", symbol);
                return 0;
            }

            // Find or create stock
            String cleanSymbol = extractCleanSymbol(symbol, instrumentKey);
            Stock stock = findOrCreateStock(cleanSymbol, instrumentKey);

            // Parse and save candles
            // Upstox returns candles as List<List<Object>>: [timestamp, open, high, low,
            // close, volume, oi]
            Object candlesObj = response.getData().getCandles();
            if (candlesObj == null) {
                log.warn("No candles in response for {}", symbol);
                return 0;
            }

            @SuppressWarnings("unchecked")
            List<List<Object>> candles = (List<List<Object>>) candlesObj;
            int savedCount = 0;

            for (List<Object> candle : candles) {
                try {
                    savedCount += saveCandle(stock, candle);
                } catch (Exception e) {
                    log.debug("Skipping candle (likely duplicate): {}", e.getMessage());
                }
            }

            log.info("Saved {}/{} candles for {}", savedCount, candles.size(), cleanSymbol);
            return savedCount;

        } catch (IllegalStateException e) {
            throw new IllegalStateException("Not authenticated with Upstox. Please login first.", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid symbol: " + symbol, e);
        } catch (Exception e) {
            log.error("Error fetching historical data for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch historical data: " + e.getMessage(), e);
        }
    }

    /**
     * Save a single candle to the database. Returns 1 if saved, 0 if skipped.
     */
    private int saveCandle(Stock stock, List<Object> candle) {
        if (candle == null || candle.size() < 6)
            return 0;

        // Candle format: [timestamp, open, high, low, close, volume, oi?]
        String timestampStr = candle.get(0).toString();
        // Upstox timestamps look like "2024-01-15T09:15:00+05:30"
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(timestampStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception e) {
            // Try without timezone
            timestamp = LocalDateTime.parse(timestampStr.substring(0, 19));
        }

        BigDecimal open = toBigDecimal(candle.get(1));
        BigDecimal high = toBigDecimal(candle.get(2));
        BigDecimal low = toBigDecimal(candle.get(3));
        BigDecimal close = toBigDecimal(candle.get(4));
        long volume = toLong(candle.get(5));

        // Check if record already exists
        Optional<MarketData> existing = marketDataRepository.findByStockAndTimestamp(stock, timestamp);
        if (existing.isPresent()) {
            // Only refresh today's candle — it may still be forming intraday.
            // Historical candles are immutable (skip to avoid re-processing).
            if (timestamp.toLocalDate().equals(LocalDate.now())) {
                MarketData md = existing.get();
                md.setOpenPrice(open);
                md.setHighPrice(high);
                md.setLowPrice(low);
                md.setClosePrice(close);
                md.setVolume(volume);
                marketDataRepository.save(md);
                log.debug("Refreshed today's candle for {} at {}", stock.getSymbol(), timestamp);
                return 1;
            }
            return 0; // historical candle already saved — skip
        }

        MarketData md = new MarketData();
        md.setStock(stock);
        md.setTimestamp(timestamp);
        md.setOpenPrice(open);
        md.setHighPrice(high);
        md.setLowPrice(low);
        md.setClosePrice(close);
        md.setVolume(volume);
        marketDataRepository.save(md);
        return 1;
    }

    private Stock findOrCreateStock(String symbol, String instrumentKey) {
        return stockRepository.findBySymbol(symbol).orElseGet(() -> {
            Stock s = new Stock();
            s.setSymbol(symbol);
            s.setName(symbol);
            s.setExchange(instrumentKey.startsWith("BSE") ? "BSE" : "NSE");
            s.setIsActive(true);
            Stock saved = stockRepository.save(s);
            log.info("Created new stock: {}", symbol);
            return saved;
        });
    }

    private String extractCleanSymbol(String symbol, String instrumentKey) {
        // If user passed a plain symbol like "TATAPOWER", use it directly
        if (!symbol.contains("|"))
            return symbol.toUpperCase();
        // Otherwise extract from instrument key: "NSE_EQ|INE245A01021" -> look up in
        // cache
        try {
            var results = instrumentService.searchInstruments(instrumentKey);
            if (!results.isEmpty())
                return results.get(0).getSymbol();
        } catch (Exception ignored) {
        }
        // Last resort: use the part after |
        return instrumentKey.contains("|") ? instrumentKey.split("\\|")[1] : instrumentKey;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long toLong(Object val) {
        if (val == null)
            return 0L;
        try {
            return Long.parseLong(val.toString().split("\\.")[0]);
        } catch (Exception e) {
            return 0L;
        }
    }
}
