# TradeIntel AI — Intelligent Trading Platform
## MCA Project Report (2025-2026)

---

## PRELIMINARIES

### CERTIFICATE OF COMPLETION

This is to certify that the project entitled **"TradeIntel AI — Intelligent Trading Platform"** carried out by **Lakshay Kathuria**, a student of **Master of COMPUTER APPLICATIONS 2nd SEMESTER**, of Ganga Institute of Technology and Management, Kablana, Jhajjar, at **Self-Developed** is a satisfactory account of the bonafide work under supervision, and is recommended towards the end of his 2nd semester of MCA.

**Project Incharge** ________________________    **Student Name: Lakshay Kathuria**
                                              MCA 2nd Sem

**HOD, MCA** ________________________

---

### CERTIFICATION FROM ORGANIZATION

**GANGA INSTITUTE OF TECHNOLOGY & MANAGEMENT**
**DEPARTMENT OF COMPUTER SCIENCE & APPLICATIONS**

**CERTIFICATE OF COMPLETION**

The is to certify that the project entitled "**TradeIntel AI — Intelligent Trading Platform**" carried out by **Lakshay Kathuria**, a student of **Master of COMPUTER APPLICATIONS 2nd SEMESTER**, of Ganga Institute of Technology and Management, Kablana, Jhajjar, at **Self-Developed** is a satisfactory account of the bonafide work, is recommended towards the award of degree of **MCA**.

**Project Incharge** ________________________    **Abhishek Kumar**
                                              MCA 2nd Sem

**HOD, MCA** ________________________

---

### DECLARATION BY THE CANDIDATE

I, **Lakshay Kathuria**, hereby declare that the project work entitled **"TradeIntel AI — Intelligent Trading Platform"** is an authenticated work carried out by me for the partial fulfillment of the award of the degree of **MCA** and this work has not been submitted for similar purpose anywhere else.

**Date:** ________________________

**Place:** ________________________

**Name:** Lakshay Kathuria

**Roll No:** ________________________

**MCA 2nd SEM**

---

### ACKNOWLEDGEMENT

I am very thankful to everyone who supported us for this assignment and gives better opportunities for executing our skills and knowledge in the real environment. We feel immense pleasure in expressing our profound gratitude to our project incharge **Mr. Abhishek Kumar** for his constant support and valuable guidance throughout this project work. We deeply acknowledge the constructive feedback and encouragement received from the faculty members of the Department of Computer Science & Applications at Ganga Institute of Technology & Management.

We extend our sincere appreciation to the technical team at Upstox for providing comprehensive API documentation and WebSocket support, which was instrumental in integrating real-time market data into our platform. We are also grateful to Amazon Web Services for the Bedrock AI services that enabled us to incorporate advanced natural language processing capabilities.

Special thanks to our peers and mentor who helped us in testing, reviewing, and iterating through various phases of development. Their suggestions and constructive criticism played a vital role in enhancing the quality of this project. Finally, we thank all those who contributed directly or indirectly to the successful completion of this project.

**Lakshay Kathuria**
**Roll No: [To be filled]**
**MCA 2nd Sem**
**Ganga Institute of Technology & Management**

---

## LIST OF TABLES

| Table No. | Table Title | Page No. |
|-----------|-------------|----------|
| 3.1 | Hardware Requirements | 8 |
| 3.2 | Software Requirements | 9 |
| 3.3 | Technology Stack Summary | 10 |
| 4.1 | Database Table: stocks | 15 |
| 4.2 | Database Table: market_data | 16 |
| 4.3 | Database Table: trade_signals | 17 |
| 4.4 | Database Table: upstox_tokens | 18 |
| 4.5 | Kafka Topic Configuration | 19 |
| 5.1 | REST API Endpoints — Market Data | 24 |
| 5.2 | REST API Endpoints — Strategy Execution | 25 |
| 5.3 | REST API Endpoints — AI Insights | 26 |
| 5.4 | REST API Endpoints — Backtesting | 27 |
| 5.5 | Risk Management Parameters | 28 |
| 6.1 | Unit Test Cases — RSI Strategy | 32 |
| 6.2 | Unit Test Cases — MACD Strategy | 33 |
| 6.3 | Integration Test Results | 34 |
| 6.4 | Backtest Results — Sample Symbols | 35 |

---

## LIST OF FIGURES

| Figure No. | Figure Title | Page No. |
|------------|-------------|----------|
| 4.1 | Entity Relationship (ER) Diagram | 12 |
| 4.2 | Data Flow Diagram — Level 0 (Context Diagram) | 13 |
| 4.3 | Data Flow Diagram — Level 1 | 14 |
| 4.4 | System Architecture / Module Diagram | 20 |
| 4.5 | Real-Time Data Flow Pipeline | 21 |
| 4.6 | Dashboard — Home Screen (Live Quotes) | 22 |
| 4.7 | Dashboard — Strategy Execution Screen | 23 |
| 4.8 | Dashboard — AI Insights Screen | 24 |
| 4.9 | Dashboard — Backtesting Screen | 25 |
| 4.10 | Dashboard — Settings / OAuth2 Screen | 26 |

