# CHAPTER 4 — DESIGN

The design phase translates the requirements identified in the SRS into concrete architectural blueprints, data models, process flows, and interface layouts. This chapter presents the complete system design of TradeIntel AI through five key design artifacts: the Entity Relationship (ER) Diagram, Data Flow Diagrams (Level 0 and Level 1), Module descriptions, Database schema, and Input-Output screen layouts.

## 4.1 ER Diagram

The ER diagram represents the data entities in the system, their attributes, and the relationships between them. The TradeIntel AI database consists of the following core entities:

### 4.1.1 Entities and Attributes

**Entity 1: STOCKS**
Stores metadata about each tracked financial instrument.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| symbol | VARCHAR(20) | UNIQUE, NOT NULL | e.g., "TATAPOWER", "RELIANCE" |
| name | VARCHAR(255) | | Full company name |
| exchange | VARCHAR(10) | | NSE or BSE |
| instrument_key | VARCHAR(100) | | Upstox internal instrument identifier |
| created_at | TIMESTAMP | | Record creation time |

**Entity 2: MARKET_DATA**
Stores OHLCV candlestick data records for each symbol.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| symbol | VARCHAR(20) | NOT NULL, FK → STOCKS.symbol | Stock ticker |
| timestamp | TIMESTAMP | NOT NULL | Candle timestamp |
| open | DECIMAL(15,4) | | Opening price |
| high | DECIMAL(15,4) | | Highest price |
| low | DECIMAL(15,4) | | Lowest price |
| close | DECIMAL(15,4) | | Closing price |
| volume | BIGINT | | Trading volume |
| interval | VARCHAR(20) | | e.g., "day", "1minute" |
| created_at | TIMESTAMP | DEFAULT NOW() | Record insertion time |
| **Constraint** | UNIQUE(symbol, timestamp, interval) | | Prevents duplicate candles |

**Entity 3: TRADE_SIGNALS**
Stores the output of each strategy execution.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| symbol | VARCHAR(20) | NOT NULL, FK → STOCKS.symbol | Stock ticker |
| strategy_name | VARCHAR(100) | | Name of strategy that generated signal |
| signal | VARCHAR(10) | NOT NULL | BUY, SELL, or HOLD |
| confidence | DECIMAL(5,4) | | 0.0000 to 1.0000 |
| reasoning | TEXT | | Human-readable explanation |
| target_price | DECIMAL(15,4) | | Target price for the trade |
| stop_loss | DECIMAL(15,4) | | Stop-loss price |
| entry_price | DECIMAL(15,4) | | Signal entry price |
| created_at | TIMESTAMP | DEFAULT NOW() | Signal generation time |

**Entity 4: UPSTOX_TOKENS**
Stores OAuth2 access tokens for Upstox authentication.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| access_token | TEXT | NOT NULL | Bearer token |
| token_type | VARCHAR(50) | | typically "Bearer" |
| expires_at | TIMESTAMP | | Token expiry time |
| created_at | TIMESTAMP | DEFAULT NOW() | Token storage time |

**Entity 5: USERS**
Stores user account information.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| username | VARCHAR(100) | UNIQUE | Login username |
| password_hash | VARCHAR(255) | | Hashed password |
| email | VARCHAR(255) | | User email |
| role | VARCHAR(50) | | ADMIN or TRADER |
| created_at | TIMESTAMP | DEFAULT NOW() | Account creation time |

**Entity 6: ORDERS**
Stores trade orders placed by the user (paper trading and live trading).

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| user_id | BIGINT | FK → USERS.id | Owning user |
| symbol | VARCHAR(20) | NOT NULL | Stock ticker |
| order_type | VARCHAR(10) | NOT NULL | BUY or SELL |
| quantity | INTEGER | NOT NULL | Number of shares |
| price | DECIMAL(15,4) | NOT NULL | Execution price |
| status | VARCHAR(20) | | PENDING / EXECUTED / CANCELLED |
| trading_mode | VARCHAR(20) | | PAPER or REAL |
| commission | DECIMAL(10,4) | | Brokerage fee |
| created_at | TIMESTAMP | DEFAULT NOW() | Order timestamp |

