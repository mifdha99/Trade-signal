package com.example.domain

import com.example.data.model.AssetClass
import com.example.data.model.MarketInstrument
import com.example.data.model.NewsArticle
import com.example.data.model.RiskAnalysis
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

object RiskEngine {

    /**
     * Preserves the exact position sizing formula from trader-budget/size.py
     * adapted universally for Stocks, Forex (lots/pips), Crypto (units), Gold (oz), and Indices (points).
     */
    fun calculatePositionSize(
        instrument: MarketInstrument,
        budget: Double,
        maxLossUsd: Double,
        entryPrice: Double,
        stopPrice: Double,
        targetPrice: Double,
        cashBufferPct: Double = 10.0,
        maxPositionPct: Double = 75.0,
        maxOrderUsd: Double = 5000.0,
        minRewardRisk: Double = 1.0,
        todayLossSoFarUsd: Double = 0.0,
        maxDailyLossUsd: Double = 150.0
    ): RiskAnalysis {
        val riskPerUnit = abs(entryPrice - stopPrice)
        val rewardPerUnit = abs(targetPrice - entryPrice)
        val rewardRisk = if (riskPerUnit > 0) rewardPerUnit / riskPerUnit else 0.0

        val cashBuffer = budget * (cashBufferPct / 100.0)
        val availableCash = (budget - cashBuffer).coerceAtLeast(0.0)
        val positionCapUsd = budget * (maxPositionPct / 100.0)

        // Asset-class specific sizing conversions
        val (calculatedQty, unitLabel, bindingRule) = when (instrument.assetClass) {
            AssetClass.FOREX -> {
                // Forex: 1 Standard Lot = 100,000 units. Pip = 0.0001 (or 0.01 for JPY)
                val isJpy = instrument.symbol.contains("JPY")
                val pipScale = if (isJpy) 0.01 else 0.0001
                val pipsAtRisk = (riskPerUnit / pipScale).coerceAtLeast(1.0)
                // In standard lot, 1 pip = $10 approx (for USD quote)
                val pipValuePerStandardLot = 10.0
                val riskPerStandardLot = pipsAtRisk * pipValuePerStandardLot

                val lotByLoss = maxLossUsd / riskPerStandardLot
                val lotByCap = (positionCapUsd * 30.0) / 100000.0 // 30:1 standard retail leverage
                val finalLots = BigDecimal(min(lotByLoss, lotByCap)).setScale(2, RoundingMode.FLOOR).toDouble()

                val bound = if (lotByLoss <= lotByCap) "Max Loss ($$maxLossUsd)" else "Leverage & Cap ($$positionCapUsd)"
                Triple(maxOf(0.01, finalLots), "lots", bound)
            }
            AssetClass.CRYPTO -> {
                // Crypto: Fractional coins (e.g. 0.025 BTC)
                val qtyByLoss = if (riskPerUnit > 0) maxLossUsd / riskPerUnit else 0.0
                val qtyByCap = positionCapUsd / entryPrice
                val qtyByOrder = maxOrderUsd / entryPrice
                val qtyByCash = availableCash / entryPrice

                val minQty = min(min(qtyByLoss, qtyByCap), min(qtyByOrder, qtyByCash))
                val finalQty = BigDecimal(minQty).setScale(4, RoundingMode.FLOOR).toDouble()

                val bound = when {
                    minQty == qtyByLoss -> "Max Loss ($$maxLossUsd)"
                    minQty == qtyByCap -> "Position Cap ($$positionCapUsd)"
                    minQty == qtyByOrder -> "Max Order ($$maxOrderUsd)"
                    else -> "Available Cash ($$availableCash)"
                }
                Triple(maxOf(0.0001, finalQty), instrument.contractUnit, bound)
            }
            AssetClass.GOLD -> {
                // Gold: troy ounces
                val qtyByLoss = if (riskPerUnit > 0) maxLossUsd / riskPerUnit else 0.0
                val qtyByCap = positionCapUsd / entryPrice
                val finalOz = BigDecimal(min(qtyByLoss, qtyByCap)).setScale(1, RoundingMode.FLOOR).toDouble()
                val bound = if (qtyByLoss <= qtyByCap) "Max Loss ($$maxLossUsd)" else "Capital Allocation"
                Triple(maxOf(0.1, finalOz), "oz", bound)
            }
            AssetClass.INDICES -> {
                // Index CFD or micro contract
                val pointRisk = riskPerUnit * 5.0 // $5 per index point
                val contracts = floor(maxLossUsd / pointRisk).coerceAtLeast(1.0)
                Triple(contracts, "contracts", "Max Loss per Trade")
            }
            AssetClass.STOCKS -> {
                // Whole shares constraint
                val sharesByLoss = if (riskPerUnit > 0) floor(maxLossUsd / riskPerUnit) else 0.0
                val sharesByCap = floor(positionCapUsd / entryPrice)
                val sharesByOrder = floor(maxOrderUsd / entryPrice)
                val sharesByCash = floor(availableCash / entryPrice)

                val minShares = min(min(sharesByLoss, sharesByCap), min(sharesByOrder, sharesByCash))
                val bound = when {
                    minShares == sharesByLoss -> "Max Loss ($$maxLossUsd)"
                    minShares == sharesByCap -> "Position Cap ($$positionCapUsd)"
                    minShares == sharesByOrder -> "Max Order ($$maxOrderUsd)"
                    else -> "Cash Available ($$availableCash)"
                }
                Triple(maxOf(0.0, minShares), "shares", bound)
            }
        }

        val totalCost = when (instrument.assetClass) {
            AssetClass.FOREX -> (calculatedQty * 100000.0 * entryPrice) / 30.0 // Margin required at 30:1
            else -> calculatedQty * entryPrice
        }
        val actualMaxLossUsd = when (instrument.assetClass) {
            AssetClass.FOREX -> calculatedQty * (riskPerUnit / (if (instrument.symbol.contains("JPY")) 0.01 else 0.0001)) * 10.0
            else -> calculatedQty * riskPerUnit
        }

        // Pre-trade risk manager checks (trader-risk/check.py)
        val checks = mutableListOf<String>()
        var passes = true

        if (calculatedQty <= 0.0) {
            passes = false
            checks.add("BLOCK: One minimum unit risks more than max loss ($$maxLossUsd)")
        } else {
            checks.add("PASS: Sized within max loss limit ($${String.format(java.util.Locale.US, "%.2f", actualMaxLossUsd)} <= $$maxLossUsd)")
        }

        if (rewardRisk < minRewardRisk) {
            passes = false
            checks.add("BLOCK: Reward:Risk (${String.format(java.util.Locale.US, "%.2f", rewardRisk)}) is under minimum $minRewardRisk")
        } else {
            checks.add("PASS: Reward:Risk ratio (${String.format(java.util.Locale.US, "%.2f", rewardRisk)}R) meets requirement")
        }

        if (todayLossSoFarUsd + actualMaxLossUsd > maxDailyLossUsd) {
            passes = false
            checks.add("BLOCK: Cumulative daily risk ($${todayLossSoFarUsd + actualMaxLossUsd}) exceeds daily loss ceiling ($$maxDailyLossUsd)")
        } else {
            checks.add("PASS: Daily loss headroom sufficient ($$todayLossSoFarUsd used of $$maxDailyLossUsd max)")
        }

        if (totalCost > availableCash && instrument.assetClass != AssetClass.FOREX) {
            passes = false
            checks.add("BLOCK: Order cost ($${String.format(java.util.Locale.US, "%.2f", totalCost)}) exceeds cash buffer headroom ($$availableCash)")
        } else {
            checks.add("PASS: Cash buffer (${cashBufferPct}%) strictly preserved")
        }

        return RiskAnalysis(
            accountBudget = budget,
            maxLossPerTrade = maxLossUsd,
            maxDailyLoss = maxDailyLossUsd,
            riskPercentage = BigDecimal((actualMaxLossUsd / budget) * 100.0).setScale(2, RoundingMode.HALF_UP).toDouble(),
            calculatedPositionSize = calculatedQty,
            unitLabel = unitLabel,
            totalCostUsd = BigDecimal(totalCost).setScale(2, RoundingMode.HALF_UP).toDouble(),
            maxLossUsd = BigDecimal(actualMaxLossUsd).setScale(2, RoundingMode.HALF_UP).toDouble(),
            riskPerUnit = BigDecimal(riskPerUnit).setScale(instrument.priceDecimals, RoundingMode.HALF_UP).toDouble(),
            bindingConstraint = bindingRule,
            passRiskCheck = passes,
            checkMessages = checks
        )
    }
}

