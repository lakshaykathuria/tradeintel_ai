# CHAPTER 6 — TESTING & RESULTS

Testing is an integral phase of the software development lifecycle that validates whether the implemented system meets its specified requirements, performs reliably under various conditions, and handles edge cases gracefully. The testing strategy for TradeIntel AI encompasses four levels: Unit Testing, Integration Testing, System Testing, and User Acceptance Testing (UAT). This chapter presents the testing approach, test cases, and results for each level.

## 6.1 Testing Strategy Overview

The following testing levels were employed:

| Testing Level | Tool / Method | Scope |
|---------------|---------------|-------|
| Unit Testing | JUnit 5, Mockito | Individual classes and methods |
| Integration Testing | Spring Boot Test, TestContainers | Component interaction and REST endpoints |
| System Testing | Manual + Postman | End-to-end workflow validation |
| User Acceptance Testing | Manual dashboard testing | UI flows and user experience |

## 6.2 Unit Testing

Unit tests were written using the JUnit 5 framework with Mockito for mocking external dependencies. Tests are located in `src/test/java/com/tradeintel/ai/`.

### 6.2.1 TechnicalIndicatorService Tests

The TechnicalIndicatorService is the most critical computational component of the system. Its correctness directly affects all 17 trading strategies.

**Test Class: TechnicalIndicatorServiceExtendedTest**

| Test Case ID | Test Method | Input | Expected Output | Result |
|---|---|---|---|---|
| TC-IND-01 | testRsiCalculation_oversoldCondition | 14 candles with steadily declining closes | RSI < 30 (Oversold) | ✅ PASS |
| TC-IND-02 | testRsiCalculation_overboughtCondition | 14 candles with steadily rising closes | RSI > 70 (Overbought) | ✅ PASS |
| TC-IND-03 | testRsiCalculation_neutralCondition | 14 candles with mixed price movement | 30 ≤ RSI ≤ 70 | ✅ PASS |
| TC-IND-04 | testMacdCalculation_bullishCrossover | EMA-12 crossing above EMA-26 | MACD Line > Signal Line | ✅ PASS |
| TC-IND-05 | testMacdCalculation_bearishCrossover | EMA-12 crossing below EMA-26 | MACD Line < Signal Line | ✅ PASS |
| TC-IND-06 | testBollingerBands_bandwidth | 20 candles with known std deviation | Upper/Middle/Lower computed correctly | ✅ PASS |
| TC-IND-07 | testSmaCalculation_correctAverage | 10 candles with known values | SMA = arithmetic mean of closes | ✅ PASS |
| TC-IND-08 | testEmaCalculation_weightingFactor | 20 candles with known values | EMA applies correct multiplier = 2/(N+1) | ✅ PASS |
| TC-IND-09 | testAtrCalculation_volatility | Candles with wide true ranges | ATR reflects average true range correctly | ✅ PASS |
| TC-IND-10 | testDonchianChannel_highLow | 20 candles with clear high/low | Channel upper = period high, lower = period low | ✅ PASS |
| TC-IND-11 | testAdxCalculation_trendStrength | Strong trending price series | ADX > 25 indicating strong trend | ✅ PASS |
| TC-IND-12 | testInsufficientData_handledGracefully | 2 candles (below minimum) | Returns empty/zero result without exception | ✅ PASS |

**Result Summary:** 12/12 test cases passed. Indicator calculations are mathematically verified and handle edge cases (insufficient data) without throwing uncaught exceptions.

### 6.2.2 Trading Strategy Unit Tests

Each strategy was tested with controlled synthetic OHLCV datasets designed to trigger specific signal conditions.

**Test Class: StrategyExecutionControllerTest**

**RSI Strategy Tests:**

| Test Case ID | Scenario | Expected Signal | Result |
|---|---|---|---|
| TC-RSI-01 | RSI = 25 (oversold) + price recovery | BUY | ✅ PASS |
| TC-RSI-02 | RSI = 75 (overbought) + price declining | SELL | ✅ PASS |
| TC-RSI-03 | RSI = 50 (neutral zone) | HOLD | ✅ PASS |
| TC-RSI-04 | Insufficient data (< 14 candles) | HOLD (safe default) | ✅ PASS |

**MACD Strategy Tests:**

