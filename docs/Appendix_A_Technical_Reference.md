# APPENDIX A — PROJECT ARTIFACTS & TECHNICAL REFERENCE

## A.1 Project Structure

The TradeIntel AI project follows a clean, modular Maven-based directory structure aligned with Spring Boot best practices:

```
tradeintel_ai/
│
├── src/main/java/com/tradeintel/ai/
│   │
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── BedrockConfig.java
│   │   └── JpaConfig.java
│   │
│   ├── controller/
│   │   ├── MarketDataController.java
│   │   ├── StrategyExecutionController.java
│   │   ├── AIInsightsController.java
│   │   ├── BacktestController.java
│   │   ├── TechnicalIndicatorController.java
│   │   ├── UpstoxAuthController.java
│   │   └── UserController.java
│   │
│   ├── service/
│   │   ├── TradingStrategyService.java
│   │   ├── TechnicalIndicatorService.java
│   │   ├── StrategyScoreEngine.java
│   │   ├── BacktestService.java
│   │   ├── AIStrategyService.java
│   │   ├── UpstoxAuthService.java
│   │   ├── UpstoxMarketDataStreamer.java
│   │   ├── MarketDataPersistenceService.java
│   │   ├── LiveQuotePollerService.java
│   │   ├── InstrumentService.java
│   │   └── NewsFetcherService.java
│   │
│   ├── strategy/
│   │   ├── TradingStrategy.java (Interface)
│   │   ├── AbstractTradingStrategy.java (Base Class)
│   │   ├── RSIStrategy.java
│   │   ├── MACDStrategy.java
│   │   ├── BollingerBandsStrategy.java
│   │   ├── MovingAverageCrossoverStrategy.java
│   │   ├── StochasticOscillatorStrategy.java
│   │   ├── VolumeBreakoutStrategy.java
│   │   ├── SupportResistanceStrategy.java
│   │   ├── SupertrendStrategy.java
│   │   ├── VWAPStrategy.java
│   │   ├── ADXTrendStrategy.java
│   │   ├── ATRVolatilityStrategy.java
│   │   ├── DonchianChannelStrategy.java
│   │   ├── MeanReversionStrategy.java
│   │   ├── GapTradingStrategy.java
│   │   └── NewsSentimentStrategy.java
│   │
│   ├── repository/
│   │   ├── StocksRepository.java
│   │   ├── MarketDataRepository.java
│   │   ├── TradeSignalsRepository.java
│   │   ├── UpstoxTokensRepository.java
│   │   ├── UsersRepository.java
│   │   └── OrdersRepository.java
│   │
│   ├── entity/
│   │   ├── Stocks.java
│   │   ├── MarketData.java
│   │   ├── TradeSignals.java
│   │   ├── UpstoxTokens.java
│   │   ├── Users.java
│   │   └── Orders.java
│   │
│   ├── dto/
│   │   ├── StrategyExecutionRequest.java
│   │   ├── StrategySignalResponse.java
│   │   ├── BacktestRequest.java
│   │   ├── BacktestResults.java
│   │   ├── AIInsightRequest.java
│   │   ├── AIInsightResponse.java
│   │   └── ErrorResponse.java
│   │
│   ├── websocket/
│   │   ├── WebSocketEventListener.java
│   │   └── StompPrincipal.java
│   │
│   ├── kafka/
│   │   ├── MarketDataProducer.java
│   │   └── MarketDataConsumer.java
│   │
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── CustomUserDetailsService.java
│   │   └── HttpBasicAuthProvider.java
│   │
│   ├── util/
│   │   ├── DateTimeUtil.java
│   │   ├── PriceFormatter.java
│   │   └── StringNormalizer.java
│   │
│   ├── exception/
│   │   ├── InsufficientDataException.java
│   │   ├── StrategyExecutionException.java
│   │   ├── AuthenticationException.java
│   │   └── GlobalExceptionHandler.java
│   │
│   ├── TradeIntelAiApplication.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── db/migration/
│   │   ├── V1__initial_schema.sql
│   │   ├── V2__strategy_enhancements.sql
│   │   └── V3__user_portfolio_schema.sql
│   ├── static/
│   │   ├── index.html
│   │   ├── dashboard.html
│   │   ├── app.js
│   │   └── styles.css
│   └── templates/ (if any server-side templates)
│
├── src/test/java/com/tradeintel/ai/
│   ├── service/
│   │   ├── TechnicalIndicatorServiceTest.java
│   │   └── StrategyExecutionControllerTest.java
│   ├── controller/
│   │   └── MarketDataControllerTest.java
│   └── integration/
│       └── EndToEndTest.java
│
├── pom.xml
├── README.md
├── .gitignore
├── .env (excluded from git)
└── docker-compose.yml (planned)
```

**Module Responsibilities:**

