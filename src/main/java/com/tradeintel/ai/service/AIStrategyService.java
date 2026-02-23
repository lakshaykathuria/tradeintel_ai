package com.tradeintel.ai.service;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered trading strategy analysis using Spring AI and OpenAI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIStrategyService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * Analyze market data using AI and generate trading signal
     */
    public TradeSignal analyzeWithAI(Stock stock, List<MarketData> marketData, String analysisType) {
        log.info("Running AI analysis for {} using {}", stock.getSymbol(), analysisType);

        try {
            String prompt = buildPrompt(stock, marketData, analysisType);

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("AI Response: {}", response);

            return parseAIResponse(stock, response);

        } catch (Exception e) {
            log.error("Error in AI analysis for {}: {}", stock.getSymbol(), e.getMessage(), e);

            // Return neutral signal on error
            TradeSignal signal = new TradeSignal();
            signal.setStock(stock);
            signal.setSignalType(SignalType.HOLD);
            signal.setConfidenceScore(BigDecimal.ZERO);
            signal.setReasoning("AI analysis failed: " + e.getMessage());
            return signal;
        }
    }

    /**
     * Build AI prompt based on analysis type
     */
    private String buildPrompt(Stock stock, List<MarketData> marketData, String analysisType) {
        StringBuilder prompt = new StringBuilder();

        // Get recent price data
        int dataPoints = Math.min(10, marketData.size());
        List<MarketData> recentData = marketData.subList(marketData.size() - dataPoints, marketData.size());

        MarketData latestData = marketData.get(marketData.size() - 1);

        prompt.append(
                "You are an expert stock market analyst. Analyze the following stock data and provide a trading recommendation.\n\n");
        prompt.append("Stock: ").append(stock.getSymbol()).append(" (").append(stock.getName()).append(")\n");
        prompt.append("Sector: ").append(stock.getSector() != null ? stock.getSector() : "Unknown").append("\n\n");

        prompt.append("Recent Price Data (last ").append(dataPoints).append(" periods):\n");
        for (int i = 0; i < recentData.size(); i++) {
            MarketData data = recentData.get(i);
            prompt.append(String.format("%d. Date: %s, Open: %.2f, High: %.2f, Low: %.2f, Close: %.2f, Volume: %d\n",
                    i + 1,
                    data.getTimestamp(),
                    data.getOpenPrice().doubleValue(),
                    data.getHighPrice().doubleValue(),
                    data.getLowPrice().doubleValue(),
                    data.getClosePrice().doubleValue(),
                    data.getVolume()));
        }

        prompt.append("\nCurrent Price: ").append(latestData.getClosePrice()).append("\n\n");

        // Add analysis type specific instructions
        switch (analysisType.toLowerCase()) {
            case "sentiment":
                prompt.append("Focus on market sentiment analysis. Consider:\n");
                prompt.append("- Price momentum and trend direction\n");
                prompt.append("- Volume patterns and buying/selling pressure\n");
                prompt.append("- Recent price action and volatility\n");
                break;

            case "pattern":
                prompt.append("Focus on technical pattern recognition. Identify:\n");
                prompt.append("- Chart patterns (head and shoulders, triangles, flags, etc.)\n");
                prompt.append("- Support and resistance levels\n");
                prompt.append("- Trend lines and breakouts\n");
                break;

            case "regime":
                prompt.append("Focus on market regime detection. Determine:\n");
                prompt.append("- Current market regime (trending, ranging, volatile)\n");
                prompt.append("- Regime stability and potential transitions\n");
                prompt.append("- Appropriate trading approach for current regime\n");
                break;

            default:
                prompt.append("Provide a comprehensive technical analysis.\n");
        }

        prompt.append("\nProvide your analysis in the following format:\n");
        prompt.append("SIGNAL: [BUY/SELL/HOLD]\n");
        prompt.append("CONFIDENCE: [0-100]\n");
        prompt.append("REASONING: [Your detailed analysis and reasoning]\n");
        prompt.append("TARGET_PRICE: [Optional target price if applicable]\n");
        prompt.append("STOP_LOSS: [Optional stop loss price if applicable]\n");

        return prompt.toString();
    }

    /**
     * Parse AI response and create TradeSignal
     */
    private TradeSignal parseAIResponse(Stock stock, String response) {
        TradeSignal signal = new TradeSignal();
        signal.setStock(stock);

        try {
            // Parse signal type
            SignalType signalType = SignalType.HOLD;
            if (response.contains("SIGNAL: BUY")) {
                signalType = SignalType.BUY;
            } else if (response.contains("SIGNAL: SELL")) {
                signalType = SignalType.SELL;
            }
            signal.setSignalType(signalType);

            // Parse confidence
            String confidenceLine = extractLine(response, "CONFIDENCE:");
            if (confidenceLine != null) {
                try {
                    double confidence = Double.parseDouble(confidenceLine.trim());
                    signal.setConfidenceScore(BigDecimal.valueOf(confidence / 100.0));
                } catch (NumberFormatException e) {
                    signal.setConfidenceScore(BigDecimal.valueOf(0.5));
                }
            } else {
                signal.setConfidenceScore(BigDecimal.valueOf(0.5));
            }

            // Parse reasoning — may be multi-line
            String reasoning = extractBlock(response, "REASONING:",
                    new String[] { "TARGET_PRICE:", "STOP_LOSS:" });
            if (reasoning != null && !reasoning.isBlank()) {
                signal.setReasoning(reasoning.trim());
            } else {
                signal.setReasoning(response);
            }

            // Parse target price
            String targetPriceLine = extractLine(response, "TARGET_PRICE:");
            if (targetPriceLine != null && !targetPriceLine.trim().isEmpty()) {
                try {
                    double targetPrice = Double.parseDouble(targetPriceLine.trim());
                    signal.setTargetPrice(BigDecimal.valueOf(targetPrice));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse target price: {}", targetPriceLine);
                }
            }

            // Parse stop loss
            String stopLossLine = extractLine(response, "STOP_LOSS:");
            if (stopLossLine != null && !stopLossLine.trim().isEmpty()) {
                try {
                    double stopLoss = Double.parseDouble(stopLossLine.trim());
                    signal.setStopLoss(BigDecimal.valueOf(stopLoss));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse stop loss: {}", stopLossLine);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            signal.setSignalType(SignalType.HOLD);
            signal.setConfidenceScore(BigDecimal.ZERO);
            signal.setReasoning("Error parsing AI response: " + e.getMessage());
        }

        return signal;
    }

    /**
     * Extract a single-line value from AI response (text after the label on the
     * same line).
     */
    private String extractLine(String response, String prefix) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    /**
     * Extract a potentially multi-line block starting at {@code startPrefix} and
     * ending just before any of the {@code stopPrefixes} or end of string.
     * The first line may already contain text after the label.
     */
    private String extractBlock(String response, String startPrefix, String[] stopPrefixes) {
        String[] lines = response.split("\n");
        StringBuilder block = new StringBuilder();
        boolean capturing = false;

        for (String line : lines) {
            if (!capturing) {
                if (line.startsWith(startPrefix)) {
                    capturing = true;
                    String firstLinePart = line.substring(startPrefix.length()).trim();
                    if (!firstLinePart.isEmpty()) {
                        block.append(firstLinePart).append("\n");
                    }
                }
            } else {
                // Stop if we hit a known next-section label
                boolean stop = false;
                for (String stop_prefix : stopPrefixes) {
                    if (line.startsWith(stop_prefix)) {
                        stop = true;
                        break;
                    }
                }
                if (stop)
                    break;
                block.append(line).append("\n");
            }
        }

        return block.length() > 0 ? block.toString().trim() : null;
    }

    /**
     * Extract the first valid decimal number from a raw price string produced by
     * AI.
     * Handles cases like "₹382–385" (en-dash range), "₹385.50", "$400", "N/A", etc.
     * Returns null if no number can be found or the value is implausibly
     * large/small.
     */
    private Double parseFirstNumber(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        // Match the first occurrence of an optional minus followed by digits
        // (optionally with a decimal point)
        Matcher m = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(raw);
        if (!m.find())
            return null;
        try {
            double value = Double.parseDouble(m.group());
            // Sanity check: a realistic stock price is between 0.01 and 1,000,000
            if (value > 0 && value < 1_000_000) {
                return value;
            }
            log.warn("Parsed price {} from '{}' is outside sane bounds — ignoring", value, raw);
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get market sentiment analysis
     */
    public String getMarketSentiment(Stock stock, List<MarketData> marketData) {
        TradeSignal signal = analyzeWithAI(stock, marketData, "sentiment");
        return String.format("Sentiment: %s (Confidence: %.0f%%)\nAnalysis: %s",
                signal.getSignalType(),
                signal.getConfidenceScore().multiply(BigDecimal.valueOf(100)).doubleValue(),
                signal.getReasoning());
    }

    /**
     * Detect chart patterns
     */
    public String detectPatterns(Stock stock, List<MarketData> marketData) {
        TradeSignal signal = analyzeWithAI(stock, marketData, "pattern");
        return signal.getReasoning();
    }

    /**
     * Detect market regime
     */
    public String detectMarketRegime(Stock stock, List<MarketData> marketData) {
        TradeSignal signal = analyzeWithAI(stock, marketData, "regime");
        return signal.getReasoning();
    }

    /**
     * Synthesize multiple strategy signals with AI and provide an overall
     * recommendation.
     * Called after "Execute All Strategies" to give the AI's own view.
     *
     * @param stock         The stock being analyzed
     * @param marketData    Recent market data
     * @param signals       The signals generated by all technical strategies
     * @param newsHeadlines Recent news headlines for the stock (may be empty)
     * @param avgBuyPrice   User's average buy price (nullable — if null, generic
     *                      analysis is returned)
     * @return A map with aiSignal, aiConfidence, aiReasoning, aiTargetPrice,
     *         aiStopLoss
     */
    public Map<String, Object> synthesizeStrategiesWithAI(
            Stock stock,
            List<MarketData> marketData,
            List<Map<String, Object>> signals,
            List<String> newsHeadlines,
            Double avgBuyPrice) {

        log.info("Running AI synthesis for {} with {} strategy signals", stock.getSymbol(), signals.size());

        try {
            // marketData is now ASC (oldest first) after the Collections.reverse in the
            // controller
            MarketData latest = marketData.get(marketData.size() - 1); // newest = last element

            // Take last N records (most recent), already in chronological order
            int dataPoints = Math.min(10, marketData.size());
            List<MarketData> recentData = marketData.subList(marketData.size() - dataPoints, marketData.size());

            log.info("=== AI SYNTHESIS INPUT for {} ===", stock.getSymbol());
            log.info("Total market data records: {}", marketData.size());
            log.info("Latest (most recent) close price: {} on {}",
                    latest.getClosePrice(), latest.getTimestamp().toLocalDate());
            log.info("Oldest record in dataset: {} on {}",
                    marketData.get(marketData.size() - 1).getClosePrice(),
                    marketData.get(marketData.size() - 1).getTimestamp().toLocalDate());

            // Build a rich prompt that includes all strategy results
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an expert stock market analyst and portfolio manager.\n\n");
            prompt.append("Stock: ").append(stock.getSymbol()).append("\n");
            prompt.append("Current Price: ₹").append(latest.getClosePrice()).append("\n");
            prompt.append("Recent Price Data (last ").append(dataPoints).append(" periods, chronological):\n");
            for (int i = 0; i < recentData.size(); i++) {
                MarketData d = recentData.get(i);
                prompt.append(String.format("  %d. %s | O:%.2f H:%.2f L:%.2f C:%.2f Vol:%d\n",
                        i + 1, d.getTimestamp().toLocalDate(),
                        d.getOpenPrice().doubleValue(), d.getHighPrice().doubleValue(),
                        d.getLowPrice().doubleValue(), d.getClosePrice().doubleValue(),
                        d.getVolume()));
            }

            // Add strategy results
            prompt.append("\nTechnical Strategy Results:\n");
            for (int i = 0; i < signals.size(); i++) {
                Map<String, Object> s = signals.get(i);
                prompt.append(String.format("  Strategy %d: Signal=%s, Confidence=%.0f%%, Reasoning: %s\n",
                        i + 1,
                        s.getOrDefault("signal", "HOLD"),
                        ((Number) s.getOrDefault("confidence", 0.5)).doubleValue() * 100,
                        s.getOrDefault("reasoning", "N/A")));
            }

            // Inject news headlines if available
            if (newsHeadlines != null && !newsHeadlines.isEmpty()) {
                prompt.append("\nRecent News Headlines (for sentiment context):\n");
                for (int i = 0; i < newsHeadlines.size(); i++) {
                    prompt.append("  ").append(i + 1).append(". ").append(newsHeadlines.get(i)).append("\n");
                }
                prompt.append("Consider these headlines when forming your overall view.\n");
            }

            // Inject personalised P&L context if the user gave their avg buy price
            if (avgBuyPrice != null && avgBuyPrice > 0) {
                double currentPrice = latest.getClosePrice().doubleValue();
                double plPct = ((currentPrice - avgBuyPrice) / avgBuyPrice) * 100.0;
                String plLabel = plPct >= 0 ? String.format("+%.2f%%", plPct) : String.format("%.2f%%", plPct);
                prompt.append("\n--- USER POSITION CONTEXT ---\n");
                prompt.append(String.format(
                        "The user holds this stock with an average buy price of ₹%.2f. "
                                + "At the current price of ₹%.2f they are %s (%s).\n",
                        avgBuyPrice, currentPrice, plPct >= 0 ? "in profit" : "at a loss", plLabel));
                prompt.append("Please tailor your recommendation specifically for someone in this position: "
                        + "should they HOLD and wait, average down, book partial profits, or cut losses? "
                        + "Give a clear, actionable view.\n");
            }

            prompt.append("\nBased on ALL the above technical signals AND your own analysis of the price data, ");
            prompt.append("provide YOUR independent recommendation. Do not just echo the majority vote — ");
            prompt.append("critically evaluate the signals, consider market context, and give your expert view.\n\n");
            prompt.append("Respond in this exact format:\n");
            prompt.append("SIGNAL: [BUY/SELL/HOLD]\n");
            prompt.append("CONFIDENCE: [0-100]\n");
            prompt.append(
                    "REASONING: [Your detailed analysis — what the strategies say, what you agree/disagree with, and why]\n");
            prompt.append("TARGET_PRICE: [A specific price target, or leave blank]\n");
            prompt.append("STOP_LOSS: [A specific stop loss price, or leave blank]\n");

            log.info("=== FULL PROMPT TO AI ===\n{}", prompt.toString());

            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content();

            log.info("=== AI Synthesis Response ===\n{}", response);

            // Parse the response
            Map<String, Object> result = new HashMap<>();

            String signalStr = extractLine(response, "SIGNAL:");
            result.put("aiSignal", signalStr != null ? signalStr.trim() : "HOLD");

            String confidenceStr = extractLine(response, "CONFIDENCE:");
            double confidence = 0.5;
            if (confidenceStr != null) {
                try {
                    confidence = Double.parseDouble(confidenceStr.trim()) / 100.0;
                } catch (Exception ignored) {
                }
            }
            result.put("aiConfidence", confidence);

            String reasoning = extractBlock(response, "REASONING:",
                    new String[] { "TARGET_PRICE:", "STOP_LOSS:" });
            result.put("aiReasoning", reasoning != null && !reasoning.isBlank() ? reasoning.trim() : response);

            String targetStr = extractLine(response, "TARGET_PRICE:");
            log.info("AI raw TARGET_PRICE: '{}'", targetStr);
            if (targetStr != null && !targetStr.isBlank()) {
                Double parsed = parseFirstNumber(targetStr);
                if (parsed != null) {
                    result.put("aiTargetPrice", parsed);
                } else {
                    log.warn("Could not parse AI target price '{}'", targetStr);
                }
            }

            String stopStr = extractLine(response, "STOP_LOSS:");
            log.info("AI raw STOP_LOSS: '{}'", stopStr);
            if (stopStr != null && !stopStr.isBlank()) {
                Double parsed = parseFirstNumber(stopStr);
                if (parsed != null) {
                    result.put("aiStopLoss", parsed);
                } else {
                    log.warn("Could not parse AI stop loss '{}'", stopStr);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Error in AI synthesis for {}: {}", stock.getSymbol(), e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("aiSignal", "HOLD");
            error.put("aiConfidence", 0.0);
            error.put("aiReasoning", "AI analysis unavailable: " + e.getMessage());
            return error;
        }
    }
}
