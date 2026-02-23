package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findByStockAndTimestampBetweenOrderByTimestampDesc(
            Stock stock, LocalDateTime start, LocalDateTime end);

    List<MarketData> findByStockOrderByTimestampDesc(Stock stock);

    List<MarketData> findByStockSymbolOrderByTimestampDesc(String symbol);

    List<MarketData> findByStockIdOrderByTimestampDesc(Long stockId);

    boolean existsByStockAndTimestamp(Stock stock, java.time.LocalDateTime timestamp);

    Optional<MarketData> findByStockAndTimestamp(Stock stock, LocalDateTime timestamp);
}
