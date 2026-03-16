package com.tradeintel.ai.service;

import com.tradeintel.ai.broker.upstox.UpstoxApiClient;
import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import com.upstox.api.GetFullMarketQuoteResponse;
import com.upstox.api.GetHistoricalCandleResponse;
import com.upstox.api.GetIntraDayCandleResponse;
import com.upstox.api.MarketQuoteSymbol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
            // Note: from/to may be null here — defaults applied below
            log.info("Fetching historical data for {} ({}), interval={}", symbol, instrumentKey, interval);

            // Parse / default dates
            LocalDate to = (toDate == null || toDate.isBlank())
                    ? LocalDate.now()
                    : LocalDate.parse(toDate);
            LocalDate from = (fromDate == null || fromDate.isBlank())
                    ? to.minusYears(1)
                    : LocalDate.parse(fromDate);
            if (interval == null || interval.isBlank()) {
                interval = "day";
            }

            // The Upstox V2 ranged historical endpoint:
            //   /v2/historical-candle/{key}/{interval}/{to_date}/{from_date}
            // rejects requests where toDate is today (for intraday) and rejects ranges
            // exceeding ~100 days in a single call.
            //
            // Strategy:
            //   1. For intraday intervals cap toDate at yesterday — today's live candles
            //      are fetched separately via the intraday endpoint.
            //   2. Always fetch in 90-day chunks to stay within the per-call limit.
            boolean isIntraday = interval.contains("minute");
            if (isIntraday && !to.isBefore(LocalDate.now())) {
                to = LocalDate.now().minusDays(1);
            }

            // Find or create stock early so we can stream candles from all chunks
            String cleanSymbol = extractCleanSymbol(symbol, instrumentKey);
            Stock stock = findOrCreateStock(cleanSymbol, instrumentKey);

            int savedCount = 0;
            final int CHUNK_DAYS = 90;
            LocalDate chunkFrom = from;

            while (!chunkFrom.isAfter(to)) {
                LocalDate chunkTo = chunkFrom.plusDays(CHUNK_DAYS - 1);
                if (chunkTo.isAfter(to)) {
                    chunkTo = to;
                }

                String chunkFromStr = chunkFrom.format(DateTimeFormatter.ISO_LOCAL_DATE);
                String chunkToStr   = chunkTo.format(DateTimeFormatter.ISO_LOCAL_DATE);
                log.info("Fetching chunk from={} to={} for {}", chunkFromStr, chunkToStr, cleanSymbol);

                try {
                    GetHistoricalCandleResponse response = upstoxApiClient.getHistoricalData(
                            instrumentKey, interval, chunkToStr, chunkFromStr);

                    if (response != null && response.getData() != null
                            && response.getData().getCandles() != null) {
                        @SuppressWarnings("unchecked")
                        List<List<Object>> candles = (List<List<Object>>) response.getData().getCandles();
                        for (List<Object> candle : candles) {
                            try {
                                savedCount += saveCandle(stock, candle);
                            } catch (Exception e) {
                                log.debug("Skipping candle (likely duplicate): {}", e.getMessage());
                            }
                        }
                        log.info("Chunk {}-{}: {} candles returned", chunkFromStr, chunkToStr, candles.size());
                    }
                } catch (Exception e) {
                    // Skip bad chunks (e.g. holidays-only ranges) and continue
                    log.warn("Chunk {}-{} failed, skipping: {}", chunkFromStr, chunkToStr, e.getMessage());
                }

                chunkFrom = chunkTo.plusDays(1);
            }

            log.info("Saved {} historical candles total for {}", savedCount, cleanSymbol);

            // Always try to fetch today's live data — the historical endpoint never includes
            // today's incomplete candle regardless of interval (day, 30minute, etc.).
            // For intraday intervals the intraday-candle endpoint is tried first;
            // for all intervals the market quote API is the final fallback.
            savedCount += fetchAndSaveTodayIntradayData(instrumentKey, interval, stock);

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

    /**
     * Fetch today's intraday candles and save them.
     *
     * Primary: the /v2/historical-candle/intraday endpoint (returns 30-min bars for today).
     * Fallback: the full market quote endpoint, which saves a single day-level candle
     *           with today's OHLC + volume. This works even outside market hours and is
     *           the same data the dashboard already fetches successfully.
     */
    private int fetchAndSaveTodayIntradayData(String instrumentKey, String interval, Stock stock) {
        // --- Primary: intraday candle bars ---
        try {
            GetIntraDayCandleResponse response = upstoxApiClient.getIntradayData(instrumentKey, interval);
            if (response != null && response.getData() != null
                    && response.getData().getCandles() != null) {
                @SuppressWarnings("unchecked")
                List<List<Object>> candles = (List<List<Object>>) response.getData().getCandles();
                if (!candles.isEmpty()) {
                    int savedCount = 0;
                    for (List<Object> candle : candles) {
                        try {
                            savedCount += saveCandle(stock, candle);
                        } catch (Exception e) {
                            log.debug("Skipping intraday candle (likely duplicate): {}", e.getMessage());
                        }
                    }
                    log.info("Saved {}/{} intraday candles for {} (today)", savedCount, candles.size(), stock.getSymbol());
                    return savedCount;
                }
            }
            log.debug("Intraday candle endpoint returned no candles for {} — trying quote fallback", stock.getSymbol());
        } catch (Exception e) {
            log.warn("Intraday candle fetch failed for {} ({}), falling back to quote: {}",
                    stock.getSymbol(), instrumentKey, e.getMessage());
        }

        // --- Fallback: market quote gives today's day OHLCV ---
        try {
            GetFullMarketQuoteResponse quote = upstoxApiClient.getQuote(instrumentKey);
            if (quote != null && quote.getData() != null) {
                // getData() returns Map<String, MarketQuoteSymbol>
                MarketQuoteSymbol sym = quote.getData().values().stream().findFirst().orElse(null);
                if (sym != null && sym.getOhlc() != null && sym.getLastPrice() != null) {
                    // Use 15:30 as a stable today marker (day-close candle)
                    LocalDateTime todayMark = LocalDate.now().atTime(15, 30);

                    BigDecimal open  = BigDecimal.valueOf(sym.getOhlc().getOpen());
                    BigDecimal high  = BigDecimal.valueOf(sym.getOhlc().getHigh());
                    BigDecimal low   = BigDecimal.valueOf(sym.getOhlc().getLow());
                    BigDecimal close = BigDecimal.valueOf(sym.getLastPrice()); // use LTP as latest close
                    long volume = sym.getVolume() != null ? sym.getVolume() : 0L;

                    // Upsert: update if today's candle already exists
                    Optional<MarketData> existing = marketDataRepository.findByStockAndTimestamp(stock, todayMark);
                    MarketData md = existing.orElseGet(MarketData::new);
                    md.setStock(stock);
                    md.setTimestamp(todayMark);
                    md.setOpenPrice(open);
                    md.setHighPrice(high);
                    md.setLowPrice(low);
                    md.setClosePrice(close);
                    md.setVolume(volume);
                    marketDataRepository.save(md);
                    log.info("Saved today's day candle for {} via quote fallback — LTP={}  Vol={}",
                            stock.getSymbol(), close, volume);
                    return 1;
                }
            }
        } catch (Exception e) {
            log.warn("Quote fallback also failed for {}: {}", stock.getSymbol(), e.getMessage());
        }

        return 0;
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