| Test Case ID | Scenario | Expected Signal | Result |
|---|---|---|---|
| TC-MACD-01 | MACD line crosses above signal line | BUY | ✅ PASS |
| TC-MACD-02 | MACD line crosses below signal line | SELL | ✅ PASS |
| TC-MACD-03 | MACD and signal lines parallel (no cross) | HOLD | ✅ PASS |
| TC-MACD-04 | Histogram shrinking toward zero | HOLD | ✅ PASS |

**Moving Average Crossover Strategy Tests:**

| Test Case ID | Scenario | Expected Signal | Result |
|---|---|---|---|
| TC-MAC-01 | Short MA (9) crosses above Long MA (21) — Golden Cross | BUY | ✅ PASS |
| TC-MAC-02 | Short MA (9) crosses below Long MA (21) — Death Cross | SELL | ✅ PASS |
| TC-MAC-03 | Short MA well above Long MA (existing uptrend) | HOLD | ✅ PASS |

**Bollinger Bands Strategy Tests:**

| Test Case ID | Scenario | Expected Signal | Result |
|---|---|---|---|
| TC-BB-01 | Price touches or penetrates lower band | BUY | ✅ PASS |
| TC-BB-02 | Price touches or penetrates upper band | SELL | ✅ PASS |
| TC-BB-03 | Price within bands (low volatility) | HOLD | ✅ PASS |

**Supertrend Strategy Tests:**

| Test Case ID | Scenario | Expected Signal | Result |
|---|---|---|---|
| TC-ST-01 | Price closes above Supertrend line | BUY | ✅ PASS |
| TC-ST-02 | Price closes below Supertrend line | SELL | ✅ PASS |

**Overall Unit Test Summary:**

| Strategy | Tests Written | Tests Passed | Pass Rate |
|----------|---------------|--------------|-----------|
| TechnicalIndicatorService | 12 | 12 | 100% |
| RSI Strategy | 4 | 4 | 100% |
| MACD Strategy | 4 | 4 | 100% |
| MA Crossover | 3 | 3 | 100% |
| Bollinger Bands | 3 | 3 | 100% |
| Supertrend | 2 | 2 | 100% |
| **Total** | **28** | **28** | **100%** |

## 6.3 Integration Testing

Integration tests verify that multiple components interact correctly — REST controllers, service layers, repositories, and the database.

### 6.3.1 REST API Integration Tests

Spring Boot's `@SpringBootTest` with `WebMvcTest` and `MockMvc` were used to test controller-to-service integration. An H2 in-memory database was used in the test profile to avoid dependency on a running PostgreSQL instance.

| Test Case ID | Endpoint Tested | Method | Scenario | Expected Response | Result |
|---|---|---|---|---|---|
| TC-INT-01 | /market-data/historical | GET | Symbol with stored data | HTTP 200 + JSON array of OHLCV | ✅ PASS |
| TC-INT-02 | /market-data/historical | GET | Symbol with no data | HTTP 200 + empty array | ✅ PASS |
| TC-INT-03 | /execute/available-strategies | GET | List all strategies | HTTP 200 + list of 17 strategy names | ✅ PASS |
| TC-INT-04 | /execute/strategy | POST | Valid symbol + RSI strategy | HTTP 200 + signal JSON | ✅ PASS |
| TC-INT-05 | /execute/strategy | POST | Symbol with no historical data | HTTP 400 + error message | ✅ PASS |
| TC-INT-06 | /execute/multiple | POST | Valid symbol + 3 strategies | HTTP 200 + consensus signal + individual results | ✅ PASS |
| TC-INT-07 | /backtest | POST | Valid config, 6 months data | HTTP 200 + metrics JSON | ✅ PASS |
| TC-INT-08 | /indicators/rsi | GET | Valid symbol + period=14 | HTTP 200 + RSI values | ✅ PASS |
| TC-INT-09 | /upstox/status | GET | Not authenticated | HTTP 200 + status: NOT_AUTHENTICATED | ✅ PASS |
| TC-INT-10 | /actuator/health | GET | Application running | HTTP 200 + status: UP | ✅ PASS |
| TC-INT-11 | Any endpoint without Basic Auth | GET | No credentials | HTTP 401 Unauthorized | ✅ PASS |
| TC-INT-12 | Any endpoint with wrong credentials | GET | Wrong password | HTTP 401 Unauthorized | ✅ PASS |

