# Intelligent Trading Automation Platform

A comprehensive trading automation platform built with Java and Spring Boot, featuring Upstox API integration, AI-powered trading strategies, real-time market data streaming, and dual trading modes (real money + paper trading).

## 🚀 Features

### ✅ Completed (Phase 1 & 2)

- **Core Infrastructure**
  - Spring Boot 3.5.10 with Java 21
  - PostgreSQL database with comprehensive schema (11 tables)
  - Apache Kafka for event streaming
  - Spring Security with basic authentication
  - WebSocket support for real-time updates
  - Actuator endpoints for monitoring

- **Upstox API Integration**
  - OAuth authentication flow with Upstox SDK
  - REST API client for trading operations
  - WebSocket client for real-time market data (5000+ instruments)
  - Support for quotes, orders, positions, funds, and historical data
  - Protobuf-based efficient data streaming

- **Dual Trading Modes**
  - Real money trading via Upstox API
  - Paper trading simulation for testing strategies
  - Configurable via environment variables

- **Database Schema**
  - Stocks, Market Data, Portfolios, Positions
  - Orders with status tracking
  - Trading Strategies and Trade Signals
  - Performance Metrics and Audit Logs

### 🚧 In Progress (Phase 3-9)

- Trading Strategy Engine with technical indicators
- AI/ML integration with OpenAI
- Portfolio management and analytics
- Order management system
- REST API endpoints
- Monitoring and analytics dashboard

## 📋 Prerequisites

- Java 21+
- Maven 3.6+
- PostgreSQL 14+
- Apache Kafka 3.0+
- Upstox trading account with API credentials
- OpenAI API key

## 🛠️ Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd tradeintel_ai
   ```

2. **Set up PostgreSQL database**
   ```bash
   createdb trading_platform
   psql -d trading_platform -f src/main/resources/db/migration/V1__initial_schema.sql
   ```

3. **Start Kafka**
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka
   bin/kafka-server-start.sh config/server.properties
   ```

4. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

5. **Build the project**
   ```bash
   mvn clean install
   ```

6. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

## ⚙️ Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# OpenAI
OPENAI_API_KEY=your_openai_key

# Upstox API
UPSTOX_API_KEY=your_api_key
UPSTOX_API_SECRET=your_api_secret
UPSTOX_REDIRECT_URL=http://localhost:8080/api/upstox/callback

# Trading Mode
TRADING_MODE=PAPER_TRADING  # or REAL_MONEY

# Security
SECURITY_USERNAME=admin
SECURITY_PASSWORD=your_secure_password
```

### Application Configuration

The main configuration is in `src/main/resources/application.yml`:

- **Server**: Runs on port 8080 with context path `/api`
- **Database**: PostgreSQL connection with JPA/Hibernate
- **Kafka**: Topics for market data, orders, signals, and portfolio updates
- **Risk Management**: Configurable position limits, stop-loss, and drawdown controls

## 🔐 Upstox Authentication

### Step 1: Get Authorization URL

```bash
curl http://localhost:8080/api/upstox/auth-url
```

### Step 2: Visit the URL and authorize

Open the returned URL in your browser and complete the Upstox login.

### Step 3: Access token is automatically generated

After authorization, you'll be redirected to the callback URL and the access token will be stored.

### Step 4: Check authentication status

```bash
curl http://localhost:8080/api/upstox/status
```

## 📊 API Endpoints

### Authentication

- `GET /api/upstox/auth-url` - Get Upstox authorization URL
- `GET /api/upstox/callback` - OAuth callback endpoint
- `GET /api/upstox/status` - Check authentication status
- `POST /api/upstox/set-token` - Manually set access token (testing)
- `POST /api/upstox/logout` - Logout and invalidate token

### Market Data

- `GET /api/market-data/quote?symbol={instrumentKey}` - Get real-time quote (e.g., NSE_EQ|INE062A01020)
- `GET /api/market-data/health` - Health check

### Monitoring

- `GET /api/actuator/health` - Application health
- `GET /api/actuator/metrics` - Application metrics

## 🏗️ Architecture

```
┌─────────────────┐
│  Spring Boot    │
│   Application   │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌──▼────┐
│Upstox │ │OpenAI │
│  API  │ │  API  │
└───┬───┘ └───────┘
    │
┌───▼──────────────┐
│  Kafka Streams   │
│  - Market Data   │
│  - Order Events  │
│  - Signals       │
└───┬──────────────┘
    │
┌───▼──────────────┐
│   PostgreSQL     │
│   - Stocks       │
│   - Orders       │
│   - Portfolios   │
│   - Positions    │
└──────────────────┘
```

## 📦 Project Structure

```
src/main/java/com/stock/analyzer/
├── broker/upstox/       # Upstox API integration
│   ├── UpstoxAuthService.java
│   ├── UpstoxApiClient.java
│   └── UpstoxMarketDataStreamer.java
├── config/              # Configuration classes
│   ├── UpstoxConfig.java
│   ├── TradingConfig.java
│   ├── KafkaConfig.java
│   ├── SecurityConfig.java
│   └── WebSocketConfig.java
├── controller/          # REST controllers
│   ├── UpstoxAuthController.java
│   └── MarketDataController.java
├── model/              # JPA entities
│   ├── Stock.java
│   ├── MarketData.java
│   ├── Portfolio.java
│   ├── Position.java
│   ├── Order.java
│   ├── TradingStrategy.java
│   └── TradeSignal.java
├── repository/         # Data repositories
└── service/           # Business logic services
```

## ⚠️ Important Notes

### Real Money Trading

> **WARNING**: This platform supports real money trading. Please note:
> - Always test strategies in PAPER_TRADING mode first
> - Set appropriate risk management parameters
> - You are responsible for all trading decisions
> - Past performance does not guarantee future results
> - Trading involves risk of loss

### Risk Management

The platform includes built-in risk management:
- Maximum position size: 10% of portfolio
- Maximum portfolio risk: 2% per trade
- Maximum daily loss: 5%
- Maximum drawdown alert: 15%
- Default stop-loss: 2%
- Default take-profit: 5%

These can be configured in `application.yml`.

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=YourTestClass

# Skip tests during build
mvn clean install -DskipTests
```

## 📈 Next Steps

1. **Phase 3**: Implement trading strategy engine with technical indicators
2. **Phase 4**: Build order management system with dual mode routing
3. **Phase 5**: Create portfolio management and analytics
4. **Phase 6**: Integrate OpenAI for sentiment analysis and predictions
5. **Phase 7**: Complete REST API and WebSocket endpoints
6. **Phase 8**: Add monitoring and analytics dashboard
7. **Phase 9**: Comprehensive testing and validation

## 📝 License

This project is for educational and personal use only.

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines first.

## 📧 Support

For issues and questions, please open an issue on GitHub.

---

**Disclaimer**: This software is provided "as is" without warranty of any kind. Use at your own risk.
