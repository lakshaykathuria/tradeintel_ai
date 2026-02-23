package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.Portfolio;
import com.tradeintel.ai.model.TradingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByTradingMode(TradingMode tradingMode);

    List<Portfolio> findByIsActiveTrue();

    Optional<Portfolio> findByNameAndTradingMode(String name, TradingMode tradingMode);
}
