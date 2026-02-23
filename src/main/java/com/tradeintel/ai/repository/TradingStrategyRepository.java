package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.TradingStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingStrategyRepository extends JpaRepository<TradingStrategy, Long> {
    Optional<TradingStrategy> findByName(String name);

    List<TradingStrategy> findByIsActiveTrue();

    List<TradingStrategy> findByStrategyType(String strategyType);
}
