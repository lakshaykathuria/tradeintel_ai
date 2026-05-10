# CHAPTER 2 — FEASIBILITY STUDY

A feasibility study is a critical preliminary analysis conducted before the commencement of any software development project. It evaluates whether the proposed system is practically achievable, financially viable, technically sound, legally compliant, and schedulable within the given constraints. The feasibility study for TradeIntel AI was conducted across five dimensions: Technical, Operational, Economic, Legal, and Schedule feasibility.

## 2.1 Technical Feasibility

Technical feasibility examines whether the required hardware, software, network infrastructure, and technical expertise exist to build and operate the proposed system.

### 2.1.1 Hardware Requirements

The platform has been designed to run on standard developer-grade hardware without the need for specialized financial computing infrastructure. The minimum and recommended hardware configurations are as follows:

| Component | Minimum Requirement | Recommended |
|-----------|-------------------|-------------|
| Processor | Intel Core i5 (4 cores) | Intel Core i7/i9 or AMD Ryzen 7 |
| RAM | 8 GB | 16 GB or more |
| Storage | 50 GB SSD | 256 GB SSD |
| Network | 10 Mbps broadband | 100 Mbps or higher |
| Operating System | Windows 10 / Ubuntu 20.04 | Ubuntu 22.04 LTS |

All the core infrastructure components — Java 21 JDK, Apache Kafka, PostgreSQL 16, and Maven — are freely available and run on commodity hardware. The application server itself requires no GPU or specialized financial hardware.

### 2.1.2 Software and Technology Availability

All technologies employed in this project are either open-source or available under developer-friendly pricing:

- **Java 21 (OpenJDK)** — Free, open-source, with long-term support (LTS). Widely used in enterprise backend development.
- **Spring Boot 3.4** — Open-source, the industry-standard framework for Java microservices and web applications.
- **Spring AI 1.0** — Open-source Spring integration layer for AI/LLM providers including Amazon Bedrock.
- **Apache Kafka** — Open-source distributed event streaming platform maintained by the Apache Software Foundation.
- **PostgreSQL 16** — Open-source, production-grade relational database system.
- **Flyway** — Open-source database migration tool; integrates natively with Spring Boot.
- **Upstox Java SDK v1.19** — Free for registered Upstox Developer accounts; provides complete API access for NSE/BSE data and order management.
- **Amazon Bedrock (Claude)** — AWS managed AI service; pay-per-use pricing with a free tier available for development and testing.

All required technologies are mature, well-documented, and actively maintained by large communities. Their integration through Spring Boot's ecosystem is well-established, and extensive documentation, tutorials, and community support are available.

### 2.1.3 Technical Expertise

The development of TradeIntel AI requires proficiency in the following technical areas:

- Java programming and object-oriented design
- Spring Boot, Spring Data JPA, Spring Security, Spring WebSocket
- REST API design and HTTP protocol
- Apache Kafka producer/consumer configuration
- PostgreSQL database design and SQL query optimization
- OAuth2 authentication flow implementation
- Amazon AWS SDK and Bedrock Converse API integration
- Frontend development with HTML, CSS, and JavaScript
- Maven build management and dependency resolution

All of the above skills fall within the standard curriculum of an MCA program and have been applied in this project. The technical feasibility is therefore confirmed — the project is implementable with existing knowledge and freely available tools.

## 2.2 Operational Feasibility

Operational feasibility assesses whether the proposed system will be accepted and effectively used by its intended users once deployed.

### 2.2.1 Target User Group

TradeIntel AI is designed for:

- Retail equity traders active on NSE/BSE who seek algorithmic assistance.
- MCA/Computer Science students interested in learning algorithmic trading and AI integration.
- Quant finance enthusiasts who want to backtest and refine their own trading strategies.

### 2.2.2 Ease of Use

The system provides a Vanilla HTML/CSS/JS web dashboard served directly from the Spring Boot backend — requiring no additional frontend framework installation. Users interact through an intuitive tabbed interface with clearly labeled sections for Live Quotes, Strategies, AI Insights, Backtesting, Technical Indicators, and Settings. All outputs are presented in plain-English text with confidence percentages and signal labels (BUY/SELL/HOLD) that are easily understood by non-technical traders.

