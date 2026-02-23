package com.tradeintel.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches recent news headlines for a stock symbol from Google News RSS.
 * Results are cached per symbol for 15 minutes to avoid excessive HTTP calls.
 */
@Service
@Slf4j
public class NewsFetcherService {

    private static final int MAX_HEADLINES = 10;
    private static final long CACHE_TTL_MS = 15 * 60 * 1000L; // 15 minutes
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 8_000;

    /** Simple cache entry: headlines + fetch timestamp */
    private record CacheEntry(List<String> headlines, long fetchedAt) {
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Fetch up to {@value #MAX_HEADLINES} news headlines for the given symbol.
     * Returns an empty list if fetching or parsing fails.
     *
     * @param symbol Stock symbol, e.g. "TATAPOWER"
     */
    public List<String> fetchHeadlines(String symbol) {
        String cacheKey = symbol.toUpperCase();
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null && (Instant.now().toEpochMilli() - entry.fetchedAt()) < CACHE_TTL_MS) {
            log.debug("Returning cached news headlines for {}", symbol);
            return entry.headlines();
        }

        List<String> headlines = fetchFromRss(symbol);
        cache.put(cacheKey, new CacheEntry(headlines, Instant.now().toEpochMilli()));
        return headlines;
    }

    private List<String> fetchFromRss(String symbol) {
        List<String> results = new ArrayList<>();
        // Google News RSS — India edition, search for symbol + stock
        String urlStr = String.format(
                "https://news.google.com/rss/search?q=%s+NSE+stock&hl=en-IN&gl=IN&ceid=IN:en",
                symbol.replace(" ", "+"));

        try {
            log.info("Fetching news RSS for {} from {}", symbol, urlStr);

            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            // Google News requires a recognisable User-Agent
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (compatible; StockAnalyzer/1.0)");
            conn.setRequestProperty("Accept", "application/rss+xml,application/xml,text/xml");

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("News RSS returned HTTP {} for {}", status, symbol);
                return results;
            }

            try (InputStream is = conn.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // Disable external entity processing for safety
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setExpandEntityReferences(false);

                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(is);

                NodeList items = doc.getElementsByTagName("item");
                int count = Math.min(MAX_HEADLINES, items.getLength());
                for (int i = 0; i < count; i++) {
                    NodeList titleNodes = items.item(i).getChildNodes();
                    for (int j = 0; j < titleNodes.getLength(); j++) {
                        if ("title".equalsIgnoreCase(titleNodes.item(j).getNodeName())) {
                            String title = titleNodes.item(j).getTextContent().trim();
                            if (!title.isBlank()) {
                                results.add(title);
                            }
                            break;
                        }
                    }
                }
            }

            log.info("Fetched {} headlines for {}", results.size(), symbol);

        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
        }

        return results;
    }
}