| Module | Primary Responsibility | Key Classes |
|--------|----------------------|-------------|
| **config** | Application configuration & bean setup | KafkaConfig, WebSocketConfig, SecurityConfig |
| **controller** | HTTP request handling & routing | 7 REST controllers for all endpoints |
| **service** | Business logic implementation | 11+ service classes for core features |
| **strategy** | Trading strategy implementations | 1 interface + 1 base class + 17 strategies |
| **repository** | Database access (Spring Data JPA) | 6 JPA repository interfaces |
| **entity** | JPA entity mappings | 6 database entity classes |
| **dto** | Data transfer objects | Request/response objects |
| **websocket** | Real-time bidirectional communication | WebSocket event handling |
| **kafka** | Event streaming pipeline | Producer/consumer implementation |
| **security** | Authentication & authorization | JWT, HTTP Basic Auth |
| **util** | Helper utilities | Date, formatting, string manipulation |
| **exception** | Custom exception handling | Global exception handler |

---

## A.2 Database Schema Scripts

### A.2.1 CREATE TABLE Statements (Flyway V1__initial_schema.sql)

```sql
-- Create stocks table
CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255),
    exchange VARCHAR(10),
    instrument_key VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create market_data table (OHLCV candlesticks)
CREATE TABLE market_data (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol),
    timestamp TIMESTAMP NOT NULL,
    open DECIMAL(15,4),
    high DECIMAL(15,4),
    low DECIMAL(15,4),
    close DECIMAL(15,4),
    volume BIGINT,
    interval VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(symbol, timestamp, interval)
);

-- Create trade_signals table
CREATE TABLE trade_signals (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL REFERENCES stocks(symbol),
    strategy_name VARCHAR(100),
    signal VARCHAR(10) NOT NULL,
    confidence DECIMAL(5,4),
    reasoning TEXT,
    target_price DECIMAL(15,4),
    stop_loss DECIMAL(15,4),
    entry_price DECIMAL(15,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create upstox_tokens table
CREATE TABLE upstox_tokens (
    id BIGSERIAL PRIMARY KEY,
    access_token TEXT NOT NULL,
    token_type VARCHAR(50),
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    email VARCHAR(255),
    role VARCHAR(50) DEFAULT 'TRADER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    symbol VARCHAR(20),
    order_type VARCHAR(10) NOT NULL,
    quantity INTEGER,
    price DECIMAL(15,4),
    status VARCHAR(20) DEFAULT 'PENDING',
    trading_mode VARCHAR(20),
    commission DECIMAL(10,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### A.2.2 Important Indexes (Flyway V2__strategy_enhancements.sql)

```sql
-- Indexes for performance optimization
CREATE INDEX idx_market_data_symbol ON market_data(symbol);
CREATE INDEX idx_market_data_timestamp ON market_data(timestamp);
CREATE INDEX idx_market_data_interval ON market_data(interval);
CREATE INDEX idx_trade_signals_symbol ON trade_signals(symbol);
CREATE INDEX idx_trade_signals_created ON trade_signals(created_at);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);