### 2.2.3 Operational Environment

The platform is designed to operate in both a local development environment (for personal use and academic submission) and a cloud-deployed production environment (e.g., AWS EC2 or any VPS). In its current form, it runs as a standalone Spring Boot application accessible via a browser at `http://localhost:8080/api/index.html`. No special operator training is required beyond standard web browser usage.

### 2.2.4 System Reliability

The platform incorporates several reliability mechanisms:

- Spring Boot Actuator endpoints for real-time health monitoring (`/actuator/health`).
- Kafka's built-in fault tolerance with configured producer retries (retries: 3) and consumer group management.
- Flyway migration versioning ensures database schema consistency across deployments.
- Structured logging to `logs/trading-platform.log` with configurable log levels per package.

The system is therefore operationally feasible for both academic and production use cases.

## 2.3 Economic Feasibility

Economic feasibility evaluates the cost-benefit balance of the project. For an academic MCA project, costs are primarily limited to API access fees and infrastructure.

### 2.3.1 Development Costs

| Cost Item | Description | Estimated Cost |
|-----------|-------------|-----------------|
| Java 21 JDK | Open-source (OpenJDK) | ₹0 |
| Spring Boot / Spring AI | Open-source | ₹0 |
| Apache Kafka | Open-source | ₹0 |
| PostgreSQL 16 | Open-source | ₹0 |
| Upstox Developer API | Free for registered developers | ₹0 |
| Amazon Bedrock (Claude) | Pay-per-use; ~$0.003/1K tokens | ~₹50–200/month (dev usage) |
| Development Hardware | Personal laptop/desktop | Already available |
| Domain & Hosting | Not required for academic submission | ₹0 |
| **Total Estimated Cost** | | **< ₹200/month** |

### 2.3.2 Benefits

- Provides retail traders with institutional-grade analytical tools at near-zero cost.
- Eliminates the need for expensive subscription-based trading signal services (which cost ₹2,000–₹10,000/month).
- Paper trading mode prevents financial loss during the learning phase.
- The backtesting engine provides data-driven validation of strategies, potentially saving traders from costly trial-and-error in live markets.

Given the negligible development cost and the significant operational benefits, the project is economically feasible.

## 2.4 Legal Feasibility

Legal feasibility examines whether the proposed system complies with applicable laws, regulations, and terms of service.

### 2.4.1 Regulatory Compliance

**SEBI Regulations** — The platform is built for analysis and signal generation only and does not execute trades autonomously. All trade execution remains under the direct control of the user. This design ensures compliance with SEBI's framework for algorithmic trading, which requires exchange approval only for fully automated order-routing systems.

**Upstox API Terms of Service** — The Upstox Developer API is used strictly within the permitted scope: market data retrieval, historical data access, and OAuth2-authenticated user operations. No scraping, rate-limit violation, or unauthorized data redistribution is performed.

**Amazon Bedrock Terms** — The platform uses Amazon Bedrock's Converse API in compliance with AWS's acceptable use policy. No sensitive user financial data is transmitted to the AI model; only aggregated market data and strategy signals are used as context.

**Data Privacy** — The platform stores only market data and user-configured API keys (encrypted via Spring Security). No personally identifiable information (PII) of third parties is collected or stored.

The project is fully legally feasible and does not violate any applicable law or terms of service.

## 2.5 Schedule Feasibility

Schedule feasibility assesses whether the project can be completed within the available academic timeframe.

### 2.5.1 Development Timeline

| Phase | Activities | Duration |
|-------|-----------|----------|
| Phase 1 — Planning | Requirements gathering, technology selection, system design | Week 1–2 |
| Phase 2 — Backend Foundation | Spring Boot setup, database design, Flyway migrations, Kafka config | Week 3–4 |
| Phase 3 — Market Data Integration | Upstox OAuth2, WebSocket streamer, Kafka pipeline, live quote polling | Week 5–6 |
| Phase 4 — Strategy Implementation | 17 trading strategies, TechnicalIndicatorService, StrategyScoreEngine | Week 7–9 |
| Phase 5 — AI Integration | Amazon Bedrock, Spring AI, AIStrategyService, sentiment/pattern/regime APIs | Week 10–11 |
| Phase 6 — Backtesting Engine | BacktestService, BacktestController, performance metrics calculation | Week 12 |
| Phase 7 — Frontend Dashboard | HTML/CSS/JS dashboard, WebSocket live updates, all tabs and forms | Week 13–14 |
| Phase 8 — Testing | JUnit unit tests, integration tests, backtesting validation | Week 15 |
| Phase 9 — Documentation | Project report, API documentation, README | Week 16 |
| | **Total Duration: ~16 weeks (4 months)** | |