---

# CHAPTER 1 — INTRODUCTION

## Overview

The rapid advancement of financial technology has fundamentally transformed the way individuals and institutions participate in capital markets. Over the past decade, algorithmic trading — once the exclusive domain of large investment banks and hedge funds — has steadily become accessible to retail traders through broker APIs, cloud computing, and open-source machine learning libraries. In India specifically, the advent of platforms such as Zerodha, Upstox, and Angel One has democratized access to NSE and BSE markets, enabling millions of new retail investors to trade equities, derivatives, and commodities digitally.

However, despite this democratization of access, a significant intelligence gap continues to exist between institutional and retail traders. Large financial institutions employ teams of quantitative analysts who build proprietary algorithms, backtest them rigorously on years of historical data, and combine multiple signals into a high-confidence trading decision. The retail trader, by contrast, is left with basic charting tools, news feeds, and their own subjective judgment — tools that are wholly insufficient in the face of today's volatile, fast-moving markets.

**TradeIntel AI — Intelligent Trading Platform** has been conceived and developed to bridge precisely this gap. It is a full-stack, Java-based intelligent trading platform that brings together four pillars of modern algorithmic trading under one unified system:

### Key Pillars of the Platform

1. **Live Market Data Streaming** — Real-time NSE/BSE price ticks sourced via the Upstox WebSocket API and distributed through an Apache Kafka event pipeline.

2. **Multi-Strategy Signal Generation** — Seventeen independent technical trading strategies, each producing a BUY/SELL/HOLD signal with a confidence score and human-readable reasoning.

3. **Historical Backtesting Engine** — Ability to replay any strategy on up to one year of historical OHLCV (Open, High, Low, Close, Volume) data and generate performance metrics such as Total Return, Win Rate, Sharpe Ratio, and Maximum Drawdown.

4. **AI-Powered Market Intelligence** — Integration with Amazon Bedrock (Claude model) through Spring AI to perform sentiment analysis, chart pattern recognition, market regime classification (bull/bear/sideways), and multi-strategy synthesis.

The platform is designed with a production-grade architecture using Spring Boot 3.4, PostgreSQL 16, Apache Kafka, Spring WebSocket (STOMP), and Spring Security. A real-time web dashboard — built with Vanilla HTML/CSS/JavaScript and served directly from the Spring Boot server — provides an intuitive interface for traders to interact with all platform capabilities without requiring any additional setup.

---

## 1.1 Abstract of Project

### 1.1.1 Title of the Project

**TradeIntel AI — Intelligent Trading Platform**

### 1.1.2 Objective

The primary objective of the TradeIntel AI project is to design, develop, and deploy a comprehensive intelligent trading platform that automates the process of stock market analysis and signal generation for Indian equity markets (NSE/BSE). The system aims to achieve the following specific objectives:

#### Real-Time Data Acquisition and Distribution
To establish a reliable, low-latency pipeline for receiving live stock price ticks from the Upstox brokerage API via WebSocket, distributing them through Apache Kafka topics, persisting them in a PostgreSQL database, and simultaneously broadcasting them to all connected browser clients via Spring WebSocket and STOMP protocol.

#### Algorithmic Strategy Implementation
To implement seventeen well-established technical trading strategies including RSI (Relative Strength Index), MACD (Moving Average Convergence Divergence), Bollinger Bands, Moving Average Crossover, Stochastic Oscillator, Volume Breakout, Support/Resistance Detection, Supertrend, VWAP (Volume Weighted Average Price), ADX (Average Directional Index), ATR Volatility Bands, Donchian Channel Breakout, Mean Reversion, Gap Trading, and News Sentiment — each producing a standardized signal output.

#### Backtesting Engine
To build a backtesting module that evaluates any implemented trading strategy against historical OHLCV data for a configurable date range and initial capital, producing quantitative performance metrics for strategy validation.

#### AI-Driven Insights
To integrate Amazon Bedrock's Claude AI model (via the Spring AI Bedrock Converse API) to provide market sentiment analysis, chart pattern detection, market regime classification, and multi-strategy signal synthesis that translates complex data into actionable, plain-English recommendations.

#### Risk Management
To implement configurable risk management parameters including maximum position size, maximum portfolio risk per trade, daily loss circuit breakers, maximum drawdown alerts, and default stop-loss and take-profit percentages — ensuring disciplined, rule-based trading.

#### Paper Trading Mode
To offer a simulated trading environment (Paper Trading) with a virtual capital of ₹10,00,000 (10 Lakhs) and a commission of ₹20 per trade, allowing traders to test strategies and gain confidence before deploying real capital.