-- Composite index for common queries
CREATE INDEX idx_market_data_symbol_ts ON market_data(symbol, timestamp DESC);
```

### A.2.3 Flyway Migration Summary

| Version | Filename | Purpose | Status |
|---------|----------|---------|--------|
| V1 | initial_schema.sql | Creates 7 core tables | ✅ Applied |
| V2 | strategy_enhancements.sql | Adds performance indexes | ✅ Applied |
| V3 | user_portfolio_schema.sql | Adds portfolio tracking | ✅ Applied |

---

## A.3 Important REST API Endpoints

### A.3.1 Market Data Endpoints

| Endpoint | Method | Purpose | Auth | Response Time |
|----------|--------|---------|------|----------------|
| `/market-data/search` | GET | Search for instruments by symbol | ✅ Required | 200-500ms |
| `/market-data/historical` | GET | Fetch stored historical OHLCV data | ✅ Required | 1-3s |
| `/market-data/fetch-historical` | POST | Trigger new historical data fetch from Upstox | ✅ Required | 3-5s |
| `/market-data/live/{symbol}` | GET | Get latest live quote for symbol | ✅ Required | 100-200ms |

### A.3.2 Strategy Execution Endpoints

| Endpoint | Method | Purpose | Request Body | Response Time |
|----------|--------|---------|--------------|----------------|
| `/execute/available-strategies` | GET | List all 17 strategies | N/A | 50ms |
| `/execute/strategy` | POST | Execute single strategy on symbol | `{symbol, strategy}` | 300-800ms |
| `/execute/multiple` | POST | Execute multiple strategies + consensus | `{symbol, strategies[]}` | 1.5-3s |
| `/execute/score-engine` | POST | Get weighted consensus from strategy signals | `{signals[]}` | 100-200ms |

### A.3.3 AI Insights Endpoints

| Endpoint | Method | Purpose | Request Body | SLA |
|----------|--------|---------|--------------|-----|
| `/ai/analyze` | POST | Generate AI insights for symbol | `{symbol, analysisType}` | 7-15s |
| `/ai/sentiment` | POST | Market sentiment analysis only | `{symbol}` | 5-10s |
| `/ai/pattern` | POST | Chart pattern detection | `{symbol}` | 5-10s |
| `/ai/regime` | POST | Market regime classification | `{symbol}` | 3-8s |

### A.3.4 Backtesting Endpoints

| Endpoint | Method | Purpose | Request Body | Response |
|----------|--------|---------|--------------|----------|
| `/backtest` | POST | Run backtest on strategy | `{symbol, strategy, startDate, endDate, capital}` | Metrics JSON |
| `/backtest/trades` | POST | Get trade-by-trade log | `{backtest_id}` | Trade array |

### A.3.5 Technical Indicators Endpoints

| Endpoint | Method | Purpose | Query Params | Response |
|----------|--------|---------|--------------|----------|
| `/indicators/rsi` | GET | Calculate RSI | `symbol, period=14` | RSI values |
| `/indicators/macd` | GET | Calculate MACD | `symbol` | MACD, Signal, Histogram |
| `/indicators/bollinger` | GET | Calculate Bollinger Bands | `symbol, period=20` | Upper, Middle, Lower |
| `/indicators/sma` | GET | Simple Moving Average | `symbol, period` | SMA array |
| `/indicators/ema` | GET | Exponential Moving Average | `symbol, period` | EMA array |

### A.3.6 Authentication Endpoints

| Endpoint | Method | Purpose | Response |
|----------|--------|---------|----------|
| `/upstox/auth-url` | GET | Get OAuth2 authorization URL | `{authUrl}` |
| `/upstox/callback` | GET | OAuth2 callback handler | Redirects + stores token |
| `/upstox/status` | GET | Check authentication status | `{authenticated, expiresAt}` |
| `/upstox/logout` | POST | Clear stored token | `{status}` |

---

## A.4 Key Algorithms (Pseudo-Code & Flow Logic)

### A.4.1 RSI Strategy Algorithm

```
ALGORITHM RSIStrategy(symbol, period = 14)
  
  INPUT: Trading symbol, RSI period
  OUTPUT: TradeSignal {BUY, SELL, or HOLD with confidence}
  
  Step 1: Fetch last 15 candles from MarketData repository
  IF candles.size() < period + 1 THEN
    RETURN HOLD (insufficient data)
  END IF
  
  Step 2: Extract closing prices array
    closes[] ← [candle.close for each candle]
  
  Step 3: Calculate price changes
    changes[] ← [closes[i] - closes[i-1] for i = 1 to length]
  
  Step 4: Separate gains and losses
    gains[] ← [change > 0 ? change : 0]
    losses[] ← [change < 0 ? abs(change) : 0]
  
  Step 5: Calculate average gain and loss using Wilder's smoothing
    avgGain ← (sum of first period gains) / period
    avgLoss ← (sum of first losses) / period
    FOR each subsequent candle:
      avgGain ← (avgGain × (period - 1) + current_gain) / period
      avgLoss ← (avgLoss × (period - 1) + current_loss) / period
    END FOR
  
  Step 6: Compute RSI
    IF avgLoss = 0 THEN
      RSI ← 100
    ELSE
      RS ← avgGain / avgLoss
      RSI ← 100 - (100 / (1 + RS))
    END IF
  
  Step 7: Generate signal
    IF RSI < 30 THEN
      SIGNAL ← BUY
      CONFIDENCE ← (30 - RSI) / 30  // Scale confidence
      REASONING ← "Oversold condition detected. Price likely to recover."
    ELSE IF RSI > 70 THEN
      SIGNAL ← SELL
      CONFIDENCE ← (RSI - 70) / 30
      REASONING ← "Overbought condition detected. Profit-taking likely."
    ELSE
      SIGNAL ← HOLD
      CONFIDENCE ← 0.5
      REASONING ← "Neutral zone. Await clear signal."
    END IF
  
  Step 8: Calculate target price and stop-loss
    currentPrice ← closes[last]
    IF SIGNAL = BUY THEN
      TARGET_PRICE ← currentPrice × 1.05  // 5% upside target
      STOP_LOSS ← currentPrice × 0.98     // 2% downside stop
    ELSE IF SIGNAL = SELL THEN
      TARGET_PRICE ← currentPrice × 0.95
      STOP_LOSS ← currentPrice × 1.02
    END IF
  
  RETURN TradeSignal(SIGNAL, CONFIDENCE, REASONING, TARGET_PRICE, STOP_LOSS)