This timeline is well within the standard MCA project submission window. The project has been completed successfully, confirming that it was schedulable and is schedule feasible.

## 2.6 Feasibility Summary

| Feasibility Dimension | Verdict | Key Reason |
|----------------------|---------|-----------|
| Technical | ✅ Feasible | All tools open-source; standard hardware sufficient |
| Operational | ✅ Feasible | Intuitive web dashboard; no special user training required |
| Economic | ✅ Feasible | Near-zero cost; significant financial benefits for users |
| Legal | ✅ Feasible | Signal-only platform; compliant with SEBI, Upstox, AWS ToS |
| Schedule | ✅ Feasible | Completed within 16-week academic timeline |

The feasibility study conclusively establishes that TradeIntel AI is a viable, practical, and achievable project across all five dimensions of analysis.

---

# CHAPTER 3 — SOFTWARE REQUIREMENT SPECIFICATIONS (SRS)

## 3.1 Introduction

### 3.1.1 Purpose of the Document

This Software Requirement Specification (SRS) document provides a complete and detailed description of the functional and non-functional requirements of the TradeIntel AI — Intelligent Trading Platform. It serves as the primary reference for the design, development, testing, and evaluation of the system. The document is intended for use by the developer, project guide, and evaluators to understand the scope, capabilities, and constraints of the system.

### 3.1.2 Scope of the System

TradeIntel AI is a server-side Java application built on the Spring Boot framework. It integrates with the Upstox brokerage API to acquire real-time and historical market data for Indian equity markets (NSE/BSE). The system provides a suite of algorithmic trading tools — strategy execution, backtesting, technical indicator calculation, and AI-driven insights — accessible through a REST API and a real-time web dashboard. The platform supports two trading modes: Paper Trading (simulated, risk-free) and Real Money (live market interaction).

The system explicitly does not include:

- Autonomous order routing or fully automated trade execution without user intervention.
- Support for derivatives (futures & options), commodities, or currency markets in the current version.
- Mobile applications (Android/iOS).
- Multi-user account management with separate portfolios per user.

### 3.1.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|------|-----------|
| NSE | National Stock Exchange of India |
| BSE | Bombay Stock Exchange |
| OHLCV | Open, High, Low, Close, Volume — standard candlestick data format |
| RSI | Relative Strength Index — momentum oscillator |
| MACD | Moving Average Convergence Divergence — trend indicator |
| SMA | Simple Moving Average |
| EMA | Exponential Moving Average |
| VWAP | Volume Weighted Average Price |
| ADX | Average Directional Index — trend strength indicator |
| ATR | Average True Range — volatility measure |
| API | Application Programming Interface |
| REST | Representational State Transfer — architectural style for web APIs |
| JWT | JSON Web Token |
| OAuth2 | Open Authorization 2.0 — authorization framework |
| JPA | Java Persistence API |
| ORM | Object-Relational Mapping |
| STOMP | Simple Text Oriented Messaging Protocol (used for WebSocket) |
| LLM | Large Language Model |
| BUY/SELL/HOLD | Standardized trading signal outputs |

### 3.1.4 Overview

The SRS is organized as follows:

- Section 3.1 provides context, scope, and definitions.
- Section 3.2 covers technology selection and specific functional/non-functional requirements.

## 3.2 Selection of Technology / Specific Requirements

### 3.2.1 Functional Requirements

Functional requirements describe the specific behaviors and capabilities the system must provide.

#### FR-01: User Authentication

- The system shall implement Upstox OAuth2 authorization flow, allowing users to authenticate their Upstox brokerage account via a browser-based login.
- The system shall securely store the access token in the PostgreSQL database for reuse across sessions.
- The system shall protect all REST API endpoints using Spring Security HTTP Basic Authentication.
- The system shall expose an endpoint to check the current Upstox authentication status.