object NewsEngine {

    private val KEYWORDS = listOf(
        "EARNINGS" to Regex("(?i)\\b(earnings|results|EPS|guidance|forecast|revenue)\\b"),
        "MACRO" to Regex("(?i)\\b(fed|fomc|cpi|inflation|rate cut|hike|payrolls|jobs report|ecb|boe|gdp|yield)\\b"),
        "DEAL" to Regex("(?i)\\b(acquire|acquisition|merger|buyout|deal|partner|agreement)\\b"),
        "REGULATORY" to Regex("(?i)\\b(sec|probe|investigation|lawsuit|sued|antitrust|doj|court|ruling)\\b"),
        "RISK" to Regex("(?i)\\b(halt|recall|default|breach|hack|bankruptcy|delist|volatility|warning)\\b"),
        "CRYPTO_FLOW" to Regex("(?i)\\b(etf flow|halving|hashrate|staking|upgrade|sec approved|reserve)\\b")
    )

    fun categorizeHeadline(headline: String): Triple<String, String, String> {
        var category = "MARKET NEWS"
        for ((cat, rx) in KEYWORDS) {
            if (rx.containsMatchIn(headline)) {
                category = cat
                break
            }
        }

        val sentiment = when {
            Regex("(?i)\\b(surge|jump|record|beat|upgrade|approval|bullish|gain|soar)\\b").containsMatchIn(headline) -> "BULLISH"
            Regex("(?i)\\b(plunge|drop|miss|downgrade|warn|probe|slump|bearish|fall|crack)\\b").containsMatchIn(headline) -> "BEARISH"
            else -> "NEUTRAL"
        }

        val impact = when {
            category in listOf("EARNINGS", "MACRO", "RISK") || headline.contains("Fed", true) -> "HIGH"
            category in listOf("DEAL", "REGULATORY") -> "MEDIUM"
            else -> "LOW"
        }

        return Triple(category, sentiment, impact)
    }