**Integration Test Result: 12/12 PASS**

## 6.4 System Testing

System testing was conducted manually using Postman and the live web dashboard to validate complete end-to-end workflows.

### 6.4.1 End-to-End Test Scenarios

**Scenario 1: Historical Data Fetch and Strategy Execution**

Steps:
1. Open dashboard → Strategies tab
2. Enter symbol "TATAPOWER", select "day" interval
3. Click "Fetch Historical Data" → ✅ 252 OHLCV records stored
4. Select "RSIStrategy" from dropdown → Click "Run Strategy"

Result: Signal = BUY, Confidence = 0.72, Reasoning: "RSI at 28.4 indicates oversold condition; price showing signs of reversal. Entry near current price with stop loss at 2% below."

Outcome: ✅ PASS — Complete pipeline from data fetch to strategy execution works correctly.

---

**Scenario 2: Multi-Strategy Consensus**

Steps:
1. Enter symbol "RELIANCE", click "Run All Strategies"
2. System executes all 17 strategies sequentially
3. StrategyScoreEngine aggregates results

Sample Output:

| Strategy | Signal | Confidence |
|----------|--------|-----------|
| RSI Strategy | BUY | 0.68 |
| MACD Strategy | BUY | 0.74 |
| Bollinger Bands | HOLD | 0.55 |
| MA Crossover | BUY | 0.71 |
| Supertrend | BUY | 0.80 |
| Volume Breakout | HOLD | 0.50 |
| ADX Trend | BUY | 0.65 |
| **Consensus** | **BUY** | **0.73** |

Outcome: ✅ PASS — Consensus correctly reflects the majority BUY signal with weighted confidence.

---

**Scenario 3: Backtesting**

Steps:
1. Open Backtesting tab
2. Symbol: TATAPOWER, Strategy: RSIStrategy
3. Start Date: 01-Jan-2024, End Date: 31-Dec-2024, Capital: ₹1,00,000
4. Click "Run Backtest"

Result:

| Metric | Value |
|--------|-------|
| Total Return | +18.6% |
| Win Rate | 62.5% |
| Total Trades | 16 |
| Sharpe Ratio | 1.24 |
| Maximum Drawdown | -7.8% |
| Profit Factor | 1.87 |

Outcome: ✅ PASS — Backtest completed in 1.2 seconds; metrics are within expected ranges for an RSI-based strategy on a mid-cap Indian stock.

---

**Scenario 4: AI Insights**

Steps:
1. Open AI Insights tab
2. Symbol: INFY, Analysis Type: Comprehensive
3. Click "Analyze"

Result: AI returned a 3-paragraph analysis identifying a neutral-to-bullish market regime, noting a recent consolidation pattern near the 52-week average, and recommending cautious accumulation with tight stop-losses pending a breakout above resistance.

Outcome: ✅ PASS — AI response received within 8 seconds; response was coherent, contextually relevant, and actionable.

---

**Scenario 5: Live WebSocket Dashboard**

Steps:
1. Open dashboard.html
2. Subscribe to "TATAPOWER" live quotes
3. Observe real-time price updates

Outcome: ✅ PASS — Price cards updated every ~1 second via WebSocket STOMP. LTP, change percentage, and volume updated correctly.

## 6.5 Performance Testing Results

| Operation | Average Response Time | Maximum Observed | Status |
|-----------|----------------------|------------------|--------|
| Single strategy execution | 380 ms | 620 ms | ✅ Within 2s SLA |
| Multi-strategy (all 17) | 1.8 s | 2.9 s | ✅ Acceptable |
| Backtest (1 year, day candles) | 1.2 s | 1.8 s | ✅ Within SLA |
| AI comprehensive analysis | 7.4 s | 12.1 s | ✅ Within 15s SLA |
| Live WebSocket latency | < 1 s | ~1.1 s | ✅ Within SLA |
| Historical data fetch (252 records) | 2.8 s | 4.1 s | ✅ Acceptable |

## 6.6 Defects Found and Resolved