#### FR-02: Live Market Data Streaming

- The system shall connect to the Upstox WebSocket feed to receive real-time price ticks for subscribed instruments.
- The system shall parse incoming price ticks and persist them as MarketData records in the PostgreSQL database.
- The system shall publish each received tick to the Kafka topic `market-data-topic`.
- The system shall broadcast live price updates to all connected browser clients via the WebSocket topic `/topic/market-data` using the STOMP protocol.
- The system shall support a Live Quote Poller as a fallback mechanism that polls the Upstox REST API at configurable intervals (default: 1000 ms) when the WebSocket stream is unavailable.

#### FR-03: Historical Data Management

- The system shall allow users to trigger a historical data fetch for any NSE/BSE symbol using the Upstox historical data API.
- Supported intervals shall include: 1minute, 30minute, day, week, and month.
- The system shall store fetched OHLCV records in the `market_data` table with symbol, timestamp, open, high, low, close, and volume fields.
- The system shall allow retrieval of stored historical data for any symbol via a REST endpoint.

#### FR-04: Trading Strategy Execution

The system shall implement the following seventeen trading strategies:

1. RSI Strategy (Period: 14, Overbought: 70, Oversold: 30)
2. MACD Strategy (Fast: 12, Slow: 26, Signal: 9)
3. Bollinger Bands Strategy (Period: 20, Std Dev: 2)
4. Moving Average Crossover Strategy (Short: 9, Long: 21)
5. Stochastic Oscillator Strategy (%K: 14, %D: 3)
6. Volume Breakout Strategy (Volume multiplier: 1.5× average)
7. Support/Resistance Strategy (Lookback: 50 bars)
8. Supertrend Strategy (Period: 10, Multiplier: 3)
9. VWAP Strategy
10. ADX Trend Strategy (Period: 14, Threshold: 25)
11. ATR Volatility Strategy
12. Donchian Channel Breakout Strategy
13. Mean Reversion Strategy
14. Gap Trading Strategy
15. News Sentiment Strategy (AI-powered via Amazon Bedrock)
16. Strategy Score Engine (multi-strategy consensus)

Each strategy shall return:

- Signal: BUY, SELL, or HOLD
- Confidence score: 0.0 to 1.0
- Human-readable reasoning text
- Target price and stop-loss price (where applicable)

The system shall support execution of a single strategy on a given symbol.

The system shall support simultaneous execution of multiple strategies on a given symbol and return an aggregated consensus signal.

#### FR-05: AI-Powered Insights

- The system shall integrate with Amazon Bedrock (Claude model) via the Spring AI Bedrock Converse API.
- The system shall support the following AI analysis types:
  - Comprehensive Analysis — Full sentiment, pattern, regime, and multi-strategy synthesis.
  - Market Sentiment — Bull/bear sentiment classification based on recent price and news data.
  - Chart Pattern Detection — Identification of candlestick and chart patterns (e.g., head and shoulders, double bottom).
  - Market Regime Classification — Categorization of current market as bull, bear, or sideways.
- AI responses shall be plain-English text suitable for display to non-technical users.

#### FR-06: Backtesting Engine

- The system shall allow users to backtest any implemented strategy on historical data for a configurable date range and initial capital.
- The system shall calculate and return the following performance metrics:
  - Total Return (%)
  - Win Rate (%)
  - Total Number of Trades
  - Sharpe Ratio
  - Maximum Drawdown (%)
  - Profit Factor

#### FR-07: Technical Indicators

- The system shall expose REST API endpoints to calculate and return the following technical indicators for any symbol:
  - RSI (configurable period)
  - MACD (line, signal, histogram)
  - Bollinger Bands (upper, middle, lower)
  - Simple Moving Average (configurable period)
  - Exponential Moving Average (configurable period)

#### FR-08: Risk Management

- The system shall enforce the following risk management rules:
  - Maximum position size: 10% of portfolio per trade
  - Maximum portfolio risk per trade: 2%
  - Daily loss circuit breaker: stop trading if daily loss exceeds 5%
  - Maximum drawdown alert: trigger alert at 15% drawdown
  - Default stop-loss: 2% below entry price
  - Default take-profit: 5% above entry price