### 4.1.2 Relationships

| Relationship | Cardinality | Description |
|--------------|-------------|-------------|
| STOCKS → MARKET_DATA | One-to-Many | One stock has many OHLCV records |
| STOCKS → TRADE_SIGNALS | One-to-Many | One stock can have many strategy signal records |
| USERS → ORDERS | One-to-Many | One user can place many orders |
| USERS → UPSTOX_TOKENS | One-to-One | One active token per user session |

---

## 4.2 Data Flow Diagram (Level 0 & Level 1)

### 4.2.1 Level 0 — Context Diagram (DFD Level 0)

The Level 0 DFD shows the system as a single process box with all external entities and major data flows.

**External Entities:**
- **Trader (User)** — Interacts via the web dashboard
- **Upstox API** — Provides live ticks, historical data, and order execution
- **Amazon Bedrock (Claude AI)** — Processes AI insight requests
- **Apache Kafka** — Acts as the message broker for the event pipeline

**Central Process:** TradeIntel AI Platform

**Data Flows:**

```
Trader ──────────────► TradeIntel AI ◄──────────── Upstox API
(Strategy requests,    (Live ticks, OHLCV,
 Backtest config,      Auth tokens)
 AI analysis requests)

TradeIntel AI ──────────────────────────────────► Amazon Bedrock
(Market data context, strategy signals)          (AI insights response)

TradeIntel AI ◄────────────────────────────────► Apache Kafka
(Publish market-data-topic)                     (Consume for DB persistence)

TradeIntel AI ──────────────────────────────────► Trader
(Signals, Backtest results, AI insights, Live quotes, Dashboard)
```

### 4.2.2 Level 1 — Detailed DFD

The Level 1 DFD decomposes the central system process into its major functional sub-processes and shows how data flows between them.

**Sub-Processes:**

**P1 — Authentication Manager**
- Receives OAuth2 authorization request from Trader
- Communicates with Upstox API to exchange auth code for access token
- Stores token in upstox_tokens table (D1)
- Returns authentication status to Trader

**P2 — Live Market Data Streamer**
- Reads access token from D1
- Connects to Upstox WebSocket feed
- Receives real-time price ticks
- Writes OHLCV records to D2 (market_data table)
- Publishes tick events to Kafka (D3)
- Broadcasts to Trader's browser via WebSocket/STOMP

**P3 — Historical Data Manager**
- Receives fetch request from Trader (symbol + interval)
- Calls Upstox Historical Data REST API
- Transforms and stores OHLCV records in D2
- Returns confirmation to Trader

**P4 — Strategy Execution Engine**
- Receives strategy execution request from Trader (symbol + strategy name)
- Reads OHLCV records from D2
- Computes technical indicators
- Runs selected strategy logic
- Stores output in D4 (trade_signals table)
- Returns BUY/SELL/HOLD signal with confidence and reasoning to Trader

**P5 — AI Insights Processor**
- Receives AI analysis request from Trader (symbol + analysis type)
- Reads recent market data from D2 and signals from D4
- Constructs prompt and calls Amazon Bedrock Converse API
- Returns AI-generated insights to Trader

**P6 — Backtesting Engine**
- Receives backtest configuration from Trader (symbol, strategy, date range, capital)
- Reads historical OHLCV records from D2
- Simulates strategy execution over the date range
- Computes performance metrics
- Returns results to Trader

**P7 — Technical Indicator Calculator**
- Receives indicator request from Trader (symbol + indicator type + period)
- Reads OHLCV records from D2
- Computes indicator values (RSI, MACD, Bollinger, SMA, EMA)
- Returns computed values to Trader

