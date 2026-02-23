package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradingStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeSignalRepository extends JpaRepository<TradeSignal, Long> {
    List<TradeSignal> findByIsExecutedFalseOrderByCreatedAtDesc();

    List<TradeSignal> findByStrategyAndIsExecutedFalse(TradingStrategy strategy);

    List<TradeSignal> findByStockAndIsExecutedFalse(Stock stock);
    List<TradeSignal> findByStockIdOrderByCreatedAtDesc(Long stockId);
    List<TradeSignal> findByStrategyIdOrderByCreatedAtDesc(Long strategyId);
}
