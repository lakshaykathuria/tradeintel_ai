package com.tradeintel.ai.model;

/**
 * Enum representing different types of trading strategies
 */
public enum StrategyType {
    /**
     * Pure technical analysis based on indicators
     */
    TECHNICAL_INDICATOR,

    /**
     * AI-powered analysis using machine learning
     */
    AI_POWERED,

    /**
     * Combination of technical indicators and AI analysis
     */
    HYBRID,

    /**
     * User-defined custom strategies
     */
    CUSTOM
}