END ALGORITHM
```

### A.4.2 MACD Strategy Algorithm

```
ALGORITHM MACDStrategy(symbol)
  
  INPUT: Trading symbol
  OUTPUT: TradeSignal {BUY, SELL, or HOLD}
  
  Step 1: Fetch last 35 candles (26-period EMA needs 34 prior candles + 1)
    closes[] ← historical closing prices
  
  Step 2: Calculate 12-period EMA
    EMA12 ← calculateEMA(closes, 12)
    multiplier12 ← 2 / (12 + 1)
  
  Step 3: Calculate 26-period EMA
    EMA26 ← calculateEMA(closes, 26)
    multiplier26 ← 2 / (26 + 1)
  
  Step 4: Calculate MACD Line
    MACDLine ← EMA12 - EMA26
  
  Step 5: Calculate Signal Line (9-period EMA of MACD)
    SignalLine ← calculateEMA(MACDLine[], 9)
  
  Step 6: Calculate MACD Histogram
    Histogram ← MACDLine - SignalLine
  
  Step 7: Detect crossover
    previousHistogram ← Histogram[t-1]
    currentHistogram ← Histogram[t]
    
    IF previousHistogram < 0 AND currentHistogram > 0 THEN
      SIGNAL ← BUY  // Bullish crossover
      CONFIDENCE ← 0.75
      REASONING ← "MACD bullish crossover detected."
    ELSE IF previousHistogram > 0 AND currentHistogram < 0 THEN
      SIGNAL ← SELL  // Bearish crossover
      CONFIDENCE ← 0.75
      REASONING ← "MACD bearish crossover detected."
    ELSE IF currentHistogram > 0 AND MACDLine > SignalLine THEN
      SIGNAL ← HOLD  // Uptrend ongoing
      CONFIDENCE ← 0.6
      REASONING ← "MACD in positive territory; uptrend confirmed."
    ELSE
      SIGNAL ← HOLD
      CONFIDENCE ← 0.5
    END IF
  
  RETURN TradeSignal(SIGNAL, CONFIDENCE, REASONING)
END ALGORITHM
```

### A.4.3 Strategy Score Engine Algorithm

```
ALGORITHM StrategyScoreEngine(strategySignals[])
  
  INPUT: Array of TradeSignal objects from multiple strategies
  OUTPUT: Consensus TradeSignal with weighted score
  
  Step 1: Assign strategy weights based on historical reliability
    weights = {
      RSI: 1.0,
      MACD: 1.1,
      BollingerBands: 0.9,
      MACrossover: 1.0,
      Supertrend: 1.1,
      ADXTrend: 1.0,
      ... (17 strategies)
    }
  
  Step 2: Convert signals to numeric scores
    FOR each strategy signal IN strategySignals:
      IF signal = BUY THEN score ← +1
      ELSE IF signal = SELL THEN score ← -1
      ELSE score ← 0  (HOLD)
    END FOR
  
  Step 3: Calculate weighted score
    totalWeight ← 0
    weightedSum ← 0
    
    FOR each strategy IN strategySignals:
      weight ← weights[strategy.name]
      weightedSum ← weightedSum + (score × weight × confidence)
      totalWeight ← totalWeight + weight
    END FOR
    
    weightedAverage ← weightedSum / totalWeight
  
  Step 4: Determine consensus signal
    IF weightedAverage > 0.3 THEN
      CONSENSUS ← BUY
    ELSE IF weightedAverage < -0.3 THEN
      CONSENSUS ← SELL
    ELSE
      CONSENSUS ← HOLD
    END IF
  
  Step 5: Calculate consensus confidence
    consensusConfidence ← abs(weightedAverage)
    IF consensusConfidence > 1.0 THEN
      consensusConfidence ← 1.0
    END IF
  
  Step 6: Aggregate reasoning from all strategies
    reasoning ← concatenate all individual strategy reasoning texts
  
  RETURN TradeSignal(CONSENSUS, consensusConfidence, reasoning)
