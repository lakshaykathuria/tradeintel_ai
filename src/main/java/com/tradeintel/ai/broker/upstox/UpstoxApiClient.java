package com.tradeintel.ai.broker.upstox;

import com.upstox.ApiClient;
import com.upstox.ApiException;
import com.upstox.api.*;
import io.swagger.client.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpstoxApiClient {

    private final UpstoxAuthService authService;

    /**
     * Get full market quote for a symbol
     * 
     * @param symbol Instrument key (e.g., "NSE_EQ|INE062A01020" for State Bank of
     *               India)
     */
    public GetFullMarketQuoteResponse getQuote(String symbol) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        MarketQuoteApi marketQuoteApi = new MarketQuoteApi(apiClient);

        log.info("Fetching quote for symbol: {}", symbol);
        return marketQuoteApi.getFullMarketQuote(symbol, "1.0");
    }

    /**
     * Get OHLC data for a symbol
     */
    public GetMarketQuoteOHLCResponse getOHLC(String symbol, String interval) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        MarketQuoteApi marketQuoteApi = new MarketQuoteApi(apiClient);

        log.info("Fetching OHLC for symbol: {}", symbol);
        return marketQuoteApi.getMarketQuoteOHLC(symbol, interval, "1.0");
    }

    /**
     * Place an order
     */
    public PlaceOrderResponse placeOrder(PlaceOrderRequest orderRequest) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        OrderApi orderApi = new OrderApi(apiClient);

        log.info("Placing order: {}", orderRequest);
        return orderApi.placeOrder(orderRequest, "1.0");
    }

    /**
     * Modify an existing order
     */
    public ModifyOrderResponse modifyOrder(ModifyOrderRequest modifyRequest) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        OrderApi orderApi = new OrderApi(apiClient);

        log.info("Modifying order: {}", modifyRequest);
        return orderApi.modifyOrder(modifyRequest, "1.0");
    }

    /**
     * Cancel an order
     */
    public CancelOrderResponse cancelOrder(String orderId) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        OrderApi orderApi = new OrderApi(apiClient);

        log.info("Canceling order: {}", orderId);
        return orderApi.cancelOrder(orderId, "1.0");
    }

    /**
     * Get order book (all orders)
     */
    public GetOrderBookResponse getOrderHistory() throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        OrderApi orderApi = new OrderApi(apiClient);

        log.info("Fetching order history");
        return orderApi.getOrderBook("1.0");
    }

    /**
     * Get order details by ID
     */
    public GetOrderResponse getOrderDetails(String orderId) throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        OrderApi orderApi = new OrderApi(apiClient);

        log.info("Fetching order details for: {}", orderId);
        return orderApi.getOrderDetails("1.0", orderId, null);
    }

    /**
     * Get positions
     */
    public GetPositionResponse getPositions() throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        PortfolioApi portfolioApi = new PortfolioApi(apiClient);

        log.info("Fetching positions");
        return portfolioApi.getPositions("1.0");
    }

    /**
     * Get holdings
     */
    public GetHoldingsResponse getHoldings() throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        PortfolioApi portfolioApi = new PortfolioApi(apiClient);

        log.info("Fetching holdings");
        return portfolioApi.getHoldings("1.0");
    }

    /**
     * Get funds and margin
     */
    public GetUserFundMarginResponse getFundsAndMargin() throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        UserApi userApi = new UserApi(apiClient);

        log.info("Fetching funds and margin");
        return userApi.getUserFundMargin("1.0", null);
    }

    /**
     * Get user profile
     */
    public GetProfileResponse getUserProfile() throws ApiException {
        ApiClient apiClient = authService.getAuthenticatedClient();
        UserApi userApi = new UserApi(apiClient);

        log.info("Fetching user profile");
        return userApi.getProfile("1.0");
    }

    /**
     * Get historical candle data
     * 
     * @param instrumentKey Instrument key
     * @param interval      Candle interval (1minute, 30minute, day, week, month)
     * @param toDate        End date (YYYY-MM-DD)
     * @param fromDate      Start date (YYYY-MM-DD)
     */
    public GetHistoricalCandleResponse getHistoricalData(
            String instrumentKey,
            String interval,
            String toDate,
            String fromDate) throws ApiException {

        ApiClient apiClient = authService.getAuthenticatedClient();
        HistoryApi historyApi = new HistoryApi(apiClient);

        log.info("Fetching historical data for {} from {} to {}", instrumentKey, fromDate, toDate);
        return historyApi.getHistoricalCandleData(instrumentKey, interval, toDate, fromDate);
    }

    /**
     * Get intraday candle data
     */
    public GetIntraDayCandleResponse getIntradayData(
            String instrumentKey,
            String interval) throws ApiException {

        ApiClient apiClient = authService.getAuthenticatedClient();
        HistoryApi historyApi = new HistoryApi(apiClient);

        log.info("Fetching intraday data for {}", instrumentKey);
        return historyApi.getIntraDayCandleData(instrumentKey, interval, "1.0");
    }
}