#### Secure Authentication
To implement the complete Upstox OAuth2 authentication flow with secure token storage and management, alongside Spring Security HTTP Basic Auth for protecting all API endpoints.

---

## 1.2 Problem Specification / Need of Project

### 1.2.1 Background

The Indian stock market sees an average daily trading volume of over ₹50,000 crore on the NSE equity segment alone. With over 5,000 listed companies and rapidly changing market conditions driven by global cues, economic data releases, corporate earnings, and geopolitical events, the task of identifying profitable trading opportunities in real time is an enormous challenge — even for experienced traders.

Retail traders currently face the following critical problems:

### 1.2.2 Information Overload and Signal Noise

A retail trader monitoring the market manually must track price movements, volumes, news, and chart patterns across hundreds of symbols simultaneously. Without automated filtering and signal generation, this becomes impossible to execute accurately and consistently. Most traders end up relying on tips from social media or following the crowd — both of which are notoriously unreliable.

### 1.2.3 Absence of Strategy Validation

Professional trading firms never deploy a strategy without backtesting it exhaustively on historical data. However, retail traders typically lack both the tools and the technical knowledge to backtest their ideas. As a result, strategies are often deployed based on gut feeling or anecdotal evidence, leading to capital losses that could have been avoided with proper validation.

### 1.2.4 Emotional Trading and Lack of Discipline

Human psychology introduces significant biases into trading decisions — fear of missing out (FOMO), panic selling during drawdowns, overconfidence after a winning streak, and anchoring to a particular price. Systematic, algorithm-driven signal generation removes these emotional biases by applying the same rules consistently across all market conditions.

### 1.2.5 Lack of AI-Augmented Decision Support

Even traders who use technical analysis tools are limited to interpreting individual indicators in isolation. They lack the ability to synthesize signals from multiple strategies, detect market regime shifts, or incorporate AI-driven sentiment analysis from news sources. This multi-dimensional analysis is computationally intensive and traditionally inaccessible to retail traders.

### 1.2.6 How TradeIntel AI Addresses These Problems

TradeIntel AI directly addresses each of the above problems:

- **Automated signal generation** across 17 strategies eliminates the need for manual chart analysis and instantly highlights high-confidence trading opportunities.
  
- **The backtesting engine** allows traders to validate any strategy on up to one year of historical NSE/BSE data before risking real capital.
  
- **Algorithm-driven execution** removes emotional bias by applying strict, rule-based logic consistently.
  
- **AI insights** powered by Amazon Bedrock (Claude) synthesize complex market data into concise, plain-English recommendations — combining sentiment analysis, pattern recognition, and multi-strategy consensus into a single actionable output.
  
- **Risk management rules** such as maximum position size, stop-loss defaults, and daily loss circuit breakers enforce trading discipline programmatically.
  
- **Paper Trading mode** provides a completely risk-free environment to practice and refine strategy selection before deploying real funds.

The platform thus empowers retail traders with the same quality of analytical tools that were previously available only to institutional participants, significantly levelling the playing field in Indian equity markets.

---

**[End of Section 1 - More content to follow as provided]**

---

## TABLE OF CONTENTS

| Sr. No | Topic | Page No. |
|--------|-------|----------|
| 1. | Introduction | 1 |
| 1.1 | Abstract of Project | 2 |
| 1.1.1 | Title of the Project | 2 |
| 1.1.2 | Objective | 2 |
| 1.2 | Problem Specification/Need of Project | 4 |
| 2. | Feasibility Study | 7 |
| 3. | Software Requirement Specifications | 8 |
| 3.1 | Introduction | 8 |
| 3.2 | Selection of Technology/Specific Requirements | 9 |
| 4. | Design | 12 |
| 4.1 | ER Diagram | 12 |
| 4.2 | Data Flow Diagram (0 & 1 Level) | 13 |
| 4.3 | Modules | 14 |
| 4.4 | Database | 15 |
| 4.5 | Input-Output form (Screen Layout) | 22 |
| 5. | Implementation/Technological Environment | 24 |
| 6. | Testing & Results | 32 |
| 7. | Limitations | 36 |
| 8. | Conclusion & Future Scope | 37 |
| | Bibliography | 38 |
| | Appendix A: Coding | 39 |
| | Appendix B: Abbreviations | 45 |

---

**Document Format Specifications Applied:**
- Font: Times New Roman
- Title of Chapter: 20pt (Bold)
- Heading: 16pt (Bold)
- Sub-Heading: 14pt (Bold)
- Sub-Subheading: 12pt (Bold)
- Paragraph Text: 12pt
- Line Spacing: 1.5 Lines
- Alignment: Justify
- Margins: Top 1", Bottom 1", Left 1.25", Right 1"
- Page Numbering: Roman (I-VIII) for Preliminaries, Arabic (1+) for Main Content
- Header: Project Title on each page
- Footer: Page number on each page