END ALGORITHM
```

### A.4.4 Backtesting Workflow

```
ALGORITHM BacktestingEngine(symbol, strategy, startDate, endDate, initialCapital)
  
  INPUT: Symbol, strategy name, date range, starting capital
  OUTPUT: BacktestResults {metrics, trade log}
  
  Step 1: Fetch historical OHLCV data for date range
    ohlcvData[] ← query market_data table WHERE symbol = symbol 
                  AND timestamp BETWEEN startDate AND endDate
                  ORDER BY timestamp ASC
    
    IF ohlcvData.empty() THEN
      RETURN ERROR "No historical data available"
    END IF
  
  Step 2: Initialize backtesting state
    portfolio.cash ← initialCapital
    portfolio.position ← null  (no open position)
    portfolio.trades[] ← []    (trade history)
    portfolio.value ← initialCapital
    
    maxPortfolioValue ← initialCapital
    maxDrawdown ← 0
  
  Step 3: MAIN BACKTESTING LOOP
    FOR EACH candle IN ohlcvData:
      
      Step 3a: Execute strategy on current OHLCV data
        signal ← strategy.execute(symbol, ohlcvData[0:currentIndex])
        // NO LOOKAHEAD: Only use candles up to current point
      
      Step 3b: Execute trading logic
        IF signal = BUY AND portfolio.position = null THEN
          // Open long position at close price
          entryPrice ← candle.close
          maxSharesAffordable ← portfolio.cash / entryPrice
          quantity ← min(maxSharesAffordable, riskLimitedQuantity)
          
          portfolio.position ← {
            entryPrice: entryPrice,
            quantity: quantity,
            entryTime: candle.timestamp
          }
          portfolio.cash ← portfolio.cash - (quantity × entryPrice + commission)
        
        ELSE IF signal = SELL AND portfolio.position ≠ null THEN
          // Exit long position at close price
          exitPrice ← candle.close
          profit ← (exitPrice - position.entryPrice) × position.quantity - commission
          
          portfolio.trades.append({
            entryPrice: position.entryPrice,
            exitPrice: exitPrice,
            quantity: position.quantity,
            profit: profit,
            pnlPercent: (profit / (position.entryPrice × quantity)) × 100
          })
          
          portfolio.cash ← portfolio.cash + (quantity × exitPrice - commission)
          portfolio.position ← null
        
        END IF
      
      Step 3c: Update portfolio value and drawdown
        IF portfolio.position ≠ null THEN
          unrealizedProfit ← (candle.close - position.entryPrice) × position.quantity
          portfolio.value ← portfolio.cash + unrealizedProfit
        ELSE
          portfolio.value ← portfolio.cash
        END IF
        
        IF portfolio.value > maxPortfolioValue THEN
          maxPortfolioValue ← portfolio.value
        ELSE
          drawdown ← (maxPortfolioValue - portfolio.value) / maxPortfolioValue
          IF drawdown > maxDrawdown THEN
            maxDrawdown ← drawdown
          END IF
        END IF
    
    END FOR
  
  Step 4: Close any open position at end of backtest
    IF portfolio.position ≠ null THEN
      finalPrice ← ohlcvData[last].close
      profit ← (finalPrice - position.entryPrice) × position.quantity - commission
      portfolio.trades.append({...})
      portfolio.cash ← portfolio.cash + (quantity × finalPrice)
    END IF
  
  Step 5: Calculate performance metrics
    totalReturn ← ((portfolio.value - initialCapital) / initialCapital) × 100
    totalTrades ← portfolio.trades.length
    winningTrades ← COUNT(trades WHERE profit > 0)
    winRate ← (winningTrades / totalTrades) × 100
    
    avgWin ← AVERAGE(trades WHERE profit > 0).profit
    avgLoss ← AVERAGE(trades WHERE profit < 0).profit
    profitFactor ← abs(TOTAL(winning trades) / TOTAL(losing trades))
    
    sharpeRatio ← calculateSharpeRatio(portfolio.dailyReturns)
  
  Step 6: Return results
    RETURN BacktestResults {
      totalReturn,
      winRate,
      totalTrades,
      sharpeRatio,
      maxDrawdown,
      profitFactor,
      trades: portfolio.trades
    }
END ALGORITHM
```

### A.4.5 AI Analysis Flow

```
ALGORITHM AIAnalysisFlow(symbol, analysisType)
  
  INPUT: Stock symbol, analysis type (COMPREHENSIVE/SENTIMENT/PATTERN/REGIME)
  OUTPUT: AI-generated narrative insights
  
  Step 1: Gather market context
    recentCandles[] ← FETCH last 50 candles from market_data
    currentPrice ← recentCandles[last].close
    priceChange ← ((currentPrice - recentCandles[0].close) / recentCandles[0].close) × 100
    volume ← AVERAGE(recentCandles.volume)
    volatility ← calculateStandardDeviation(recentCandles.close)
  
  Step 2: Execute trading strategies
    strategyResults[] ← []
    FOR EACH strategy IN [RSI, MACD, BollingerBands, ... all 17]:
      signal ← strategy.execute(symbol)
      strategyResults.append(signal)
    END FOR
    
    consensus ← StrategyScoreEngine(strategyResults)
  
  Step 3: Fetch recent news headlines
    news[] ← NewsFetcherService.fetch(symbol, limit=5)
  
  Step 4: Construct prompt for Claude AI
    PROMPT ← """
    You are a quantitative financial analyst specializing in Indian equity markets.
    
    Stock: {symbol}
    Current Price: {currentPrice}
    Price Change (50-candle period): {priceChange}%
    Average Volume: {volume}
    Volatility: {volatility}
    
    Trading Strategies Summary:
    {strategyResults formatted as table}
    
    Consensus Signal: {consensus.signal}
    Consensus Confidence: {consensus.confidence}
    
    Recent News Headlines:
    {news formatted as bullet points}
    
    Analysis Type Requested: {analysisType}
    
    Provide a concise, actionable analysis in 2-3 paragraphs.
    """
  
  Step 5: Call Amazon Bedrock Claude API
    response ← BedrockChatModel.call(PROMPT)
    // Model: us.anthropic.claude-opus-4-7
    // Tokens: max 4096
    // Temperature: 0 (deterministic)
  
  Step 6: Format and return response
    RETURN AIInsightResponse {
      symbol: symbol,
      analysisType: analysisType,
      insight: response.text,
      generatedAt: CURRENT_TIMESTAMP,
      modelUsed: "Claude Opus 4.7",
      tokensUsed: response.tokenUsage
    }
