package com.example.data.model

enum class AssetClass(val displayName: String, val icon: String) {
    FOREX("Forex", "💱"),
    GOLD("Gold & Commodities", "🪙"),
    CRYPTO("Crypto", "₿"),
    STOCKS("Stocks", "📈"),
    INDICES("Indices", "🌐")
}

data class MarketInstrument(
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val priceDecimals: Int = 2,
    val contractUnit: String = "shares",
    val tickSize: Double = 0.01,
    val sessionHours: String = "09:30 - 16:00 ET",
    val baseCurrency: String = "USD"
)

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

enum class SetupType(val key: String, val displayName: String, val description: String) {
    ORB("orb", "Opening-Range Breakout", "First 5-min close above/below opening range"),
    BREAKOUT_20D("breakout_20d", "20-Day High Breakout", "Last close is above all 20 previous session closes"),
    BULL_FLAG("bull_flag", "Bull Flag Setup", "Up >10% over 10 sessions then consolidates within 5% band"),
    RECLAIM_MA20("reclaim_ma20", "20-Day MA Reclaim", "Crossed from below 20-day SMA to close above it"),
    OVERSOLD("oversold", "Oversold Indicator", "Wilder's 14-period RSI dropped below 30"),
    OVERSOLD_BOUNCE("oversold_bounce", "Oversold Bounce", "RSI was under 30 yesterday and today closed positive")
}

enum class SignalAction {
    BUY,
    SELL,
    WAIT
}

data class TradingSignal(
    val instrument: MarketInstrument,
    val action: SignalAction,
    val setup: SetupType,
    val status: String, // "Triggered", "Waiting", "Expired", "Closed"
    val entryPrice: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val stopRule: String, // "or_mid", "or_low", "atr", "pct", "price"
    val targetR: Double, // 1.0, 1.5, 2.0, 3.0
    val riskRewardRatio: Double,
    val confidenceScore: Int, // 0 - 100 based on historical replay
    val volumeSurgeMultiplier: Double,
    val timestamp: String,
    val rationale: String,
    val isDelayed: Boolean,
    val providerSource: String,
    val sessionPhase: String
)

data class RiskAnalysis(
    val accountBudget: Double,
    val maxLossPerTrade: Double,
    val maxDailyLoss: Double,
    val riskPercentage: Double,
    val calculatedPositionSize: Double,
    val unitLabel: String,
    val totalCostUsd: Double,
    val maxLossUsd: Double,
    val riskPerUnit: Double,
    val bindingConstraint: String,
    val passRiskCheck: Boolean,
    val checkMessages: List<String>
)

data class MonteCarloResult(
    val replays: Int,
    val pTarget: Double,
    val pStop: Double,
    val pNeither: Double,
    val pGreen: Double,
    val medianClose: Double,
    val p5Close: Double,
    val p95Close: Double,
    val avgR: Double,
    val verdict: String,
    val verdictDetail: String,
    val fanBands: List<List<Double>>, // 5 values per remaining bar: p5, p25, p50, p75, p95
    val terminalHistogram: List<Int>,
    val histLo: Double,
    val histHi: Double
)

data class BacktestTrade(
    val id: Int,
    val date: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopPrice: Double,
    val targetPrice: Double,
    val outcome: String, // "TARGET", "STOP", "CLOSE"
    val returnR: Double
)

data class BacktestResult(
    val instrumentSymbol: String,
    val setupName: String,
    val totalSessions: Int,
    val totalTrades: Int,
    val winRatePct: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val netProfitR: Double,
    val avgRPerTrade: Double,
    val trades: List<BacktestTrade>
)

data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val timeAgo: String,
    val relatedSymbol: String,
    val category: String, // "EARNINGS", "MACRO", "DEAL", "REGULATORY", "RISK", "FED"
    val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val volatilityImpact: String, // "HIGH", "MEDIUM", "LOW"
    val summary: String
)

data class ScannerItem(
    val instrument: MarketInstrument,
    val currentPrice: Double,
    val change24hPct: Double,
    val volume: Long,
    val matchedSetups: List<SetupType>,
    val rsi14: Double,
    val primarySignal: SignalAction,
    val rankScore: Double
)