**Data Stores:**
- **D1** — upstox_tokens (PostgreSQL)
- **D2** — market_data (PostgreSQL)
- **D3** — Kafka Topics (market-data-topic, signal-events-topic)
- **D4** — trade_signals (PostgreSQL)

---

## 4.3 Modules

The system is organized into the following major functional modules:

### Module 1: Authentication & Security Module

**Classes:** `UpstoxAuthService`, `UpstoxAuthController`, `SecurityConfig`

**Responsibility:** Manages the complete Upstox OAuth2 flow — generating the authorization URL, handling the callback with authorization code, exchanging it for an access token, persisting the token, and verifying authentication status. Spring Security HTTP Basic Auth protects all non-public endpoints.

### Module 2: Market Data Module

**Classes:** `UpstoxMarketDataStreamer`, `MarketDataController`, `MarketDataPersistenceService`, `LiveQuotePollerService`, `InstrumentService`

**Responsibility:** Establishes and maintains the Upstox WebSocket connection, receives and parses live price ticks, publishes them to Kafka, persists them to PostgreSQL, and broadcasts them to the browser dashboard. The Live Quote Poller provides a REST-API-based fallback polling mechanism. Instrument lookup and caching is handled by InstrumentService.

### Module 3: Strategy Execution Module

**Classes:** `TradingStrategy` (interface), `AbstractTradingStrategy` (base class), all 17 strategy implementations, `TradingStrategyService`, `StrategyExecutionController`, `StrategyScoreEngine`

**Responsibility:** Implements the complete library of technical trading strategies. Each strategy reads OHLCV data, computes its indicator logic, and returns a standardized TradeSignal object. The StrategyScoreEngine aggregates signals from multiple strategies into a consensus recommendation with weighted scoring.

### Module 4: AI Intelligence Module

**Classes:** `AIStrategyService`, `AIInsightsController`, `NewsFetcherService`, `BedrockConfig`

**Responsibility:** Integrates Amazon Bedrock (Claude) via Spring AI's Bedrock Converse client. Constructs structured prompts containing market data and strategy signals, calls the LLM API, and returns actionable insights for sentiment, patterns, regime, and comprehensive analysis.

### Module 5: Backtesting Module

**Classes:** `BacktestService`, `BacktestController`

**Responsibility:** Simulates a selected strategy's performance on historical OHLCV data over a configurable period. Tracks virtual portfolio value, calculates entry/exit points, and computes quantitative performance metrics including return percentage, win rate, Sharpe ratio, and maximum drawdown.

### Module 6: Technical Indicators Module

**Classes:** `TechnicalIndicatorService`, `TechnicalIndicatorController`

**Responsibility:** Provides stateless calculation of standard technical indicators from OHLCV data arrays. Acts as a shared utility used by all strategy implementations and exposed as a REST API for direct indicator queries from the dashboard.

### Module 7: Kafka Pipeline Module

**Classes:** `KafkaConfig`, `MarketDataPersistenceService` (consumer)

**Responsibility:** Defines Kafka topics and configures producer/consumer beans. The persistence service consumes market data events from the market-data-topic and writes them to the database, decoupling data ingestion from persistence.

### Module 8: WebSocket Module

**Classes:** `WebSocketConfig`, `MarketDataWebSocketHandler`

**Responsibility:** Configures the STOMP-over-WebSocket endpoint (`/ws`) and manages the `/topic/market-data` broadcast channel. Delivers live price updates to all connected browser clients in real time.

### Module 9: Web Dashboard Module

**Files:** `index.html`, `dashboard.html`, `app.js`, `styles.css`

**Responsibility:** Provides the complete user-facing interface served as static resources from Spring Boot. Implements all dashboard tabs: Live Quotes, Strategies, AI Insights, Backtesting, Technical Indicators, and Settings.

---

## 4.4 Database Schema