END ALGORITHM
```

---

## A.5 Configuration Snapshots

### A.5.1 application.yml (Masked Credentials)

```yaml
spring:
  application:
    name: tradeintel-ai
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/tradeintel_db
    username: ${DB_USERNAME:tradeintel_user}
    password: ${DB_PASSWORD:changeme}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  # JPA / Hibernate
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL16Dialect
        format_sql: true
  
  # Flyway Migration
  flyway:
    baseline-on-migrate: true
    enabled: true
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      retries: 3
      acks: all
    consumer:
      bootstrap-servers: localhost:9092
      group-id: tradeintel-consumer-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /api

# Logging Configuration
logging:
  level:
    root: INFO
    com.tradeintel.ai: DEBUG
  file:
    name: logs/trading-platform.log
    max-size: 10MB
    max-history: 30

# Actuator (Monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# AWS Bedrock Configuration (via Environment Variables)
aws:
  bedrock:
    region: us-east-1
    model-id: us.anthropic.claude-opus-4-7
    max-tokens: 4096
    temperature: 0

# Upstox API Configuration (via Environment Variables)
upstox:
  api-base-url: https://api.upstox.com/v2
  websocket-url: wss://stream.upstox.com/feed/
  client-id: ${UPSTOX_CLIENT_ID}
  client-secret: ${UPSTOX_CLIENT_SECRET}
  redirect-uri: http://localhost:8080/api/upstox/callback

# Security Configuration
security:
  username: ${SECURITY_USERNAME:admin}
  password: ${SECURITY_PASSWORD:admin123}
  
  cors:
    allowed-origins: http://localhost:3000,http://localhost:8080
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: '*'
    allow-credentials: true
```

### A.5.2 Kafka Topic Configuration (KafkaConfig.java)

```java
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic marketDataTopic() {
        return TopicBuilder.name("market-data-topic")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000") // 24 hours
                .build();
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic signalEventsTopic() {
        return TopicBuilder.name("signal-events-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic portfolioUpdatesTopic() {
        return TopicBuilder.name("portfolio-updates-topic")
                .partitions(2)
                .replicas(1)
                .build();
    }

    // Kafka Template for producers
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### A.5.3 WebSocket Configuration (WebSocketConfig.java)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000});
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.info("WebSocket client connected");
                }
                return message;
            }
        });
    }
}
```

### A.5.4 Security Configuration (SecurityConfig.java)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/upstox/callback").permitAll()
                .requestMatchers("/api/actuator/health").permitAll()
                .requestMatchers("/ws").permitAll()
                .requestMatchers("/static/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf().disable()
            .cors(Customizer.withDefaults());
        
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## A.6 Sample JSON Request/Response Payloads

### A.6.1 Execute Single Strategy

**Request:**
```json
POST /api/execute/strategy
Content-Type: application/json

{
  "symbol": "RELIANCE",
  "strategy": "RSI",
  "period": 14,
  "averageBuyPrice": 2850.50
}
```

**Response:**
```json
{
  "success": true,
  "symbol": "RELIANCE",
  "strategy": "RSI",
  "signal": "BUY",
  "confidence": 0.72,
  "reasoning": "RSI at 28.4 indicates oversold condition. Price showing signs of recovery with volume increasing. Suitable for entry with risk management.",
  "targetPrice": 2950.00,
  "stopLoss": 2790.50,
  "entryPrice": 2850.50,
  "timestamp": "2026-05-10T14:30:45.123Z"
}
```

### A.6.2 Execute Multiple Strategies (Consensus)

**Request:**
```json
POST /api/execute/multiple
Content-Type: application/json

