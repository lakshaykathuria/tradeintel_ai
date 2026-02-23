// TradeIntel AI Web Application
// Main JavaScript file for handling all UI interactions

// ========== Global State ==========
let stompClient = null;
let stocks = new Map();
let availableStrategies = {};
let selectedIndex = -1;
let autocompleteResults = [];
let searchTimeout = null;

// ========== Initialization ==========
document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    loadAvailableStrategies();
    connectWebSocket();
    setDefaultDates();
    checkAuthStatus(); // Auto-check auth status on load
});

// ========== Tab Navigation ==========
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;
            switchTab(tabName);
        });
    });
}

function switchTab(tabName) {
    // Update buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabName);
    });

    // Update content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.toggle('active', content.id === tabName);
    });
}

// ========== WebSocket Connection ==========
function connectWebSocket() {
    const socket = new SockJS('/api/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs

    stompClient.connect({}, function (frame) {
        console.log('WebSocket Connected:', frame);
        updateConnectionStatus(true);

        stompClient.subscribe('/topic/market-data', function (message) {
            handleMarketUpdate(JSON.parse(message.body));
        });
    }, function (error) {
        console.error('WebSocket Error:', error);
        updateConnectionStatus(false);
        setTimeout(connectWebSocket, 5000); // Auto reconnect
    });

    socket.onclose = function () {
        console.log('WebSocket connection closed');
        updateConnectionStatus(false);
    };
}

function updateConnectionStatus(connected) {
    const wsStatusEl = document.getElementById('wsStatus');
    if (wsStatusEl) wsStatusEl.textContent = connected ? 'Connected' : 'Disconnected';
    // Header badge is driven by Upstox auth; only update if auth hasn't set it yet
    const statusEl = document.getElementById('connectionStatus');
    if (statusEl && !statusEl.dataset.authChecked) {
        statusEl.className = connected ? 'status-badge connected' : 'status-badge disconnected';
        statusEl.innerHTML = `<span class="status-dot"></span><span class="broker-label">Upstox</span><span class="status-text">${connected ? 'Connected' : 'Disconnected'}</span>`;
    }
}

// ========== Market Data Functions ==========
function setSymbol(symbol) {
    document.getElementById('symbolInput').value = symbol;
    hideAutocomplete();
}

function subscribeSymbol() {
    const symbol = document.getElementById('symbolInput').value.trim().toUpperCase();
    if (!symbol) {
        showToast('Please enter a stock symbol', 'error');
        return;
    }

    showLoading();

    fetch(`/api/market-data/quote?symbol=${encodeURIComponent(symbol)}`)
        .then(response => {
            if (!response.ok) {
                if (response.status === 401 || response.status === 503) {
                    updateConnectionStatus(false);
                }
                throw new Error('Failed to fetch quote');
            }
            return response.json();
        })
        .then(data => {
            handleMarketUpdate(data);
            document.getElementById('symbolInput').value = '';
            showToast(`Added ${symbol} to dashboard`, 'success');
        })
        .catch(error => {
            console.error('Error subscribing:', error);
            showToast(error.message, 'error');
        })
        .finally(() => hideLoading());
}

function handleMarketUpdate(data) {
    const symbolKey = Object.keys(data)[0];
    const stockData = data[symbolKey];
    const instrumentToken = stockData.instrumentToken;
    const safeId = instrumentToken.replace(/\|/g, '_');

    if (!document.getElementById(`card-${safeId}`)) {
        createStockCard(instrumentToken, symbolKey);
    }

    const priceEl = document.getElementById(`price-${safeId}`);
    const cardEl = document.getElementById(`card-${safeId}`);
    const newPrice = stockData.lastPrice;
    const previousPrice = stocks.get(instrumentToken)?.lastPrice || newPrice;

    // Update price
    priceEl.textContent = '₹' + newPrice.toFixed(2);

    // Update volume if element exists
    const volEl = document.getElementById(`vol-${safeId}`);
    if (volEl && stockData.volume) {
        volEl.textContent = stockData.volume.toLocaleString();
    }

    // Flash animation
    if (newPrice > previousPrice) {
        priceEl.classList.remove('down');
        priceEl.classList.add('up');
        cardEl.classList.remove('flash-red');
        void cardEl.offsetWidth;
        cardEl.classList.add('flash-green');
    } else if (newPrice < previousPrice) {
        priceEl.classList.remove('up');
        priceEl.classList.add('down');
        cardEl.classList.remove('flash-green');
        void cardEl.offsetWidth;
        cardEl.classList.add('flash-red');
    }

    stocks.set(instrumentToken, { lastPrice: newPrice, symbol: symbolKey });
}

function createStockCard(instrumentToken, displayName) {
    const safeId = instrumentToken.replace(/\|/g, '_');
    // Strip exchange prefix e.g. "NSE_EQ:TATAPOWER" -> "TATAPOWER"
    const cleanName = displayName.includes(':') ? displayName.split(':').pop() : displayName;
    // Format instrument token for display: "NSE_EQ|INE245A01021" -> "NSE_EQ · INE245A01021"
    const cleanToken = instrumentToken.replace('|', ' · ');

    const card = document.createElement('div');
    card.id = `card-${safeId}`;
    card.className = 'stock-card';
    card.style.cursor = 'pointer';
    card.title = 'Click to use this symbol in Strategies/Backtest';
    card.onclick = () => fillSymbolInForms(cleanName);
    card.innerHTML = `
        <div class="stock-header">
            <div style="min-width: 0;">
                <div class="stock-symbol">${cleanName}</div>
                <div style="font-size: 0.72rem; color: var(--text-muted); font-family: monospace; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${cleanToken}</div>
            </div>
        </div>
        <div id="price-${safeId}" class="stock-price" style="margin: 0.5rem 0 0.75rem;">---</div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
            <div>
                <div style="color: var(--text-muted); font-size: 0.8rem;">Volume</div>
                <div id="vol-${safeId}" style="font-family: monospace; font-size: 0.9375rem; margin-top: 2px;">---</div>
            </div>
        </div>
    `;

    document.getElementById('stockGrid').prepend(card);
}

// Fill symbol in all form inputs
function fillSymbolInForms(symbol) {
    document.getElementById('strategySymbol').value = symbol;
    document.getElementById('backtestSymbol').value = symbol;
    document.getElementById('aiSymbol').value = symbol;
    showToast(`Symbol ${symbol} filled in all forms`, 'success');
}


// ========== Autocomplete Functions ==========
document.getElementById('symbolInput')?.addEventListener('input', (e) => {
    handleSymbolInput(e.target.value);
});

document.getElementById('symbolInput')?.addEventListener('keydown', handleKeyDown);

function handleSymbolInput(value) {
    clearTimeout(searchTimeout);

    if (!value || value.trim().length < 2) {
        hideAutocomplete();
        return;
    }

    searchTimeout = setTimeout(() => {
        searchSymbols(value.trim());
    }, 300);
}

function searchSymbols(query) {
    fetch(`/api/market-data/search?query=${encodeURIComponent(query)}`)
        .then(response => response.ok ? response.json() : [])
        .then(results => displayAutocomplete(results))
        .catch(error => {
            console.error('Search error:', error);
            hideAutocomplete();
        });
}

function displayAutocomplete(results) {
    const dropdown = document.getElementById('autocompleteDropdown');

    if (!results || results.length === 0) {
        hideAutocomplete();
        return;
    }

    autocompleteResults = results;
    selectedIndex = -1;

    dropdown.innerHTML = results.map((item, index) => `
        <div class="autocomplete-item" onclick="selectInstrument('${item.symbol}')" 
             onmouseenter="selectedIndex=${index}; updateSelectedItem();">
            <div style="font-weight: 600;">${item.symbol}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">${item.name}</div>
        </div>
    `).join('');

    dropdown.classList.remove('hidden');
}

function hideAutocomplete() {
    document.getElementById('autocompleteDropdown').classList.add('hidden');
    selectedIndex = -1;
    autocompleteResults = [];
}

function selectInstrument(symbol) {
    document.getElementById('symbolInput').value = symbol;
    hideAutocomplete();
}

function handleKeyDown(event) {
    const dropdown = document.getElementById('autocompleteDropdown');
    const isVisible = !dropdown.classList.contains('hidden');

    if (event.key === 'Enter') {
        event.preventDefault();
        if (isVisible && selectedIndex >= 0 && selectedIndex < autocompleteResults.length) {
            selectInstrument(autocompleteResults[selectedIndex].symbol);
        }
        subscribeSymbol();
    } else if (event.key === 'ArrowDown' && isVisible) {
        event.preventDefault();
        selectedIndex = Math.min(selectedIndex + 1, autocompleteResults.length - 1);
        updateSelectedItem();
    } else if (event.key === 'ArrowUp' && isVisible) {
        event.preventDefault();
        selectedIndex = Math.max(selectedIndex - 1, -1);
        updateSelectedItem();
    } else if (event.key === 'Escape') {
        hideAutocomplete();
    }
}

function updateSelectedItem() {
    const items = document.querySelectorAll('.autocomplete-item');
    items.forEach((item, index) => {
        item.classList.toggle('selected', index === selectedIndex);
    });
}

// ========== Historical Data Functions ==========
function fetchHistoricalData() {
    const symbol = document.getElementById('histSymbol').value.trim().toUpperCase();
    const interval = document.getElementById('histInterval').value;
    const resultEl = document.getElementById('histResult');

    if (!symbol) {
        showToast('Please enter a stock symbol', 'error');
        return;
    }

    resultEl.textContent = '⏳ Fetching data from Upstox...';
    resultEl.style.color = 'var(--text-secondary)';
    showLoading();

    fetch(`/api/market-data/fetch-historical?symbol=${encodeURIComponent(symbol)}&interval=${interval}`, {
        method: 'POST',
        headers: getAuthHeaders()
    })
        .then(response => response.json())
        .then(data => {
            hideLoading();
            if (data.error) {
                resultEl.textContent = `❌ Error: ${data.message}`;
                resultEl.style.color = '#ef4444';
                showToast('Failed to fetch data: ' + data.message, 'error');
            } else {
                resultEl.textContent = `✅ ${data.message}`;
                resultEl.style.color = '#22c55e';
                showToast(data.message, 'success');
                // Pre-fill strategy symbol
                document.getElementById('strategySymbol').value = symbol;
            }
        })
        .catch(error => {
            hideLoading();
            resultEl.textContent = '❌ Network error: ' + error.message;
            resultEl.style.color = '#ef4444';
            showToast('Failed to fetch historical data', 'error');
        });
}

// ========== Strategy Functions ==========
function loadAvailableStrategies() {
    fetch('/api/execute/available-strategies', {
        headers: {
            'Authorization': 'Basic ' + btoa('admin:admin123'),
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(strategies => {
            availableStrategies = strategies;
            populateStrategySelects(strategies);
            console.log('Loaded strategies:', strategies);
        })
        .catch(error => {
            console.error('Error loading strategies:', error);
            showToast('Failed to load strategies', 'error');
        });
}

function populateStrategySelects(strategies) {
    // Strategies excluded from backtesting (make AI/HTTP calls on every bar)
    const NOT_BACKTESTABLE = new Set(['newsSentimentStrategy']);

    ['strategySelect', 'backtestStrategy'].forEach(selectId => {
        const select = document.getElementById(selectId);
        if (!select) return;

        const isBacktest = selectId === 'backtestStrategy';
        const filtered = Object.entries(strategies).filter(([key]) =>
            !isBacktest || !NOT_BACKTESTABLE.has(key)
        );

        select.innerHTML = '<option value="">Select a strategy...</option>' +
            filtered.map(([key, desc]) =>
                `<option value="${key}">${desc}</option>`
            ).join('');
    });
}


function executeSingleStrategy() {
    const symbol = document.getElementById('strategySymbol').value.trim().toUpperCase();
    const strategy = document.getElementById('strategySelect').value;

    if (!symbol || !strategy) {
        showToast('Please enter symbol and select strategy', 'error');
        return;
    }

    showLoading();

    fetch('/api/execute/strategy', {
        method: 'POST',
        headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ strategyName: strategy, symbol: symbol })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayStrategyResult(data);
                showToast('Strategy executed successfully', 'success');
            } else {
                showToast(data.error || 'Execution failed', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('Failed to execute strategy', 'error');
        })
        .finally(() => hideLoading());
}

function executeAllStrategies() {
    const symbol = document.getElementById('strategySymbol').value.trim().toUpperCase();
    const avgBuyPriceRaw = document.getElementById('avgBuyPrice').value.trim();

    if (!symbol) {
        showToast('Please enter a stock symbol', 'error');
        return;
    }

    const strategyNames = Object.keys(availableStrategies);

    // Build request — only include avgBuyPrice if provided
    const requestBody = {
        strategyNames: strategyNames,
        symbol: symbol
    };
    const avgBuyPrice = parseFloat(avgBuyPriceRaw);
    if (avgBuyPriceRaw && !isNaN(avgBuyPrice) && avgBuyPrice > 0) {
        requestBody.avgBuyPrice = avgBuyPrice;
    }

    showLoading();

    fetch('/api/execute/multiple', {
        method: 'POST',
        headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayMultiStrategyResult(data);
                showToast('All strategies executed', 'success');
            } else {
                showToast(data.error || 'Execution failed', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('Failed to execute strategies', 'error');
        })
        .finally(() => hideLoading());
}

function displayStrategyResult(data) {
    const container = document.getElementById('strategyResults');
    const card = document.createElement('div');
    card.className = 'result-card';
    card.innerHTML = `
        <div class="result-header">
            <div>
                <h3>${data.symbol}</h3>
                <p style="color: var(--text-muted); font-size: 0.875rem;">${availableStrategies[data.strategy]}</p>
            </div>
            <span class="signal-badge ${data.signal.toLowerCase()}">${data.signal}</span>
        </div>
        <div class="confidence-bar">
            <div class="confidence-fill" style="width: ${(data.confidence * 100).toFixed(0)}%"></div>
        </div>
        <p style="margin-bottom: 1rem;"><strong>Confidence:</strong> ${(data.confidence * 100).toFixed(1)}%</p>
        <p style="margin-bottom: 1rem;"><strong>Reasoning:</strong> ${data.reasoning}</p>
        ${data.targetPrice ? `<p><strong>Target Price:</strong> ₹${data.targetPrice.toFixed(2)}</p>` : ''}
        ${data.stopLoss ? `<p><strong>Stop Loss:</strong> ₹${data.stopLoss.toFixed(2)}</p>` : ''}
    `;
    container.prepend(card);
}

function displayMultiStrategyResult(data) {
    const container = document.getElementById('strategyResults');
    const card = document.createElement('div');
    card.className = 'result-card';

    const ai = data.aiView || {};
    const aiSignal = ai.aiSignal || 'HOLD';
    const aiConfidence = ai.aiConfidence != null ? (ai.aiConfidence * 100).toFixed(0) : '0';
    const aiReasoning = ai.aiReasoning || 'AI analysis not available.';
    const aiSignalClass = aiSignal.toLowerCase();

    const aiTargetHtml = ai.aiTargetPrice
        ? `<p style="margin: 0.25rem 0;"><strong>🎯 AI Target:</strong> ₹${Number(ai.aiTargetPrice).toFixed(2)}</p>` : '';
    const aiStopHtml = ai.aiStopLoss
        ? `<p style="margin: 0.25rem 0;"><strong>🛡️ AI Stop Loss:</strong> ₹${Number(ai.aiStopLoss).toFixed(2)}</p>` : '';

    card.innerHTML = `
        <div class="result-header">
            <div>
                <h3>${data.symbol} - Multi-Strategy Analysis</h3>
                <p style="color: var(--text-muted); font-size: 0.875rem;">${data.totalStrategies} strategies analyzed</p>
            </div>
            <span class="signal-badge ${data.consensus.toLowerCase()}">${data.consensus} Consensus</span>
        </div>

        <!-- Vote counts -->
        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; margin: 1rem 0;">
            <div style="text-align: center; padding: 1rem; background: rgba(16, 185, 129, 0.1); border-radius: 0.5rem;">
                <div style="font-size: 2rem; font-weight: 700; color: var(--success);">${data.buyVotes}</div>
                <div style="color: var(--text-muted); font-size: 0.875rem;">BUY</div>
            </div>
            <div style="text-align: center; padding: 1rem; background: rgba(239, 68, 68, 0.1); border-radius: 0.5rem;">
                <div style="font-size: 2rem; font-weight: 700; color: var(--danger);">${data.sellVotes}</div>
                <div style="color: var(--text-muted); font-size: 0.875rem;">SELL</div>
            </div>
            <div style="text-align: center; padding: 1rem; background: rgba(245, 158, 11, 0.1); border-radius: 0.5rem;">
                <div style="font-size: 2rem; font-weight: 700; color: var(--warning);">${data.holdVotes}</div>
                <div style="color: var(--text-muted); font-size: 0.875rem;">HOLD</div>
            </div>
        </div>

        <!-- AI View Section -->
        <div style="margin-top: 1.25rem; padding: 1.25rem; background: linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.08)); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: 0.75rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <span style="font-size: 1.25rem;">🤖</span>
                    <strong style="font-size: 1rem; color: var(--primary);">AI's Independent View</strong>
                </div>
                <span class="signal-badge ${aiSignalClass}" style="font-size: 0.9rem;">${aiSignal}</span>
            </div>
            <div style="margin-bottom: 0.5rem;">
                <div style="display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.25rem;">
                    <span>AI Confidence</span><span>${aiConfidence}%</span>
                </div>
                <div class="confidence-bar">
                    <div class="confidence-fill" style="width: ${aiConfidence}%"></div>
                </div>
            </div>
            <div style="font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6; margin: 0.75rem 0; white-space: pre-wrap;">${aiReasoning.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')}</div>

            ${aiTargetHtml}${aiStopHtml}
        </div>

        <!-- Individual signals collapsible -->
        <details style="margin-top: 1rem;">
            <summary style="cursor: pointer; font-weight: 600; margin-bottom: 0.5rem;">View Individual Strategy Signals</summary>
            ${data.signals.map(s => `
                <div style="padding: 0.75rem; margin: 0.5rem 0; background: var(--bg-tertiary); border-radius: 0.5rem;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span class="signal-badge ${s.signal.toLowerCase()}">${s.signal}</span>
                        <span>${(s.confidence * 100).toFixed(0)}%</span>
                    </div>
                    <p style="margin-top: 0.5rem; font-size: 0.875rem; color: var(--text-muted);">${s.reasoning}</p>
                </div>
            `).join('')}
        </details>
    `;
    container.prepend(card);
}

// ========== Backtest Functions ==========
function setDefaultDates() {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 6);

    document.getElementById('backtestEndDate').valueAsDate = endDate;
    document.getElementById('backtestStartDate').valueAsDate = startDate;
}

function runBacktest() {
    const symbol = document.getElementById('backtestSymbol').value.trim().toUpperCase();
    const strategy = document.getElementById('backtestStrategy').value;
    const startDate = document.getElementById('backtestStartDate').value;
    const endDate = document.getElementById('backtestEndDate').value;
    const capital = document.getElementById('backtestCapital').value;

    if (!symbol || !strategy) {
        showToast('Please enter symbol and select strategy', 'error');
        return;
    }

    showLoading();

    const requestBody = {
        strategyName: strategy,
        symbol: symbol,
        startDate: startDate ? new Date(startDate).toISOString() : null,
        endDate: endDate ? new Date(endDate).toISOString() : null,
        initialCapital: parseFloat(capital)
    };

    fetch('/api/backtest', {
        method: 'POST',
        headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    })
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                showToast(data.error, 'error');
            } else {
                displayBacktestResult(data);
                showToast('Backtest completed', 'success');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('Backtest failed', 'error');
        })
        .finally(() => hideLoading());
}

function displayBacktestResult(data) {
    const container = document.getElementById('backtestResults');
    const card = document.createElement('div');
    card.className = 'result-card';
    card.innerHTML = `
        <div class="result-header">
            <div>
                <h3>${data.symbol} - ${data.strategyName}</h3>
                <p style="color: var(--text-muted); font-size: 0.875rem;">Backtest Results</p>
            </div>
        </div>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-top: 1rem;">
            <div class="info-item">
                <span class="info-label">Total Return</span>
                <span class="info-value" style="color: ${data.totalReturnPercentage >= 0 ? 'var(--success)' : 'var(--danger)'}">
                    ${data.totalReturnPercentage >= 0 ? '+' : ''}${data.totalReturnPercentage.toFixed(2)}%
                </span>
            </div>
            <div class="info-item">
                <span class="info-label">Win Rate</span>
                <span class="info-value">${data.winRate.toFixed(2)}%</span>
            </div>
            <div class="info-item">
                <span class="info-label">Total Trades</span>
                <span class="info-value">${data.totalTrades}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Sharpe Ratio</span>
                <span class="info-value">${data.sharpeRatio.toFixed(2)}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Max Drawdown</span>
                <span class="info-value" style="color: var(--danger)">${data.maxDrawdownPercentage.toFixed(2)}%</span>
            </div>
            <div class="info-item">
                <span class="info-label">Profit Factor</span>
                <span class="info-value">${data.profitFactor.toFixed(2)}</span>
            </div>
        </div>
    `;
    container.prepend(card);
}

// ========== AI Insights Functions ==========
function runAIAnalysis() {
    const symbol = document.getElementById('aiSymbol').value.trim().toUpperCase();
    const analysisType = document.getElementById('aiAnalysisType').value;

    if (!symbol) {
        showToast('Please enter a stock symbol', 'error');
        return;
    }

    showLoading();

    fetch('/api/ai/analyze', {
        method: 'POST',
        headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ symbol: symbol, analysisType: analysisType })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayAIResult(data);
                showToast('AI analysis completed', 'success');
            } else {
                showToast(data.error || 'Analysis failed', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('AI analysis failed', 'error');
        })
        .finally(() => hideLoading());
}