### Table 1: stocks

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| symbol | VARCHAR(20) | UNIQUE, NOT NULL | Stock ticker symbol |
| name | VARCHAR(255) | | Full company name |
| exchange | VARCHAR(10) | | NSE or BSE |
| instrument_key | VARCHAR(100) | | Upstox instrument ID |
| created_at | TIMESTAMP | DEFAULT NOW() | Record creation time |

**Indexes:** PRIMARY KEY (id), UNIQUE (symbol)

### Table 2: market_data

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| symbol | VARCHAR(20) | NOT NULL, INDEX | Stock ticker |
| timestamp | TIMESTAMP | NOT NULL, INDEX | Candle timestamp |
| open | DECIMAL(15,4) | | Opening price |
| high | DECIMAL(15,4) | | Highest price |
| low | DECIMAL(15,4) | | Lowest price |
| close | DECIMAL(15,4) | | Closing price |
| volume | BIGINT | | Trading volume |
| interval | VARCHAR(20) | | Candle interval |
| created_at | TIMESTAMP | DEFAULT NOW() | Insert time |

**Indexes:** PRIMARY KEY (id), INDEX (symbol), INDEX (timestamp), UNIQUE (symbol, timestamp, interval)

### Table 3: trade_signals

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| symbol | VARCHAR(20) | NOT NULL | Stock ticker |
| strategy_name | VARCHAR(100) | | Strategy that generated signal |
| signal | VARCHAR(10) | NOT NULL | BUY / SELL / HOLD |
| confidence | DECIMAL(5,4) | | 0.0000 to 1.0000 |
| reasoning | TEXT | | Human-readable explanation |
| target_price | DECIMAL(15,4) | | Target price |
| stop_loss | DECIMAL(15,4) | | Stop-loss price |
| entry_price | DECIMAL(15,4) | | Signal entry price |
| created_at | TIMESTAMP | DEFAULT NOW() | Signal generation time |

**Indexes:** PRIMARY KEY (id), INDEX (symbol), INDEX (created_at)

### Table 4: upstox_tokens

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| access_token | TEXT | NOT NULL | Upstox OAuth2 bearer token |
| token_type | VARCHAR(50) | | Bearer |
| expires_at | TIMESTAMP | | Token expiry time |
| created_at | TIMESTAMP | DEFAULT NOW() | Token storage time |

**Indexes:** PRIMARY KEY (id)

### Table 5: users

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| username | VARCHAR(100) | UNIQUE | Login username |
| password_hash | VARCHAR(255) | | Hashed password |
| email | VARCHAR(255) | | User email |
| role | VARCHAR(50) | | ADMIN or TRADER |
| created_at | TIMESTAMP | DEFAULT NOW() | Account creation time |

**Indexes:** PRIMARY KEY (id), UNIQUE (username)

### Table 6: orders

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| user_id | BIGINT | FK → users.id | Owning user |
| symbol | VARCHAR(20) | NOT NULL | Stock ticker |
| order_type | VARCHAR(10) | NOT NULL | BUY or SELL |
| quantity | INTEGER | NOT NULL | Number of shares |
| price | DECIMAL(15,4) | NOT NULL | Execution price |
| status | VARCHAR(20) | | PENDING / EXECUTED / CANCELLED |
| trading_mode | VARCHAR(20) | | PAPER or REAL |
| commission | DECIMAL(10,4) | | Brokerage fee |
| created_at | TIMESTAMP | DEFAULT NOW() | Order timestamp |

**Indexes:** PRIMARY KEY (id), FK (user_id), INDEX (symbol, created_at)

---

## 4.5 Input-Output Forms (Screen Layout)

### Screen 1: Dashboard — Live Quotes Tab

**Input:**
- Symbol name (text field)
- "Subscribe" button

**Output:**
- Real-time price cards showing Symbol, LTP (Last Traded Price), Change (%), Volume
- Live status indicator
- Updates every ~1 second via WebSocket

**Design:** Grid layout with multiple price cards. Each card displays live data with color-coded changes (green for up, red for down).

### Screen 2: Strategy Execution Tab