{
  "symbol": "INFY",
  "strategies": ["RSI", "MACD", "BollingerBands", "Supertrend"]
}
```

**Response:**
```json
{
  "success": true,
  "symbol": "INFY",
  "consensus": {
    "signal": "BUY",
    "confidence": 0.76,
    "reasoning": "Multi-strategy consensus indicates bullish setup. RSI oversold, MACD bullish crossover, price near lower Bollinger Band, Supertrend confirming uptrend."
  },
  "individualResults": [
    {
      "strategy": "RSI",
      "signal": "BUY",
      "confidence": 0.68,
      "reasoning": "RSI = 24 (oversold)"
    },
    {
      "strategy": "MACD",
      "signal": "BUY",
      "confidence": 0.74,
      "reasoning": "Bullish crossover detected"
    },
    {
      "strategy": "BollingerBands",
      "signal": "BUY",
      "confidence": 0.82,
      "reasoning": "Price at lower band"
    },
    {
      "strategy": "Supertrend",
      "signal": "BUY",
      "confidence": 0.78,
      "reasoning": "Price above Supertrend line"
    }
  ],
  "timestamp": "2026-05-10T14:45:22.456Z"
}
```

### A.6.3 Backtest Request/Response

**Request:**
```json
POST /api/backtest
Content-Type: application/json

{
  "symbol": "TATAPOWER",
  "strategy": "RSI",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 100000,
  "commission": 20
}
```

**Response:**
```json
{
  "success": true,
  "symbol": "TATAPOWER",
  "strategy": "RSI",
  "backtest": {
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "initialCapital": 100000,
    "finalCapital": 118600,
    "metrics": {
      "totalReturn": 18.6,
      "winRate": 62.5,
      "totalTrades": 16,
      "winningTrades": 10,
      "losingTrades": 6,
      "sharpeRatio": 1.24,
      "maxDrawdown": -7.8,
      "profitFactor": 1.87,
      "avgWin": 2156.25,
      "avgLoss": -1147.50
    },
    "trades": [
      {
        "tradeId": 1,
        "entryDate": "2024-01-15",
        "entryPrice": 245.50,
        "exitDate": "2024-01-25",
        "exitPrice": 252.30,
        "quantity": 100,
        "pnl": 580,
        "pnlPercent": 2.76
      }
      // ... more trades
    ]
  },
  "generatedAt": "2026-05-10T15:30:00.000Z"
}
```

### A.6.4 AI Insights Request/Response

**Request:**
```json
POST /api/ai/analyze
Content-Type: application/json

{
  "symbol": "WIPRO",
  "analysisType": "COMPREHENSIVE"
}
```

**Response:**
```json
{
  "success": true,
  "symbol": "WIPRO",
  "analysisType": "COMPREHENSIVE",
  "insight": "Wipro is currently trading in a consolidation phase with technical indicators showing mixed signals. The RSI has recovered from oversold levels (28), suggesting renewed buying interest, while the MACD remains in positive territory but with weakening momentum. The stock faces near-term resistance at ₹510, with support holding at ₹485. Given the recent positive divergence in volume and the consensus of three out of four strategies indicating BUY signals, a cautious long setup with a tight stop-loss below support appears justified. Risk-reward ratio appears favorable at 1:2.5. Monitor for breakout above ₹510 for confirmation of uptrend continuation.",
  "modelUsed": "Claude Opus 4.7",
  "tokensUsed": 287,
  "generatedAt": "2026-05-10T16:15:33.789Z"
}
```

### A.6.5 Technical Indicator Request/Response

**Request:**
```json
GET /api/indicators/rsi?symbol=HDFCBANK&period=14
```

**Response:**
```json
{
  "success": true,
  "symbol": "HDFCBANK",
  "indicator": "RSI",
  "period": 14,
  "data": [
    {
      "timestamp": "2026-05-08",
      "close": 1650.50,
      "rsi": 45.32
    },
    {
      "timestamp": "2026-05-09",
      "close": 1655.75,
      "rsi": 48.67
    },
    {
      "timestamp": "2026-05-10",
      "close": 1662.30,
      "rsi": 52.41
    }
  ],
  "current": {
    "value": 52.41,
    "status": "NEUTRAL",
    "interpretation": "RSI in neutral zone; awaiting directional catalyst"
  }
}
```

---

## A.7 Sample Logs & Output

### A.7.1 Kafka Event Log Sample

```
2026-05-10 14:30:15.243 [pool-1-thread-1] INFO  KafkaProducer - Published market data event
Topic: market-data-topic | Partition: 1 | Offset: 45823
Event: {symbol: "RELIANCE", timestamp: "2026-05-10T14:30:15.000Z", 
        open: 2840.50, high: 2860.75, low: 2835.00, close: 2858.25, volume: 2450000}

2026-05-10 14:30:16.512 [kafka-consumer-0] INFO  MarketDataConsumer - Consumed message
Topic: market-data-topic | Partition: 1 | Offset: 45823
Persisting to database: RELIANCE candlestick (2026-05-10 14:30:00)

2026-05-10 14:30:17.834 [http-nio-8080-exec-5] INFO  StrategyExecutor - Strategy execution completed
Symbol: RELIANCE | Strategy: RSI | Signal: BUY | Confidence: 0.72
Reasoning: RSI oversold at 28.4; recovery expected. Entry: 2858.25 | SL: 2790.50
```

### A.7.2 Strategy Execution Output Sample

```
═══════════════════════════════════════════════════════════════════════════
STRATEGY EXECUTION REPORT
═══════════════════════════════════════════════════════════════════════════