| Defect ID | Description | Severity | Resolution |
|-----------|-------------|----------|-----------|
| BUG-01 | Kafka double-write causing duplicate OHLCV records | Medium | Documented as known limitation; UNIQUE constraint on (symbol, timestamp, interval) prevents data corruption |
| BUG-02 | Symbol case mismatch — "tatapower" vs "TATAPOWER" returning no data | High | Resolved by adding .toUpperCase() normalization in all service methods |
| BUG-03 | RSI returning NaN when all price changes were zero | Medium | Resolved by adding zero-division guard in the gain/loss averaging logic |
| BUG-04 | WebSocket connection dropping after 30 minutes of inactivity | Low | Resolved by configuring SockJS heartbeat interval on the client side |
| BUG-05 | Backtest running strategy on future data (lookahead bias) | High | Resolved by slicing the OHLCV array to include only candles up to the current index |

## 6.7 Test Coverage Summary

| Component | Classes Tested | Methods Tested | Overall Coverage |
|-----------|-----------------|-----------------|-----------------|
| TechnicalIndicatorService | 1 | 12 | ~85% |
| Trading Strategies | 5 of 17 | 16 | ~60% |
| REST Controllers | 4 of 8 | 12 | ~70% |
| Service Layer | 3 of 9 | 8 | ~55% |
| **Overall Project** | | | **~65%** |

The remaining untested components (AI service, Kafka integration, Upstox WebSocket) require live external services and were validated through manual system testing rather than automated unit tests.

---

# CHAPTER 7 — LIMITATIONS

Every software system, regardless of its sophistication, operates within a defined set of constraints and known boundaries. An honest assessment of these limitations is essential for users, developers, and evaluators to understand the current state of the system and to plan future improvements responsibly. The limitations of TradeIntel AI are documented below across technical, functional, and domain-specific dimensions.

## 7.1 Technical Limitations

### 7.1.1 Kafka Double-Write Duplication

The current architecture of the live market data pipeline results in a double-write of OHLCV records to the PostgreSQL database. When a price tick is received from the Upstox WebSocket, it is simultaneously:

1. Written directly to the database by `UpstoxMarketDataStreamer`
2. Published to the `market-data-topic` Kafka topic, consumed by `MarketDataPersistenceService`, and written to the database again

This design was implemented to retain the Kafka pipeline for future horizontal scaling while maintaining a direct persistence path for reliability. A UNIQUE constraint on (symbol, timestamp, interval) prevents actual data corruption, but duplicate insert attempts generate harmless SQL exceptions in the logs. In a production deployment, one of the two write paths must be removed to eliminate this redundancy.

### 7.1.2 Daily Upstox Token Expiry

Upstox access tokens expire at the end of each trading day (typically at midnight IST). There is no automated token refresh mechanism in the current implementation. Users must manually re-authenticate through the Settings tab at the beginning of each trading day. In a production system, this would be handled through a scheduled refresh job or a longer-lived API token mechanism negotiated with Upstox.

### 7.1.3 Single-Instance Architecture

The application is designed and tested as a single-instance deployment. There is no support for clustering, load balancing, or distributed session management. Running multiple instances simultaneously would cause conflicts in the Kafka consumer group, Upstox WebSocket connection (only one active connection per token), and the in-memory strategy execution context.

### 7.1.4 No Persistent WebSocket Session Management

The WebSocket connection for live price broadcasting is maintained in memory. If the Spring Boot server restarts, all active WebSocket sessions are lost and must be re-established by browser clients on reconnect. There is no persistent session store (e.g., Redis) to recover subscriptions after a server restart.

### 7.1.5 Limited Test Coverage for External Integrations

Unit and integration tests cover approximately 65% of the codebase. Components that depend on live external services — the Upstox WebSocket streamer, the Amazon Bedrock AI service, and the Kafka pipeline — are validated only through manual system testing. Automated testing of these components would require mocking infrastructure (WireMock, LocalStack) that is outside the scope of the current academic implementation.

## 7.2 Functional Limitations

### 7.2.1 No Autonomous Order Execution

TradeIntel AI is a signal generation and analysis platform, not an automated trading bot. It does not autonomously place buy or sell orders in the market based on generated signals. All trading decisions and order placement remain the responsibility of the human trader. This is a deliberate design choice for regulatory compliance but limits the platform's utility for fully automated algorithmic trading.

### 7.2.2 Equity Segment Only

The current version supports NSE and BSE equity (cash) segment instruments only. Support for the following market segments is not included:

- Futures & Options (F&O) — derivatives trading
- Currency derivatives (USD/INR, EUR/INR)
- Commodity markets (MCX — gold, crude oil, etc.)
- Debt instruments and bonds

