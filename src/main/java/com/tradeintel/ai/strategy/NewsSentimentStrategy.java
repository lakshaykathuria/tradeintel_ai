package com.tradeintel.ai.strategy;

import com.tradeintel.ai.model.MarketData;
import com.tradeintel.ai.model.SignalType;
import com.tradeintel.ai.model.Stock;
import com.tradeintel.ai.model.TradeSignal;
import com.tradeintel.ai.service.NewsFetcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * News & trend-based trading strategy.
 * Fetches recent news headlines for the stock symbol via Google News RSS,
 * then uses the AI (ChatClient) to classify the overall sentiment as
 * BUY / SELL / HOLD.
 *
 * <p>
 * Participates in the existing strategy framework — its vote appears in the
 * "Execute All Strategies" consensus alongside the technical strategies.
 */
@Component
@Slf4j
public class NewsSentimentStrategy extends AbstractTradingStrategy {

    private final NewsFetcherService newsFetcherService;
    private final ChatClient.Builder chatClientBuilder;

    public NewsSentimentStrategy(NewsFetcherService newsFetcherService,
            ChatClient.Builder chatClientBuilder) {
        this.newsFetcherService = newsFetcherService;
        this.chatClientBuilder = chatClientBuilder;
        this.strategyName = "News Sentiment Strategy";
        this.description = "AI-powered sentiment analysis of recent news headlines";
        this.minDataPoints = 1; // only needs the current price from market data
    }

    @Override
    public TradeSignal analyze(Stock stock, List<MarketData> marketData) {
        String symbol = stock.getSymbol();
        log.info("Running News Sentiment analysis for {}", symbol);

        // --- Fetch headlines ---
        List<String> headlines = newsFetcherService.fetchHeadlines(symbol);
        if (headlines == null || headlines.isEmpty()) {
            log.warn("No headlines found for {}; returning neutral signal", symbol);
            return createSignal(stock, SignalType.HOLD, 0.3,
                    "No recent news found for " + symbol + ". Defaulting to HOLD.");
        }

        // --- Get current price from most recent market data ---
        double currentPrice = marketData.isEmpty() ? 0.0
                : marketData.get(marketData.size() - 1).getClosePrice().doubleValue();

        // --- Build prompt ---
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "You are a financial news analyst. Based ONLY on the following recent news headlines for the stock ")
                .append(symbol)
                .append(" (current price ₹").append(String.format("%.2f", currentPrice)).append("), ")
                .append("determine the overall market sentiment.\n\n");
        prompt.append("Recent News Headlines:\n");
        for (int i = 0; i < headlines.size(); i++) {
            prompt.append(i + 1).append(". ").append(headlines.get(i)).append("\n");
        }
        prompt.append("\nBased on these headlines, provide:\n");
        prompt.append(
                "SIGNAL: [BUY if news is clearly positive/bullish, SELL if clearly negative/bearish, HOLD if mixed/neutral/insufficient]\n");
        prompt.append("CONFIDENCE: [0-100, where 100 = extremely clear signal]\n");
        prompt.append("REASONING: [2-3 sentence summary of the news sentiment and what it implies for the stock]\n");
        prompt.append("\nBe concise. Do NOT fabricate information not present in the headlines.");

        // --- Call AI ---
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content();

            log.debug("News sentiment AI response for {}: {}", symbol, response);
            return parseResponse(stock, response, headlines);

        } catch (Exception e) {
            log.error("AI call failed in NewsSentimentStrategy for {}: {}", symbol, e.getMessage());
            // Still provide value: return HOLD with the raw headlines as reasoning
            String fallbackReasoning = "AI unavailable. Recent headlines: " + String.join(" | ", headlines);
            return createSignal(stock, SignalType.HOLD, 0.3, fallbackReasoning);
        }
    }

    private TradeSignal parseResponse(Stock stock, String response, List<String> headlines) {
        SignalType signalType = SignalType.HOLD;
        double confidence = 0.5;
        String reasoning = response; // fallback to full response

        // Parse SIGNAL
        String signalLine = extractLine(response, "SIGNAL:");
        if (signalLine != null) {
            String s = signalLine.trim().toUpperCase();
            if (s.startsWith("BUY"))
                signalType = SignalType.BUY;
            else if (s.startsWith("SELL"))
                signalType = SignalType.SELL;
        }

        // Parse CONFIDENCE
        String confLine = extractLine(response, "CONFIDENCE:");
        if (confLine != null) {
            try {
                Matcher m = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(confLine);
                if (m.find()) {
                    double raw = Double.parseDouble(m.group());
                    confidence = Math.min(raw, 100.0) / 100.0;
                }
            } catch (Exception ignored) {
            }
        }

        // Parse REASONING
        String reasoningBlock = extractBlock(response, "REASONING:",
                new String[] { "TARGET_PRICE:", "STOP_LOSS:" });
        if (reasoningBlock != null && !reasoningBlock.isBlank()) {
            reasoning = reasoningBlock.trim();
        }

        // Prepend headline count as context
        reasoning = String.format("📰 Based on %d recent headlines: %s", headlines.size(), reasoning);

        TradeSignal signal = new TradeSignal();
        signal.setStock(stock);
        signal.setSignalType(signalType);
        signal.setConfidenceScore(BigDecimal.valueOf(confidence));
        signal.setReasoning(reasoning);
        return signal;
    }

    /** Extract value on same line as prefix. */
    private String extractLine(String response, String prefix) {
        for (String line : response.split("\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    /** Extract multi-line block between startPrefix and any of stopPrefixes. */
    private String extractBlock(String response, String startPrefix, String[] stopPrefixes) {
        String[] lines = response.split("\n");
        StringBuilder block = new StringBuilder();
        boolean capturing = false;
        for (String line : lines) {
            if (!capturing) {
                if (line.startsWith(startPrefix)) {
                    capturing = true;
                    String first = line.substring(startPrefix.length()).trim();
                    if (!first.isEmpty())
                        block.append(first).append("\n");
                }
            } else {
                boolean stop = false;
                for (String sp : stopPrefixes) {
                    if (line.startsWith(sp)) {
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

    @Override
    public String getStrategyName() {
        return strategyName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * News strategy makes live AI + HTTP calls on every data point —
     * running it through hundreds of historical bars would cause 252+ AI calls
     * and extreme token costs. Excluded from backtesting.
     */
    @Override
    public boolean isBacktestable() {
        return false;
    }
}
