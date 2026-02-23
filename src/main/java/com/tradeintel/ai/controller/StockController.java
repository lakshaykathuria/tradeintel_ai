package com.tradeintel.ai.controller;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.repository.MarketDataRepository;
import com.tradeintel.ai.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for stock management
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StockController {

    private final StockRepository stockRepository;
    private final MarketDataRepository marketDataRepository;

    /**
     * Get all stocks
     */
    @GetMapping
    public ResponseEntity<List<Stock>> getAllStocks() {
        try {
            List<Stock> stocks = stockRepository.findAll();
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            log.error("Error fetching stocks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get stock by symbol
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStockBySymbol(@PathVariable String symbol) {
        try {
            return stockRepository.findBySymbol(symbol)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching stock {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get market data for a stock
     */
    @GetMapping("/{symbol}/market-data")
    public ResponseEntity<?> getMarketData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Stock stock = stockRepository.findBySymbol(symbol)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + symbol));

            List<MarketData> marketData = marketDataRepository
                    .findByStockIdOrderByTimestampDesc(stock.getId())
                    .stream()
                    .limit(limit)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("dataPoints", marketData.size());
            response.put("data", marketData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching market data for {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add a new stock
     */
    @PostMapping
    public ResponseEntity<?> addStock(@RequestBody Stock stock) {
        try {
            // Check if stock already exists
            if (stockRepository.findBySymbol(stock.getSymbol()).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Stock already exists: " + stock.getSymbol()));
            }

            Stock savedStock = stockRepository.save(stock);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedStock);

        } catch (Exception e) {
            log.error("Error adding stock", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