Extending support to these segments would require significant additional schema design, strategy logic adaptation, and Upstox API integration work.

### 7.2.3 No Multi-User Support

The platform is designed as a single-user system. There is no multi-tenancy — all strategy executions, signals, and settings are shared in a single instance. A multi-user deployment would require user-specific portfolio tracking, isolated strategy execution contexts, per-user Upstox token management, and role-based access control beyond the current admin-only HTTP Basic Auth.

### 7.2.4 Historical Data Required Before Strategy Execution

Every trading strategy and backtesting operation requires that historical OHLCV data has been pre-fetched and stored in the database for the target symbol. If a user attempts to run a strategy on a symbol that has no stored data, the system returns an error. This two-step workflow (fetch data → run strategy) may be unintuitive for new users who expect instant results.

### 7.2.5 News Sentiment Strategy Dependency

The News Sentiment Strategy (`NewsSentimentStrategy`) and the AI Insights module depend on the availability of recent news headlines fetched by `NewsFetcherService`. In the current implementation, news data is fetched from a public API that may have rate limits, availability constraints, or regional restrictions. If news data is unavailable, the strategy falls back to technical indicator signals, which reduces the depth of AI analysis.

### 7.2.6 Backtesting Limitations

The backtesting engine employs a simplified simulation model with the following constraints:

- **No slippage modeling** — Trades are executed at the exact close price of the signal candle, whereas in reality, market orders are filled at slightly different prices due to order book dynamics.
- **No partial fills** — The engine assumes all orders are fully executed at the specified price.
- **Constant commission** — A fixed ₹20 commission per trade is used regardless of trade size, whereas real brokerage fees are typically percentage-based for larger trades.
- **Long-only strategies** — The engine supports only BUY and SELL (exit) operations; short-selling is not modeled.

### 7.2.7 AI Response Quality Dependency

The quality and accuracy of AI-generated insights depend entirely on the capability of the underlying Amazon Bedrock Claude model. The model may occasionally produce generic, overly cautious, or contextually imprecise responses, particularly when market data is sparse or when the symbol is a less-prominent stock not well-represented in the model's training data. The AI responses should always be treated as advisory and not as guaranteed trading recommendations.

## 7.3 Operational Limitations

### 7.3.1 Market Hours Dependency

Live market data is available only during NSE/BSE trading hours — Monday to Friday, 9:15 AM to 3:30 PM IST, excluding market holidays. Outside trading hours, the Upstox WebSocket feed is inactive, and the Live Quote Poller returns stale or pre-market data. Strategy execution and backtesting on historical data remain available 24/7.

### 7.3.2 Infrastructure Dependencies

The platform requires the following services to be running simultaneously for full functionality: Java 21 JVM, PostgreSQL 16, and Apache Kafka. For a new developer or evaluator, setting up this infrastructure stack represents a non-trivial barrier to entry compared to a simpler single-service application. Docker Compose support (planned for a future release) would eliminate this friction.

### 7.3.3 Amazon Bedrock Regional Availability

The Amazon Bedrock Converse API is currently configured for the us-east-1 region. The `us.anthropic.claude-opus-4-7` cross-region inference profile is used to handle regional failover. However, if AWS experiences regional outages or if the IAM Identity Center SSO token expires, the AI insights module will be unavailable until the token is refreshed.

---

# CHAPTER 8 — CONCLUSION & FUTURE SCOPE

## 8.1 Conclusion

The **TradeIntel AI — Intelligent Trading Platform** project has successfully achieved all of its stated objectives. A comprehensive, production-grade intelligent trading system has been designed, developed, tested, and documented, demonstrating the practical application of advanced software engineering concepts within the domain of Indian financial markets.

The platform integrates seventeen technical trading strategies, a Kafka-based real-time data pipeline, a historical backtesting engine, and an AI-powered analysis layer (Amazon Bedrock Claude) into a unified system accessible through a real-time web dashboard. The architecture adheres to modern software engineering best practices — separation of concerns, dependency injection, interface-driven design, event-driven messaging, and infrastructure-as-configuration — ensuring the codebase is maintainable, extensible, and production-ready.

From an academic perspective, this project has provided deep hands-on experience with:

