package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.Position;
import com.tradeintel.ai.model.Portfolio;
import com.tradeintel.ai.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByPortfolioAndIsOpenTrue(Portfolio portfolio);

    Optional<Position> findByPortfolioAndStockAndIsOpenTrue(Portfolio portfolio, Stock stock);

    List<Position> findByPortfolio(Portfolio portfolio);
}