Symbol: TATAPOWER
Execution Time: 2026-05-10 14:45:22.156 UTC
Data Points Analyzed: 252 candles (1-year daily)

────────────────────────────────────────────────────────────────────────────
INDIVIDUAL STRATEGY RESULTS
────────────────────────────────────────────────────────────────────────────

1. RSI Strategy (Period: 14)
   Signal: BUY | Confidence: 0.68
   Current RSI: 28.4 | Status: Oversold
   Reasoning: Price showing recovery; gaining volume

2. MACD Strategy
   Signal: BUY | Confidence: 0.74
   MACD Line: +12.35 | Signal Line: +8.92 | Histogram: +3.43
   Status: Bullish crossover

3. Bollinger Bands Strategy (Period: 20)
   Signal: BUY | Confidence: 0.82
   Upper: 265.50 | Middle: 255.75 | Lower: 246.00
   Current Price: 248.30 | Status: Near lower band

4. Moving Average Crossover (9/21)
   Signal: HOLD | Confidence: 0.55
   SMA9: 252.10 | SMA21: 254.80 | Status: Bearish alignment

5. Supertrend Strategy (ATR-based)
   Signal: BUY | Confidence: 0.78
   Supertrend Level: 245.60 | Current Price: 248.30 | Status: Above line

[... 12 more strategies ...]

────────────────────────────────────────────────────────────────────────────
CONSENSUS SIGNAL
────────────────────────────────────────────────────────────────────────────

Final Signal: BUY
Confidence: 0.73 (Weighted)
Agreement: 13 of 17 strategies bullish

Target Entry Price: 248.30
Stop Loss: 243.33 (2% below entry)
Target Price: 260.71 (5% above entry)
Risk/Reward Ratio: 1:2.5

═══════════════════════════════════════════════════════════════════════════
```

### A.7.3 AI Response Sample Output

```
═══════════════════════════════════════════════════════════════════════════
AI MARKET ANALYSIS REPORT
═══════════════════════════════════════════════════════════════════════════

Symbol: INFY (Infosys Limited)
Analysis Date: 2026-05-10 16:45:00 UTC
Analysis Type: COMPREHENSIVE

Market Regime: NEUTRAL-TO-BULLISH (Recent recovery from support)

SENTIMENT ANALYSIS:
Recent news sentiment shows 70% positive bias driven by expectations of 
improved global IT spending and strong earnings outlook. The stock has 
recovered from 52-week lows and is consolidating near the 200-day moving 
average, indicating institutional accumulation.

TECHNICAL PATTERN RECOGNITION:
A symmetrical triangle pattern has formed over the past 20 trading days. 
The stock is testing the upper trendline resistance at ₹1890, with 
confirmation of breakout expected within 2-3 trading sessions. Volume 
profile shows increasing participation on up moves, suggesting readiness 
for a directional breakout.

MARKET REGIME:
The 50-period and 200-period moving averages are in bullish alignment 
(50 > 200), and the ADX indicator reads 32 (strong trend). However, 
the 14-period RSI at 62 indicates we are still in early-to-mid uptrend 
phase with room for further appreciation before overbought signals emerge.

MULTI-STRATEGY CONSENSUS:
- BUY Signals: 14 of 17 strategies
- HOLD Signals: 3 of 17 strategies
- Consensus Confidence: 78% (Strong)

RECOMMENDATION:
CAUTIOUS ACCUMULATION with tight risk management. Position entry near ₹1885 
with a stop-loss at ₹1820 (2% risk) offers a favorable risk-reward profile 
of approximately 1:3.5. Scale into positions rather than deploying capital 
all at once, given the consolidation pattern that could still resolve 
downward if support fails.

KEY LEVELS TO WATCH:
- Resistance: ₹1890, ₹1920, ₹1950
- Support: ₹1840, ₹1780 (52-week average)

NEXT CATALYST:
Company earnings announcement expected on 2026-05-25. Ahead of earnings, 
expect continued consolidation with intraday swings. Increase position size 
only after confirmed breakout above ₹1890.

═══════════════════════════════════════════════════════════════════════════
Report Generated by: Amazon Bedrock Claude Opus 4.7
Execution Time: 7.3 seconds
Confidence Level: HIGH
═══════════════════════════════════════════════════════════════════════════
```

### A.7.4 Application Health Check Output

```json
GET /api/actuator/health

{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "kafkaHealthIndicator": {
      "status": "UP",
      "details": {
        "status": "UP",
        "brokerId": 0,
        "clusterId": "MkQkR2e0SFeed-nCEggOHQ",
        "brokers": 1
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 483183820800,
        "free": 356920201216,
        "threshold": 10485760,
        "exists": true
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

---

**END OF APPENDIX A**