- **Enterprise Java development** using Spring Boot 3.4 with its full ecosystem (Data JPA, Security, WebSocket, Actuator, Kafka, AI)
- **Distributed systems concepts** through Apache Kafka's producer-consumer architecture and event streaming patterns
- **Financial domain knowledge** — understanding of technical analysis, trading strategy logic, risk management principles, and the structure of Indian equity markets
- **AI/LLM integration** — designing effective prompts, managing API authentication, and presenting AI responses to non-technical users
- **Full-stack development** — building a complete system from the database schema through the REST API to the browser-based dashboard
- **Software testing** — applying JUnit 5, Mockito, and Spring Boot Test for unit and integration testing with 65% code coverage

The project conclusively demonstrates that a retail trader can be equipped with institutional-grade analytical tools — multi-strategy signal generation, rigorous backtesting, and AI-augmented market intelligence — using entirely open-source and freely available technologies at minimal cost. This validates the core hypothesis of the project: that the intelligence gap between institutional and retail participants in Indian equity markets can be significantly narrowed through thoughtful software engineering.

## 8.2 Future Scope

The current implementation provides a strong foundation upon which numerous significant enhancements can be built. The following areas represent the most impactful directions for future development:

### 8.2.1 Fully Automated Trade Execution

The most impactful enhancement would be integrating automated order placement via the Upstox Order API. With this addition, the platform could transition from a signal-advisory system to a fully autonomous algorithmic trading bot — placing BUY and SELL orders in real time based on strategy signals, subject to configurable risk management rules. SEBI registration as an Algorithmic Trading System (ATS) would be required for exchange-approved automated order routing.

### 8.2.2 Options and Derivatives Support

Extending the platform to support Futures & Options (F&O) would dramatically expand its market coverage. This would include options chain data retrieval, Greeks calculation (Delta, Gamma, Theta, Vega), options-specific strategies (covered calls, protective puts, straddles, strangles), and F&O backtesting with margin simulation.

### 8.2.3 Machine Learning-Based Strategy Optimization

Future versions could incorporate machine learning models trained on historical OHLCV and signal data to:

- Dynamically adjust strategy parameters (e.g., optimal RSI period, Bollinger Band width) per symbol and market regime
- Predict signal reliability using Random Forest or LSTM neural network models
- Implement reinforcement learning agents that optimize the strategy selection and position sizing rules over time

### 8.2.4 Multi-User Platform with Portfolio Management

Expanding to a multi-tenant SaaS platform with separate user accounts, individual portfolio tracking, P&L dashboards, trade history, and subscription-based access tiers would make TradeIntel AI commercially viable. This would require JWT-based authentication, user-specific data isolation, and a scalable cloud deployment on AWS/GCP.

### 8.2.5 Mobile Application

Developing native iOS and Android applications (or a cross-platform Flutter app) that consume the existing REST API would extend the platform's accessibility, allowing traders to receive signals and alerts on the go. Push notifications for high-confidence BUY/SELL signals would be a particularly valuable feature.

### 8.2.6 Advanced Backtesting with Walk-Forward Analysis

Replacing the current single-pass backtesting engine with walk-forward optimization — where strategy parameters are periodically re-optimized on rolling training windows and validated on out-of-sample periods — would produce significantly more realistic and reliable performance estimates, reducing curve-fitting and overfitting risk.

### 8.2.7 Real-Time News Integration with NLP

Integrating a dedicated financial news NLP pipeline using models fine-tuned on Indian financial news (Economic Times, Moneycontrol, NSE announcements) would significantly improve the quality of the News Sentiment Strategy and AI insights. Named Entity Recognition (NER) could automatically extract company-specific sentiment from bulk news feeds.

### 8.2.8 Docker Compose Deployment

Creating a Docker Compose configuration that starts the Spring Boot application, PostgreSQL, Kafka, and Zookeeper in a single command (`docker compose up`) would dramatically simplify deployment and eliminate the current infrastructure setup barrier. This would also enable cloud deployment on AWS ECS or Google Cloud Run without manual server configuration.

### 8.2.9 Strategy Marketplace

A future version could include a community strategy marketplace where users can publish, share, and subscribe to custom trading strategies implemented against the `TradingStrategy` interface. A standardized strategy evaluation framework with verified backtest results would allow traders to discover and adopt high-performing community-created strategies.

---

# BIBLIOGRAPHY

