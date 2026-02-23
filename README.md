<div align="center">

<img src="src/main/resources/static/favicon.png" alt="TradeIntel AI" width="90" style="border-radius:18px;" />

# TradeIntel AI

**Intelligent Trading Platform — Smart Trading · AI Insights · Strategy Backtesting**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4-412991?logo=openai)](https://openai.com/)
[![Upstox](https://img.shields.io/badge/Broker-Upstox-purple)](https://upstox.com/)

</div>

---

## Overview

TradeIntel AI is a full-stack intelligent trading platform that connects to the **Upstox** brokerage API to stream live NSE/BSE market data, execute technical trading strategies, run historical backtests, and provide AI-generated insights powered by OpenAI GPT models. It features a real-time web dashboard served directly from the Spring Boot server.

### Key Capabilities

| Feature | Description |
|---|---|
| 📡 **Live Market Data** | WebSocket streaming of real-time price ticks via Upstox API |
| 🎯 **8 Trading Strategies** | RSI, MACD, Bollinger Bands, MA Crossover, Stochastic, Volume Breakout, Support/Resistance, News Sentiment |
| 🤖 **AI Analysis** | OpenAI-powered market sentiment, pattern detection, regime detection & multi-strategy synthesis |
| 📈 **Backtesting Engine** | Test any strategy on up to 1 year of historical OHLCV data |
| 🔐 **OAuth2 Authentication** | Full Upstox OAuth2 flow with secure token management |
| 📊 **Technical Indicators** | RSI, MACD, Bollinger Bands, Stochastic, ATR, OBV, and more via REST API |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4 |
| **AI** | Spring AI 1.0, OpenAI GPT |
| **Broker API** | Upstox Java SDK v1.19 |
| **Database** | PostgreSQL 16 + Flyway migrations |
| **Messaging** | Apache Kafka (market-data pipeline) |
| **Real-time** | Spring WebSocket + STOMP |
| **Security** | Spring Security (HTTP Basic) |
| **Frontend** | Vanilla HTML/CSS/JS |
| **Build** | Maven, Spring Boot Maven Plugin |

---

## Prerequisites

Before running, make sure you have the following installed and configured:

- **Java 21+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/)
- **PostgreSQL 16+** — running locally on port `5432`
- **Apache Kafka** — running locally on port `9092`
- **Upstox Developer Account** — [Register](https://developer.upstox.com/)
- **OpenAI API Key** — [Get one](https://platform.openai.com/)

---

## Configuration

### 1. Database Setup

Create the PostgreSQL database:

```sql
CREATE DATABASE trading_platform;
CREATE USER lakshay WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE trading_platform TO lakshay;
```

Flyway will automatically run all migrations from `src/main/resources/db/migration/` on startup.

### 2. Kafka Setup

Start Kafka locally (default port `9092`). The app will auto-create these topics on startup:

| Topic | Partitions | Purpose |
|---|---|---|
| `market-data-topic` | 3 | Live price tick streaming |
| `order-events-topic` | 3 | Order lifecycle events (future) |
| `signal-events-topic` | 3 | Trade signal events (future) |
| `portfolio-updates-topic` | 3 | Portfolio change events (future) |

### 3. Environment Variables

Copy `.env.example` to `.env` and fill in your credentials:

```bash
cp .env.example .env
```

```dotenv
# Upstox API
UPSTOX_API_KEY=your_upstox_api_key
UPSTOX_API_SECRET=your_upstox_api_secret
UPSTOX_REDIRECT_URL=http://localhost:8080/api/upstox/callback

# OpenAI
OPENAI_API_KEY=sk-...

# App Security (Basic Auth for the admin API)
SECURITY_USERNAME=admin
SECURITY_PASSWORD=admin123

# Trading Mode: PAPER_TRADING or REAL_MONEY
TRADING_MODE=PAPER_TRADING

# Server
SERVER_PORT=8080
```

> **⚠️ Warning:** Never commit `.env` or `application.yml` with real credentials to version control. These files are listed in `.gitignore`.

---

## Running the Application

```bash
# Clone the repository
git clone <repo-url>
cd tradeintel_ai

# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

The application starts at **http://localhost:8080/api** (context path is `/api`).

Open the web dashboard at: **http://localhost:8080/api/index.html**

---

## Upstox Authentication Flow

The platform uses Upstox OAuth2. Follow these steps after starting the app:

1. Go to the **Settings** tab in the dashboard
2. Click **"Get Auth URL"** — opens Upstox login in a new tab
3. Log in with your Upstox credentials and authorize the app
4. You'll be redirected back to the app with the access token automatically stored
5. The header badge will show **"Upstox — Authenticated"**

> The token is stored in the database and reused across sessions until it expires (daily rotation required).

---

## Project Structure

```
tradeintel_ai/
├── src/main/java/com/tradeintel/ai/
│   ├── TradeIntelAiApplication.java      # Spring Boot entry point
│   │
│   ├── broker/upstox/                    # Upstox integration layer
│   │   ├── UpstoxAuthService.java        # OAuth2 token management
│   │   └── UpstoxMarketDataStreamer.java # WebSocket → Kafka + DB pipeline
│   │
│   ├── config/                           # Spring configuration beans
│   │   ├── KafkaConfig.java              # Kafka topic definitions
│   │   ├── SecurityConfig.java           # HTTP Basic + CORS/CSRF config
│   │   ├── WebSocketConfig.java          # STOMP WebSocket endpoint
│   │   └── ...
│   │
│   ├── controller/                       # REST API controllers
│   │   ├── AIInsightsController.java     # /ai/* — AI analysis endpoints
│   │   ├── BacktestController.java       # /backtest — backtesting engine
│   │   ├── MarketDataController.java     # /market-data/* — quotes & search
│   │   ├── StrategyExecutionController.java  # /execute/* — run strategies
│   │   ├── TechnicalIndicatorController.java # /indicators/* — raw indicators
│   │   ├── UpstoxAuthController.java     # /upstox/* — auth flow
│   │   └── ...
│   │
│   ├── strategy/                         # Trading strategy implementations
│   │   ├── TradingStrategy.java          # Interface
│   │   ├── AbstractTradingStrategy.java  # Base class with indicator helpers
│   │   ├── RSIStrategy.java
│   │   ├── MACDStrategy.java
│   │   ├── BollingerBandsStrategy.java
│   │   ├── MovingAverageCrossoverStrategy.java
│   │   ├── StochasticStrategy.java
│   │   ├── VolumeBreakoutStrategy.java
│   │   ├── SupportResistanceStrategy.java
│   │   └── NewsSentimentStrategy.java    # AI + news-driven strategy
│   │
│   ├── service/                          # Business logic services
│   │   ├── AIStrategyService.java        # OpenAI GPT integration
│   │   ├── TradingStrategyService.java   # Strategy dispatcher
│   │   ├── MarketDataPersistenceService.java  # Kafka consumer → DB
│   │   ├── InstrumentService.java        # Instrument key lookup & cache
│   │   ├── NewsFetcherService.java       # News headline fetcher (for AI)
│   │   └── ...
│   │
│   ├── model/                            # JPA entity models
│   │   ├── Stock.java
│   │   ├── MarketData.java               # OHLCV + timestamp
│   │   ├── TradeSignal.java              # Signal output (BUY/SELL/HOLD)
│   │   └── ...
│   │
│   └── repository/                       # Spring Data JPA repositories
│
├── src/main/resources/
│   ├── application.yml                   # All configuration (use env vars for secrets)
│   ├── db/migration/                     # Flyway SQL migration scripts
│   │   ├── V1__initial_schema.sql
│   │   └── V2__strategy_enhancements.sql
│   └── static/                           # Frontend (served by Spring Boot)
│       ├── index.html                    # Main trading platform UI
│       ├── dashboard.html                # Real-time live price dashboard
│       ├── app.js                        # Frontend JavaScript
│       ├── styles.css                    # Stylesheet
│       ├── favicon.png                   # App icon
│       └── logo.png                      # Banner logo
│
├── .env                                  # Your local secrets (git-ignored)
├── .env.example                          # Template for .env
├── pom.xml                               # Maven build configuration
└── logs/                                 # Application log files
```

---

## REST API Reference

All endpoints are prefixed with `/api` (the server context path). HTTP Basic Auth is required for most endpoints (`admin:admin123` by default).

### Market Data

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/market-data/quote?symbol=TATAPOWER` | Get latest quote for a symbol |
| `GET` | `/market-data/search?query=TATA` | Autocomplete instrument search |
| `POST` | `/market-data/fetch-historical?symbol=TATAPOWER&interval=day` | Fetch and store 1 year of historical OHLCV data from Upstox |
| `GET` | `/market-data/historical?symbol=TATAPOWER` | Retrieve stored historical data |

**Intervals:** `1minute`, `30minute`, `day` (recommended), `week`, `month`

---

### Strategy Execution

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/execute/available-strategies` | List all available strategies |
| `POST` | `/execute/strategy` | Run a single strategy on a symbol |
| `POST` | `/execute/multiple` | Run all strategies and get consensus |

**Single Strategy Request:**
```json
{
  "strategyName": "RSIStrategy",
  "symbol": "TATAPOWER"
}
```

**Multiple Strategy Request:**
```json
{
  "strategyNames": ["RSIStrategy", "MACDStrategy", "bollingerBandsStrategy"],
  "symbol": "TATAPOWER",
  "avgBuyPrice": 350.50
}
```

---

### AI Insights

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/ai/analyze` | Comprehensive AI analysis (sentiment + signals) |
| `POST` | `/ai/sentiment` | Market sentiment only |
| `POST` | `/ai/patterns` | Chart pattern detection |
| `POST` | `/ai/regime` | Market regime classification (bull/bear/sideways) |

**Request body:**
```json
{
  "symbol": "TATAPOWER",
  "analysisType": "comprehensive"
}
```
**Analysis types:** `comprehensive`, `sentiment`, `pattern`, `regime`

---

### Backtesting

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/backtest` | Run backtest for a strategy on historical data |

**Request body:**
```json
{
  "strategyName": "RSIStrategy",
  "symbol": "TATAPOWER",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T00:00:00Z",
  "initialCapital": 100000
}
```

**Response includes:** Total Return %, Win Rate, Total Trades, Sharpe Ratio, Max Drawdown, Profit Factor.

---

### Technical Indicators

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/indicators/rsi?symbol=TATAPOWER&period=14` | RSI values |
| `GET` | `/indicators/macd?symbol=TATAPOWER` | MACD line, signal, histogram |
| `GET` | `/indicators/bollinger?symbol=TATAPOWER` | Bollinger Bands upper/middle/lower |
| `GET` | `/indicators/sma?symbol=TATAPOWER&period=20` | Simple Moving Average |
| `GET` | `/indicators/ema?symbol=TATAPOWER&period=20` | Exponential Moving Average |

---

### Upstox Authentication

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/upstox/auth-url` | Get the OAuth2 login URL |
| `GET` | `/upstox/callback?code=...` | OAuth2 callback (auto-handled) |
| `GET` | `/upstox/status` | Check authentication status |

---

### Health & Monitoring

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Application health check |
| `GET` | `/actuator/metrics` | Spring Actuator metrics |
| `GET` | `/actuator/prometheus` | Prometheus metrics endpoint |

---

## Trading Strategies

| Strategy | Description | Key Parameters |
|---|---|---|
| **RSI** | Momentum-based overbought (>70) / oversold (<30) signals | Period: 14 |
| **MACD** | Trend-following crossover using fast/slow EMAs | Fast: 12, Slow: 26, Signal: 9 |
| **Bollinger Bands** | Volatility-based mean reversion at band boundaries | Period: 20, StdDev: 2 |
| **MA Crossover** | Golden/Death cross — short MA vs long MA trend | Short: 9, Long: 21 |
| **Stochastic** | Short-term reversal using %K and %D oscillators | K: 14, D: 3 |
| **Volume Breakout** | High-volume price moves above resistance | Volume multiplier: 1.5× avg |
| **Support/Resistance** | Key price level detection and bounce/breakout signals | Lookback: 50 bars |
| **News Sentiment** | AI-powered news headline analysis + trend confirmation | OpenAI + news API |

Every strategy returns:
- **Signal:** `BUY` / `SELL` / `HOLD`
- **Confidence:** 0.0 – 1.0
- **Reasoning:** Human-readable explanation
- **Target Price** and **Stop Loss** (where applicable)

---

## Real-Time Data Flow

```
Upstox WebSocket (live ticks)
         │
         ▼
UpstoxMarketDataStreamer
         │
         ├──► PostgreSQL (direct save)
         │
         ├──► Kafka "market-data-topic"
         │         └──► MarketDataPersistenceService (Kafka consumer)
         │
         └──► WebSocket "/topic/market-data" → Browser Dashboard
```

---

## Risk Management Configuration

Configured in `application.yml` under `trading.risk-management`:

| Parameter | Default | Description |
|---|---|---|
| `max-position-size-percent` | 10% | Max portfolio allocation per position |
| `max-portfolio-risk-percent` | 2% | Max risk per trade |
| `max-daily-loss-percent` | 5% | Daily loss circuit breaker |
| `max-drawdown-percent` | 15% | Drawdown alert threshold |
| `stop-loss-percent` | 2% | Default stop loss |
| `take-profit-percent` | 5% | Default take profit |

> **Paper Trading mode** starts with ₹10,00,000 (10 Lakhs) and ₹20 commission per trade.

---

## Logs

Application logs are written to `logs/trading-platform.log` and the console.

```bash
tail -f logs/trading-platform.log
```

---

## Known Limitations & Notes

- **Kafka double-write:** Live market ticks are currently saved to the DB both directly and via the Kafka consumer — this causes duplicate OHLCV records per tick. The Kafka pipeline is intended for future scale-out.
- **Daily token rotation:** Upstox access tokens expire daily. Re-authenticate via the Settings tab each trading day.
- **Symbol case-insensitivity:** All symbol inputs (e.g., `tatapower`, `TataPower`) are normalized to uppercase before DB lookups and API calls.
- **Historical data required for strategies:** Run "Fetch Historical Data" in the Strategies tab before executing any strategy or backtest on a symbol.