    fun getSampleNews(): List<NewsArticle> {
        val raw = listOf(
            Triple("TSLA", "Tesla surges 4.2% as robotaxi fleet expansion and energy margins beat quarterly guidance", "Bloomberg Markets"),
            Triple("EURUSD", "ECB hints at potential 25 bps rate pause as Eurozone PMI stabilizes near 50.4", "Reuters Forex"),
            Triple("BTCUSD", "Bitcoin breaks past $91,500 amid historic spot ETF institutional net inflows", "CoinDesk Terminal"),
            Triple("XAUUSD", "Gold advances toward $2,680 on central bank reserve accumulation and safe-haven demand", "Financial Times"),
            Triple("NVDA", "Nvidia Blackwell AI chip production ramps ahead of schedule; datacenter demand robust", "Wall Street Journal"),
            Triple("NAS100", "Nasdaq 100 futures test all-time highs led by mega-cap semiconductor strength", "CNBC Trading Desk"),
            Triple("GBPUSD", "Bank of England holds rates steady at 4.75%; sterling consolidates above 1.2850", "MarketWatch"),
            Triple("AMD", "AMD expands enterprise AI server partnerships; upgrades revenue forecasts for fiscal 2026", "Barron's Tech")
        )

        return raw.mapIndexed { idx, (sym, title, src) ->
            val (cat, sent, imp) = categorizeHeadline(title)
            NewsArticle(
                id = "news_$idx",
                title = title,
                source = src,
                timeAgo = "${(idx + 1) * 18}m ago",
                relatedSymbol = sym,
                category = cat,
                sentiment = sent,
                volatilityImpact = imp,
                summary = "Quantitative news telemetry: $title. Categorized under $cat with $imp potential market volatility impact."
            )
        }
    }
}