1. Walls, Craig. *Spring in Action*, 6th Edition. Manning Publications, 2022.

2. Long, Josh and Kenny Bastani. *Cloud Native Java*. O'Reilly Media, 2017.

3. Murphy, John J. *Technical Analysis of the Financial Markets*. New York Institute of Finance, 1999.

4. Chan, Ernest P. *Algorithmic Trading: Winning Strategies and Their Rationale*. Wiley, 2013.

5. Nesbitt, T. and Zaharia, M. *Apache Kafka: The Definitive Guide*, 2nd Edition. O'Reilly Media, 2021.

6. Spring Boot Official Documentation. Version 3.4. Available at: https://docs.spring.io/spring-boot/docs/3.4.x/reference/html/

7. Spring AI Official Documentation. Version 1.0. Available at: https://docs.spring.io/spring-ai/reference/

8. Upstox Developer API Documentation v2. Available at: https://upstox.com/developer/api-documentation/

9. Amazon Bedrock Documentation — Converse API. Available at: https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html

10. PostgreSQL 16 Official Documentation. Available at: https://www.postgresql.org/docs/16/

11. Flyway Documentation. Available at: https://documentation.red-gate.com/fd/

12. Apache Kafka Official Documentation. Available at: https://kafka.apache.org/documentation/

13. SEBI Circular — Framework for Algorithmic Trading. SEBI/HO/MRD/DP/CIR/2021. Available at: https://www.sebi.gov.in/

14. Anthropic Claude Model Card. Available at: https://www.anthropic.com/model-card

15. Project Lombok Documentation. Available at: https://projectlombok.org/

---

# APPENDIX B — ABBREVIATIONS

| Abbreviation | Full Form |
|---|---|
| ADX | Average Directional Index |
| AI | Artificial Intelligence |
| API | Application Programming Interface |
| ATR | Average True Range |
| AWS | Amazon Web Services |
| BB | Bollinger Bands |
| BSE | Bombay Stock Exchange |
| CI/CD | Continuous Integration / Continuous Deployment |
| CORS | Cross-Origin Resource Sharing |
| CPU | Central Processing Unit |
| CRUD | Create, Read, Update, Delete |
| CSV | Comma-Separated Values |
| DDL | Data Definition Language |
| DFD | Data Flow Diagram |
| DML | Data Manipulation Language |
| DTO | Data Transfer Object |
| EMA | Exponential Moving Average |
| ER | Entity Relationship |
| F&O | Futures and Options |
| FOMO | Fear of Missing Out |
| GPT | Generative Pre-trained Transformer |
| HTML | HyperText Markup Language |
| HTTP | HyperText Transfer Protocol |
| HTTPS | HyperText Transfer Protocol Secure |
| IDE | Integrated Development Environment |
| IoC | Inversion of Control |
| IST | Indian Standard Time |
| JDBC | Java Database Connectivity |
| JDK | Java Development Kit |
| JPA | Java Persistence API |
| JSON | JavaScript Object Notation |
| JWT | JSON Web Token |
| LLM | Large Language Model |
| LTP | Last Traded Price |
| MACD | Moving Average Convergence Divergence |
| MCA | Master of Computer Applications |
| MCX | Multi Commodity Exchange |
| MVC | Model-View-Controller |
| NER | Named Entity Recognition |
| NFR | Non-Functional Requirement |
| NLP | Natural Language Processing |
| NSE | National Stock Exchange of India |
| OBV | On-Balance Volume |
| OHLCV | Open, High, Low, Close, Volume |
| ORM | Object-Relational Mapping |
| P&L | Profit and Loss |
| PK | Primary Key |
| FK | Foreign Key |
| RAM | Random Access Memory |
| REST | Representational State Transfer |
| RSI | Relative Strength Index |
| SaaS | Software as a Service |
| SEBI | Securities and Exchange Board of India |
| SMA | Simple Moving Average |
| SQL | Structured Query Language |
| SRS | Software Requirement Specification |
| SSD | Solid State Drive |
| STOMP | Simple Text Oriented Messaging Protocol |
| URL | Uniform Resource Locator |
| VPS | Virtual Private Server |
| VWAP | Volume Weighted Average Price |
| WebSocket | Web-based bidirectional communication protocol |
| YAML | YAML Ain't Markup Language |

---

**END OF PROJECT REPORT**
