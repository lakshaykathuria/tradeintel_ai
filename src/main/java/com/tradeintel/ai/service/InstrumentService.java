package com.tradeintel.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to handle instrument symbol lookup and conversion
 * Converts user-friendly symbols (e.g., "TATAPOWER") to instrument keys (e.g.,
 * "NSE_EQ|INE245A01021")
 */
@Service
@Slf4j
public class InstrumentService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Cache for symbol to instrument key mapping
    private final Map<String, List<InstrumentInfo>> symbolCache = new ConcurrentHashMap<>();
    private final Map<String, InstrumentInfo> instrumentKeyCache = new ConcurrentHashMap<>();

    /**
     * Search for instruments by symbol or name
     * 
     * @param query Search query (e.g., "TATAPOWER", "SBI", "Reliance")
     * @return List of matching instruments
     */
    public List<InstrumentInfo> searchInstruments(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.trim().toUpperCase();

        // Check if it's already an instrument key format (contains |)
        if (normalizedQuery.contains("|")) {
            InstrumentInfo cached = instrumentKeyCache.get(normalizedQuery);
            if (cached != null) {
                return Collections.singletonList(cached);
            }
        }

        // Search in cache
        List<InstrumentInfo> results = new ArrayList<>();

        // Exact symbol match first
        if (symbolCache.containsKey(normalizedQuery)) {
            results.addAll(symbolCache.get(normalizedQuery));
        }

        // Partial matches
        if (results.isEmpty()) {
            results = symbolCache.entrySet().stream()
                    .filter(entry -> entry.getKey().contains(normalizedQuery) ||
                            (entry.getValue().get(0).getName() != null &&
                                    entry.getValue().get(0).getName().toUpperCase().contains(normalizedQuery)))
                    .flatMap(entry -> entry.getValue().stream())
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Resolve a symbol to its Upstox instrument key.
     * Strategy:
     * 1. If it already looks like an instrument key (contains |), return as-is.
     * 2. Check the local cache populated at startup.
     * 3. Fall back to a live NSE API lookup to get the ISIN dynamically.
     */
    public String resolveToInstrumentKey(String symbolOrKey) {
        if (symbolOrKey == null || symbolOrKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        String normalized = symbolOrKey.trim().toUpperCase();

        // Already an instrument key
        if (normalized.contains("|")) {
            return normalized;
        }

        // Check local cache first
        List<InstrumentInfo> matches = searchInstruments(normalized);
        if (!matches.isEmpty()) {
            Optional<InstrumentInfo> nseEquity = matches.stream()
                    .filter(info -> info.getInstrumentKey().startsWith("NSE_EQ|"))
                    .findFirst();
            String key = nseEquity.map(InstrumentInfo::getInstrumentKey)
                    .orElse(matches.get(0).getInstrumentKey());
            log.info("Resolved {} → {} (from cache)", normalized, key);
            return key;
        }

        // Live lookup from NSE API
        log.info("Symbol {} not in cache, attempting live NSE ISIN lookup...", normalized);
        try {
            String isin = lookupIsinFromNse(normalized);
            if (isin != null && !isin.isBlank()) {
                String instrumentKey = "NSE_EQ|" + isin;
                // Cache it for future requests
                addInstrument(instrumentKey, normalized, normalized);
                log.info("Resolved {} → {} (from NSE live lookup)", normalized, instrumentKey);
                return instrumentKey;
            }
        } catch (Exception e) {
            log.warn("NSE live lookup failed for {}: {}", normalized, e.getMessage());
        }

        throw new IllegalArgumentException("No instrument found for symbol: " + symbolOrKey
                + ". Make sure it is a valid NSE equity symbol (e.g. RELIANCE, INFY).");
    }

    /**
     * Call NSE India's quote API to get the ISIN for a given trading symbol.
     * NSE returns JSON like: { "info": { "isin": "INE814H01029", ... }, ... }
     */
    @SuppressWarnings("unchecked")
    private String lookupIsinFromNse(String symbol) {
        try {
            // NSE requires browser-like headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Referer", "https://www.nseindia.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = "https://www.nseindia.com/api/quote-equity?symbol=" + symbol;

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (response.getBody() != null) {
                Map<String, Object> info = (Map<String, Object>) response.getBody().get("info");
                if (info != null) {
                    String isin = (String) info.get("isin");
                    log.info("NSE ISIN for {}: {}", symbol, isin);
                    return isin;
                }
            }
        } catch (Exception e) {
            log.warn("NSE API call failed for symbol {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Load instrument data from Upstox's public instrument JSON file.
     * Falls back to a hardcoded list of popular stocks if download fails.
     */
    @PostConstruct
    public void loadInstrumentData() {
        log.info("Loading instrument data from Upstox...");
        try {
            // Upstox publishes daily instrument files — NSE equity segment
            String url = "https://assets.upstox.com/market-quote/instruments/exchange/NSE.json";
            String json = restTemplate.getForObject(url, String.class);
            if (json != null && !json.isBlank()) {
                parseAndLoadInstruments(json);
                log.info("Loaded {} instruments from Upstox NSE file", instrumentKeyCache.size());
                return;
            }
        } catch (Exception e) {
            log.warn("Could not load instruments from Upstox URL, falling back to hardcoded list: {}", e.getMessage());
        }
        loadHardcodedInstruments();
    }

    /**
     * Parse Upstox NSE JSON instrument file and populate caches.
     * Format: array of objects with instrument_key, trading_symbol, name,
     * instrument_type
     */
    @SuppressWarnings("unchecked")
    private void parseAndLoadInstruments(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> instruments = mapper.readValue(json, List.class);
            int count = 0;
            for (Map<String, Object> inst : instruments) {
                try {
                    String instrumentKey = (String) inst.get("instrument_key");
                    String symbol = (String) inst.get("trading_symbol");
                    String name = (String) inst.get("name");
                    String type = (String) inst.get("instrument_type");

                    // Only load NSE equity instruments (skip futures, options, indices)
                    if (instrumentKey != null && symbol != null
                            && instrumentKey.startsWith("NSE_EQ|")
                            && ("EQ".equals(type) || type == null)) {
                        addInstrument(instrumentKey, symbol, name != null ? name : symbol);
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
            log.info("Parsed {} NSE equity instruments", count);
        } catch (Exception e) {
            log.error("Error parsing instrument JSON: {}", e.getMessage());
            loadHardcodedInstruments();
        }
    }

    /**
     * Fallback: hardcoded list of popular NSE stocks
     */
    private void loadHardcodedInstruments() {
        log.info("Loading hardcoded instrument list...");
        // Nifty 50 + popular stocks
        addInstrument("NSE_EQ|INE062A01020", "SBIN", "State Bank of India");
        addInstrument("NSE_EQ|INE002A01018", "RELIANCE", "Reliance Industries Limited");
        addInstrument("NSE_EQ|INE467B01029", "TCS", "Tata Consultancy Services Limited");
        addInstrument("NSE_EQ|INE245A01021", "TATAPOWER", "Tata Power Company Limited");
        addInstrument("NSE_EQ|INE040A01034", "HDFCBANK", "HDFC Bank Limited");
        addInstrument("NSE_EQ|INE009A01021", "INFY", "Infosys Limited");
        addInstrument("NSE_EQ|INE030A01027", "ICICIBANK", "ICICI Bank Limited");
        addInstrument("NSE_EQ|INE019A01038", "AXISBANK", "Axis Bank Limited");
        addInstrument("NSE_EQ|INE090A01021", "TATASTEEL", "Tata Steel Limited");
        addInstrument("NSE_EQ|INE081A01020", "TATAMOTORS", "Tata Motors Limited");
        addInstrument("NSE_EQ|INE397D01024", "BHARTIARTL", "Bharti Airtel Limited");
        addInstrument("NSE_EQ|INE018A01030", "WIPRO", "Wipro Limited");
        addInstrument("NSE_EQ|INE101D01020", "MARUTI", "Maruti Suzuki India Limited");
        addInstrument("NSE_EQ|INE239A01016", "HCLTECH", "HCL Technologies Limited");
        addInstrument("NSE_EQ|INE192A01025", "SUNPHARMA", "Sun Pharmaceutical Industries Limited");
        // Additional popular stocks
        addInstrument("NSE_EQ|INE814H01029", "ADANIPOWER", "Adani Power Limited");
        addInstrument("NSE_EQ|INE423A01024", "ADANIENT", "Adani Enterprises Limited");
        addInstrument("NSE_EQ|INE931S01010", "ADANIPORTS", "Adani Ports and Special Economic Zone");
        addInstrument("NSE_EQ|INE752E01010", "ADANIGREEN", "Adani Green Energy Limited");
        addInstrument("NSE_EQ|INE910H01017", "ADANITRANS", "Adani Transmission Limited");
        addInstrument("NSE_EQ|INE585B01010", "POWERGRID", "Power Grid Corporation of India");
        addInstrument("NSE_EQ|INE752H01013", "NTPC", "NTPC Limited");
        addInstrument("NSE_EQ|INE522F01014", "ONGC", "Oil and Natural Gas Corporation");
        addInstrument("NSE_EQ|INE242A01010", "IOC", "Indian Oil Corporation Limited");
        addInstrument("NSE_EQ|INE029A01011", "BPCL", "Bharat Petroleum Corporation Limited");
        addInstrument("NSE_EQ|INE117A01022", "BAJFINANCE", "Bajaj Finance Limited");
        addInstrument("NSE_EQ|INE296A01024", "BAJAJFINSV", "Bajaj Finserv Limited");
        addInstrument("NSE_EQ|INE160A01022", "HDFC", "Housing Development Finance Corporation");
        addInstrument("NSE_EQ|INE066A01021", "KOTAKBANK", "Kotak Mahindra Bank Limited");
        addInstrument("NSE_EQ|INE176B01034", "INDUSINDBK", "IndusInd Bank Limited");
        addInstrument("NSE_EQ|INE129A01019", "GRASIM", "Grasim Industries Limited");
        addInstrument("NSE_EQ|INE021A01026", "HINDALCO", "Hindalco Industries Limited");
        addInstrument("NSE_EQ|INE038A01020", "HINDUNILVR", "Hindustan Unilever Limited");
        addInstrument("NSE_EQ|INE001A01036", "HDFCLIFE", "HDFC Life Insurance Company");
        addInstrument("NSE_EQ|INE491A01021", "NESTLEIND", "Nestle India Limited");
        addInstrument("NSE_EQ|INE318A01026", "TITAN", "Titan Company Limited");
        addInstrument("NSE_EQ|INE216A01030", "M&M", "Mahindra and Mahindra Limited");
        addInstrument("NSE_EQ|INE585B01010", "COALINDIA", "Coal India Limited");
        addInstrument("NSE_EQ|INE694A01020", "DRREDDY", "Dr. Reddy's Laboratories Limited");
        addInstrument("NSE_EQ|INE326A01037", "CIPLA", "Cipla Limited");
        addInstrument("NSE_EQ|INE058A01010", "DIVISLAB", "Divi's Laboratories Limited");
        addInstrument("NSE_EQ|INE213A01029", "APOLLOHOSP", "Apollo Hospitals Enterprise Limited");
        addInstrument("NSE_EQ|INE070A01015", "LT", "Larsen and Toubro Limited");
        addInstrument("NSE_EQ|INE528G01035", "LTIM", "LTIMindtree Limited");
        addInstrument("NSE_EQ|INE356A01018", "TECHM", "Tech Mahindra Limited");
        addInstrument("NSE_EQ|INE121A01024", "JSWSTEEL", "JSW Steel Limited");
        addInstrument("NSE_EQ|INE148I01020", "ZOMATO", "Zomato Limited");
        addInstrument("NSE_EQ|INE758T01015", "PAYTM", "One97 Communications Limited");
        addInstrument("NSE_EQ|INE040H01021", "NYKAA", "FSN E-Commerce Ventures Limited");
        addInstrument("NSE_EQ|INE00IN01015", "POLICYBZR", "PB Fintech Limited");
        addInstrument("NSE_EQ|INE647O01011", "IRCTC", "Indian Railway Catering and Tourism Corporation");
        addInstrument("NSE_EQ|INE155A01022", "ASIANPAINT", "Asian Paints Limited");
        addInstrument("NSE_EQ|INE301A01014", "ULTRACEMCO", "UltraTech Cement Limited");
        addInstrument("NSE_EQ|INE383C01021", "SHREECEM", "Shree Cement Limited");
        addInstrument("NSE_EQ|INE059A01026", "HEROMOTOCO", "Hero MotoCorp Limited");
        addInstrument("NSE_EQ|INE917I01010", "BAJAJ-AUTO", "Bajaj Auto Limited");
        addInstrument("NSE_EQ|INE585B01010", "EICHERMOT", "Eicher Motors Limited");
        addInstrument("NSE_EQ|INE274J01014", "TATACOMM", "Tata Communications Limited");
        addInstrument("NSE_EQ|INE092T01019", "TATAELXSI", "Tata Elxsi Limited");
        addInstrument("NSE_EQ|INE467B01029", "PERSISTENT", "Persistent Systems Limited");
        addInstrument("NSE_EQ|INE124L01009", "MPHASIS", "Mphasis Limited");
        addInstrument("NSE_EQ|INE437A01024", "VEDL", "Vedanta Limited");
        addInstrument("NSE_EQ|INE205A01025", "SAIL", "Steel Authority of India Limited");
        addInstrument("NSE_EQ|INE101A01026", "NMDC", "NMDC Limited");
        addInstrument("NSE_EQ|INE775A01035", "BANKBARODA", "Bank of Baroda");
        addInstrument("NSE_EQ|INE028A01039", "PNB", "Punjab National Bank");
        addInstrument("NSE_EQ|INE562A01011", "CANBK", "Canara Bank");
        addInstrument("NSE_EQ|INE237A01028", "UNIONBANK", "Union Bank of India");
        addInstrument("NSE_EQ|INE614G01033", "IDEA", "Vodafone Idea Limited");
        addInstrument("NSE_EQ|INE669C01036", "YESBANK", "Yes Bank Limited");
        addInstrument("NSE_EQ|INE752H01013", "RECLTD", "REC Limited");
        addInstrument("NSE_EQ|INE020B01018", "PFC", "Power Finance Corporation Limited");
        addInstrument("NSE_EQ|INE053F01010", "IRFC", "Indian Railway Finance Corporation");
        addInstrument("NSE_EQ|INE860H01027", "HAL", "Hindustan Aeronautics Limited");
        addInstrument("NSE_EQ|INE263A01024", "BEL", "Bharat Electronics Limited");
        addInstrument("NSE_EQ|INE216P01012", "BHEL", "Bharat Heavy Electricals Limited");
        addInstrument("NSE_EQ|INE003A01024", "GAIL", "GAIL (India) Limited");
        addInstrument("NSE_EQ|INE542A01019", "HINDPETRO", "Hindustan Petroleum Corporation");
        log.info("Loaded {} hardcoded instruments", instrumentKeyCache.size());
    }

    private void addInstrument(String instrumentKey, String symbol, String name) {
        InstrumentInfo info = new InstrumentInfo(instrumentKey, symbol, name);
        instrumentKeyCache.put(instrumentKey, info);

        // Add to symbol cache
        symbolCache.computeIfAbsent(symbol.toUpperCase(), k -> new ArrayList<>()).add(info);
    }

    /**
     * Inner class to hold instrument information
     */
    public static class InstrumentInfo {
        private final String instrumentKey;
        private final String symbol;
        private final String name;

        public InstrumentInfo(String instrumentKey, String symbol, String name) {
            this.instrumentKey = instrumentKey;
            this.symbol = symbol;
            this.name = name;
        }

        public String getInstrumentKey() {
            return instrumentKey;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getName() {
            return name;
        }
    }
}