function displayAIResult(data) {
    const container = document.getElementById('aiResults');
    const card = document.createElement('div');
    card.className = 'result-card';
    card.innerHTML = `
        <div class="result-header">
            <div>
                <h3>${data.symbol} - AI Analysis</h3>
                <p style="color: var(--text-muted); font-size: 0.875rem;">${data.analysisType}</p>
            </div>
            <span class="signal-badge ${data.signal.toLowerCase()}">🤖 ${data.signal}</span>
        </div>
        <div class="confidence-bar">
            <div class="confidence-fill" style="width: ${(data.confidence * 100).toFixed(0)}%"></div>
        </div>
        <p style="margin: 1rem 0;"><strong>Confidence:</strong> ${(data.confidence * 100).toFixed(1)}%</p>
        <div style="background: var(--bg-tertiary); padding: 1rem; border-radius: 0.5rem; margin: 1rem 0;">
            <p style="white-space: pre-wrap;">${data.reasoning}</p>
        </div>
        ${data.targetPrice ? `<p><strong>Target Price:</strong> ₹${data.targetPrice.toFixed(2)}</p>` : ''}
        ${data.stopLoss ? `<p><strong>Stop Loss:</strong> ₹${data.stopLoss.toFixed(2)}</p>` : ''}
    `;
    container.prepend(card);
}

