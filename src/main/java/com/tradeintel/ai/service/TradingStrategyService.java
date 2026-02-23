package com.tradeintel.ai.service;

import com.tradeintel.ai.model.*;
import com.tradeintel.ai.repository.TradeSignalRepository;
import com.tradeintel.ai.repository.TradingStrategyRepository;
import com.tradeintel.ai.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing and executing trading strategies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradingStrategyService {

    private final TradingStrategyRepository strategyRepository;
    private final TradeSignalRepository signalRepository;
    private final Map<String, TradingStrategy> strategies; // Spring will inject all TradingStrategy beans

    /**
     * Get all available strategies
     */
    public List<com.tradeintel.ai.model.TradingStrategy> getAllStrategies() {
        return strategyRepository.findAll();
    }

    /**
     * Get strategy by ID
     */
    public Optional<com.tradeintel.ai.model.TradingStrategy> getStrategyById(Long id) {
        return strategyRepository.findById(id);
    }

    /**
     * Create a new strategy
     */
    @Transactional
    public com.tradeintel.ai.model.TradingStrategy createStrategy(com.tradeintel.ai.model.TradingStrategy strategy) {
        strategy.setCreatedAt(LocalDateTime.now());
        strategy.setUpdatedAt(LocalDateTime.now());
        return strategyRepository.save(strategy);
    }

    /**
     * Update an existing strategy
     */
    @Transactional
    public com.tradeintel.ai.model.TradingStrategy updateStrategy(Long id,
            com.tradeintel.ai.model.TradingStrategy updatedStrategy) {
        return strategyRepository.findById(id)
                .map(strategy -> {
                    strategy.setName(updatedStrategy.getName());
                    strategy.setDescription(updatedStrategy.getDescription());
                    strategy.setStrategyType(updatedStrategy.getStrategyType());
                    strategy.setParameters(updatedStrategy.getParameters());
                    strategy.setIsActive(updatedStrategy.getIsActive());
                    strategy.setUpdatedAt(LocalDateTime.now());
                    return strategyRepository.save(strategy);
                })
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found with id: " + id));
    }

    /**
     * Delete a strategy
     */
    @Transactional
    public void deleteStrategy(Long id) {
        strategyRepository.deleteById(id);
    }

    /**
     * Execute a strategy on a stock
     * 
     * @param strategyName Name of the strategy implementation class
     * @param stock        Stock to analyze
     * @param marketData   Historical market data
     * @return Generated trade signal
     */
    @Transactional
    public TradeSignal executeStrategy(String strategyName, Stock stock, List<MarketData> marketData) {
        log.info("Executing strategy {} for stock {}", strategyName, stock.getSymbol());

        // Get the strategy implementation (Spring bean)
        TradingStrategy strategyImpl = strategies.get(strategyName);
        if (strategyImpl == null) {
            throw new IllegalArgumentException("Strategy not found: " + strategyName);
        }

        // Validate strategy
        if (!strategyImpl.validate()) {
            throw new IllegalStateException("Strategy validation failed: " + strategyName);
        }

        // Look up or auto-create the TradingStrategy entity (needed for FK)
        com.tradeintel.ai.model.TradingStrategy strategyEntity = strategyRepository.findByName(strategyName)
                .orElseGet(() -> {
                    com.tradeintel.ai.model.TradingStrategy newEntity = new com.tradeintel.ai.model.TradingStrategy();
                    newEntity.setName(strategyName);
                    newEntity.setDescription(strategyName);
                    newEntity.setStrategyType("TECHNICAL");
                    newEntity.setIsActive(true);
                    newEntity.setCreatedAt(LocalDateTime.now());
                    newEntity.setUpdatedAt(LocalDateTime.now());
                    return strategyRepository.save(newEntity);
                });

        // Execute strategy analysis
        TradeSignal signal = strategyImpl.analyze(stock, marketData);

        // Set the required strategy entity (fixes strategy_id NOT NULL constraint)
        signal.setStrategy(strategyEntity);

        // Save the signal
        TradeSignal savedSignal = signalRepository.save(signal);
        log.info("Generated {} signal for {} with confidence {}",
                signal.getSignalType(), stock.getSymbol(), signal.getConfidenceScore());

        return savedSignal;
    }

    /**
     * Execute multiple strategies on a stock and aggregate results
     */
    @Transactional
    public List<TradeSignal> executeMultipleStrategies(List<String> strategyNames, Stock stock,
            List<MarketData> marketData) {
        return strategyNames.stream()
                .map(name -> executeStrategy(name, stock, marketData))
                .toList();
    }

    /**
     * Get all signals for a stock
     */
    public List<TradeSignal> getSignalsForStock(Long stockId) {
        return signalRepository.findByStockIdOrderByCreatedAtDesc(stockId);
    }

    /**
     * Get recent signals for a strategy
     */
    public List<TradeSignal> getRecentSignalsForStrategy(Long strategyId, int limit) {
        return signalRepository.findByStrategyIdOrderByCreatedAtDesc(strategyId)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get active strategies
     */
    public List<com.tradeintel.ai.model.TradingStrategy> getActiveStrategies() {
        return strategyRepository.findByIsActiveTrue();
    }
}