#### FR-09: Paper Trading Mode

- The system shall support a Paper Trading mode with a configurable virtual capital (default: ₹10,00,000).
- Paper trades shall incur a simulated commission of ₹20 per trade.
- Paper trading results shall be tracked and reported separately from live trading.

### 3.2.2 Non-Functional Requirements

Non-functional requirements describe the quality attributes, performance standards, and constraints of the system.

#### NFR-01: Performance

- The system shall process and persist incoming live market ticks within 500 milliseconds of receipt.
- REST API endpoints shall respond within 2 seconds under normal load for all non-AI operations.
- AI insight generation via Amazon Bedrock shall complete within 15 seconds under typical network conditions.
- The WebSocket dashboard shall reflect live price updates with a maximum latency of 1 second end-to-end.

#### NFR-02: Reliability

- The system shall maintain 99% uptime during Indian market trading hours (9:15 AM – 3:30 PM IST) on trading days.
- The Kafka consumer shall automatically recover from transient broker connectivity issues using Spring Kafka's built-in retry mechanism.
- The system shall log all errors and exceptions to a rolling log file (`logs/trading-platform.log`) for post-incident analysis.

#### NFR-03: Scalability

- The Kafka-based event pipeline shall be architected to support horizontal scaling of consumer instances without code changes.
- The `market-data-topic` shall be configured with 3 partitions to support parallel consumption.
- The PostgreSQL database schema shall support indexing on symbol and timestamp columns to ensure fast query performance even with millions of OHLCV records.

#### NFR-04: Security

- All REST API endpoints (except Upstox OAuth2 callback and static resources) shall require HTTP Basic Authentication.
- API credentials (Upstox API key/secret, AWS credentials) shall be stored in environment variables and never committed to version control.
- The Upstox access token shall be stored in the database and retrieved per-request; it shall not be exposed to the frontend.
- Cross-Origin Resource Sharing (CORS) shall be configured to restrict API access to trusted origins in production.

#### NFR-05: Maintainability

- All trading strategies shall implement a common `TradingStrategy` interface and extend the `AbstractTradingStrategy` base class, ensuring consistent structure and easy addition of new strategies.
- Database schema changes shall be managed exclusively through Flyway versioned migration scripts.
- Application configuration shall be centralized in `application.yml` with environment variable overrides for all sensitive values.

#### NFR-06: Portability

- The application shall run on any operating system with Java 21 JDK installed (Linux, macOS, Windows).
- Docker containerization shall be supported in a future release for simplified deployment.

### 3.2.3 Hardware Requirements Summary

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| CPU | 4 cores, 2.0 GHz | 8 cores, 3.0 GHz |
| RAM | 8 GB | 16 GB |
| Disk | 50 GB SSD | 256 GB SSD |
| Network | 10 Mbps | 100 Mbps |

### 3.2.4 Software Requirements Summary

| Software | Version | Purpose |
|----------|---------|---------|
| Java (OpenJDK) | 21 LTS | Backend runtime |
| Spring Boot | 3.4 | Application framework |
| Spring AI | 1.0.0 | AI/LLM integration layer |
| Apache Kafka | 3.x | Event streaming pipeline |
| PostgreSQL | 16 | Primary relational database |
| Flyway | 10.x | Database migration management |
| Upstox Java SDK | 1.19 | Brokerage API integration |
| AWS SDK (Bedrock) | 2.26.4 | Amazon Bedrock runtime client |
| Maven | 3.8+ | Build and dependency management |
| Git | 2.x | Version control |

### 3.2.5 External Interface Requirements

| Interface | Type | Description |
|-----------|------|-------------|
| Upstox REST API v2 | HTTP/REST | Historical data, quote lookup, auth |
| Upstox WebSocket | WebSocket | Real-time live price tick streaming |
| Amazon Bedrock Converse API | HTTPS/REST | LLM-powered AI insights |
| PostgreSQL | JDBC | Primary data persistence |
| Apache Kafka | TCP | Market data event pipeline |
| Web Dashboard | HTTP + WebSocket | User-facing browser interface |
| Spring Actuator | HTTP/REST | Health, metrics, Prometheus monitoring |

---

**End of Chapters 2 and 3**