**Input:**
- Symbol (text field)
- Strategy selection (dropdown with 17 strategies)
- Average Buy Price (optional number field)
- "Run Strategy" button
- "Run All Strategies" button

**Output:**
- Signal result card showing:
  - Signal (BUY/SELL/HOLD with color coding)
  - Confidence (%)
  - Reasoning (multi-line text)
  - Target Price
  - Stop Loss
  - Entry Price
- For multi-strategy: individual results for each strategy + consensus recommendation

**Design:** Tabbed interface with dropdown selector and result cards. Multi-strategy results displayed in collapsible accordion.

### Screen 3: AI Insights Tab

**Input:**
- Symbol (text field)
- Analysis Type (dropdown: Comprehensive / Sentiment / Pattern / Regime)
- "Analyze" button

**Output:**
- AI-generated narrative text displayed in formatted result card
- Response includes:
  - Market conditions summary
  - Key observations
  - Trading recommendation with rationale
  - Risk factors

**Design:** Single-column layout with markdown-formatted AI response displayed in a card.

### Screen 4: Backtesting Tab

**Input:**
- Symbol (text field)
- Strategy (dropdown)
- Start Date (date picker)
- End Date (date picker)
- Initial Capital (number field, default: ₹1,00,000)
- "Run Backtest" button

**Output:**
- Performance metrics table showing:
  - Total Return (%)
  - Win Rate (%)
  - Total Trades
  - Sharpe Ratio
  - Maximum Drawdown (%)
  - Profit Factor
- Trade-by-trade log with entry/exit details

**Design:** Form at top, metrics table below, collapsible trade history log at bottom.

### Screen 5: Technical Indicators Tab

**Input:**
- Symbol (text field)
- Indicator type (dropdown: RSI/MACD/Bollinger/SMA/EMA)
- Period (number field, default varies by indicator)
- "Calculate" button

**Output:**
- Table of computed indicator values with timestamps
- Color-coded current reading (overbought/oversold for RSI, above/below signal for MACD, etc.)
- Optional chart visualization

**Design:** Form and table layout with clear labeling and color-coded values.

### Screen 6: Settings Tab

**Input:**
- "Get Auth URL" button (initiates Upstox OAuth2)
- "Check Status" button
- Trading mode selector (Paper Trading / Real Money toggle)

**Output:**
- Authentication status badge (Authenticated / Not Authenticated)
- Connected Upstox account details
- Token expiry time
- Trading mode indicator
- Available portfolio balance (for paper trading)

**Design:** Status cards with clear badges and toggle switches for trading mode selection.

---

## 4.6 API Endpoint Summary

| Endpoint | Method | Description | Input | Output |
|----------|--------|-------------|-------|--------|
| `/market-data/historical` | GET | Fetch historical OHLCV data | symbol, interval, startDate, endDate | Array of OHLCV records |
| `/market-data/live` | GET | Get latest live quote | symbol | Current price, LTP, change |
| `/execute/strategy` | POST | Run single strategy | symbol, strategyName | TradeSignal object |
| `/execute/multiple` | POST | Run all strategies | symbol | Array of signals + consensus |
| `/execute/available-strategies` | GET | List all available strategies | - | Array of strategy names |
| `/backtest` | POST | Run backtesting | symbol, strategy, dateRange, capital | BacktestResult object |
| `/indicators/rsi` | GET | Calculate RSI | symbol, period | RSI values array |
| `/indicators/macd` | GET | Calculate MACD | symbol | MACD, signal, histogram arrays |
| `/ai/analyze` | POST | Get AI insights | symbol, analysisType | AI response text |
| `/upstox/auth-url` | GET | Get OAuth2 URL | - | Authorization URL |
| `/upstox/callback` | GET | OAuth2 callback handler | code, state | Redirect to dashboard |
| `/upstox/status` | GET | Check auth status | - | Authentication status object |
| `/actuator/health` | GET | Application health check | - | Health status |

---

**End of Chapter 4 — Design**
