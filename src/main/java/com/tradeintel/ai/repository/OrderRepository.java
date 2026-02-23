package com.tradeintel.ai.repository;

import com.tradeintel.ai.model.Order;
import com.tradeintel.ai.model.OrderStatus;
import com.tradeintel.ai.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByPortfolioOrderByPlacedAtDesc(Portfolio portfolio);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPortfolioAndStatus(Portfolio portfolio, OrderStatus status);
}