// ========== Settings Functions ==========
function getAuthUrl() {
    showLoading();
    fetch('/api/upstox/auth-url', {  // Added /api prefix
        headers: getAuthHeaders()
    })
        .then(response => response.json())  // Parse as JSON, not text
        .then(data => {
            hideLoading();
            if (data.authUrl) {
                window.open(data.authUrl, '_blank');  // Extract authUrl from JSON
                showToast('Auth URL opened in new tab', 'info');
            } else {
                showToast('Failed to get auth URL', 'error');
            }
        })
        .catch(error => {
            hideLoading();
            console.error('Error:', error);
            showToast('Failed to get auth URL', 'error');
        });
}

function checkAuthStatus() {
    showLoading();
    const authStatusEl = document.getElementById('authStatus');
    const headerBadge = document.getElementById('connectionStatus');
    fetch('/api/upstox/status', {
        headers: getAuthHeaders()
    })
        .then(response => response.json())
        .then(data => {
            hideLoading();
            if (headerBadge) headerBadge.dataset.authChecked = 'true';
            if (data.authenticated) {
                if (authStatusEl) authStatusEl.textContent = '✅ Authenticated';
                // Update header badge to show authenticated Upstox connection
                if (headerBadge) {
                    headerBadge.className = 'status-badge connected';
                    headerBadge.innerHTML = '<span class="status-dot"></span><span class="broker-label">Upstox</span><span class="status-text">Authenticated</span>';
                }
                showToast('✅ Upstox Authenticated - Token is valid', 'success');
            } else if (data.hasToken) {
                if (authStatusEl) authStatusEl.textContent = '⚠️ Token Expired';
                if (headerBadge) {
                    headerBadge.className = 'status-badge warning';
                    headerBadge.innerHTML = '<span class="status-dot"></span><span class="broker-label">Upstox</span><span class="status-text">Token Expired</span>';
                }
                showToast('⚠️ Upstox token exists but may be expired', 'warning');
            } else {
                if (authStatusEl) authStatusEl.textContent = '❌ Not Authenticated';
                if (headerBadge) {
                    headerBadge.className = 'status-badge disconnected';
                    headerBadge.innerHTML = '<span class="status-dot"></span><span class="broker-label">Upstox</span><span class="status-text">Not Connected</span>';
                }
                showToast('❌ Not connected to Upstox - Please login', 'error');
            }
        })
        .catch(error => {
            hideLoading();
            if (authStatusEl) authStatusEl.textContent = '❌ Error';
            console.error('Error:', error);
            showToast('Failed to check Upstox status', 'error');
        });
}

// ========== Utility Functions ==========
function getAuthHeaders() {
    return {
        'Authorization': 'Basic ' + btoa('admin:admin123'),
        'Content-Type': 'application/json'
    };
}

function showLoading() {
    document.getElementById('loadingOverlay').classList.remove('hidden');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.add('hidden');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Close autocomplete when clicking outside
document.addEventListener('click', (e) => {
    const input = document.getElementById('symbolInput');
    const dropdown = document.getElementById('autocompleteDropdown');
    if (input && dropdown && !input.contains(e.target) && !dropdown.contains(e.target)) {
        hideAutocomplete();
    }
});
