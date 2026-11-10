package com.example.domain

import com.example.data.model.Candle
import com.example.data.model.MarketInstrument
import com.example.data.model.SetupType
import com.example.data.model.SignalAction
import com.example.data.model.TradingSignal
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object EntryEngine {

    data class EntryEvaluation(
        val status: String, // "Triggered", "Waiting", "Forming", "Expired", "Closed"
        val action: SignalAction,
        val entryPrice: Double,
        val triggerBarTime: String,
        val orHigh: Double,
        val orLow: Double,
        val orMid: Double,
        val avgOrVolume: Double,
        val triggerVolumeMultiplier: Double,
        val reason: String
    )

    fun evaluateSessionEntry(
        bars5m: List<Candle>,
        orMinutes: Int = 15,
        minVolumeMultiplier: Double = 1.0,
        isShortAllowed: Boolean = true
    ): EntryEvaluation {
        val orBarCount = orMinutes / 5
        val sdf = SimpleDateFormat("HH:mm", Locale.US)

        if (bars5m.isEmpty()) {
            return EntryEvaluation(
                status = "Waiting",
                action = SignalAction.WAIT,
                entryPrice = 0.0,
                triggerBarTime = "--:--",
                orHigh = 0.0,
                orLow = 0.0,
                orMid = 0.0,
                avgOrVolume = 0.0,
                triggerVolumeMultiplier = 0.0,
                reason = "No session bar data received yet"
            )
        }

        if (bars5m.size < orBarCount) {
            val lastBar = bars5m.last()
            return EntryEvaluation(
                status = "Forming",
                action = SignalAction.WAIT,
                entryPrice = lastBar.close,
                triggerBarTime = sdf.format(Date(lastBar.timestamp)),
                orHigh = bars5m.maxOf { it.high },
                orLow = bars5m.minOf { it.low },
                orMid = (bars5m.maxOf { it.high } + bars5m.minOf { it.low }) / 2.0,
                avgOrVolume = bars5m.map { it.volume }.average(),
                triggerVolumeMultiplier = 1.0,
                reason = "Opening range forming: ${bars5m.size} of $orBarCount bars completed"
            )
        }

        val orBars = bars5m.take(orBarCount)
        val orHigh = orBars.maxOf { it.high }
        val orLow = orBars.minOf { it.low }
        val orMid = (orHigh + orLow) / 2.0
        val avgOrVol = orBars.map { it.volume.toDouble() }.average().coerceAtLeast(1.0)

        val subsequentBars = bars5m.drop(orBarCount)

        for (bar in subsequentBars) {
            val volRatio = bar.volume / avgOrVol
            val volPass = volRatio >= minVolumeMultiplier

            // Long breakout
            if (bar.close > orHigh && volPass) {
                return EntryEvaluation(
                    status = "Triggered",
                    action = SignalAction.BUY,
                    entryPrice = bar.close,
                    triggerBarTime = sdf.format(Date(bar.timestamp)),
                    orHigh = orHigh,
                    orLow = orLow,
                    orMid = orMid,
                    avgOrVolume = avgOrVol,
                    triggerVolumeMultiplier = volRatio,
                    reason = "5-min candle closed above opening range high ($orHigh) with ${String.format(Locale.US, "%.1fx", volRatio)} volume"
                )
            }

            // Short breakdown
            if (isShortAllowed && bar.close < orLow && volPass) {
                return EntryEvaluation(
                    status = "Triggered",
                    action = SignalAction.SELL,
                    entryPrice = bar.close,
                    triggerBarTime = sdf.format(Date(bar.timestamp)),
                    orHigh = orHigh,
                    orLow = orLow,
                    orMid = orMid,
                    avgOrVolume = avgOrVol,
                    triggerVolumeMultiplier = volRatio,
                    reason = "5-min candle closed below opening range low ($orLow) with ${String.format(Locale.US, "%.1fx", volRatio)} volume"
                )
            }
        }

        // If no breakout has occurred yet
        val latest = bars5m.last()
        return EntryEvaluation(
            status = "Waiting",
            action = SignalAction.WAIT,
            entryPrice = latest.close,
            triggerBarTime = sdf.format(Date(latest.timestamp)),
            orHigh = orHigh,
            orLow = orLow,
            orMid = orMid,
            avgOrVolume = avgOrVol,
            triggerVolumeMultiplier = latest.volume / avgOrVol,
            reason = "Range established ($orLow - $orHigh). Price ${latest.close} is awaiting breakout confirmation"
        )
    }
}

object ExitEngine {

    data class ExitPlan(
        val entryPrice: Double,
        val stopLoss: Double,
        val takeProfit: Double,
        val riskPerUnit: Double,
        val rewardRiskRatio: Double,
        val stopRuleLabel: String,
        val targetRuleLabel: String,
        val isValid: Boolean,
        val rejectionReason: String? = null
    )

    /**
     * Calculates 14-period True Range / Average True Range on session bars
     */
    fun calculateAtr(bars: List<Candle>, period: Int = 14): Double {
        if (bars.size < 2) return 0.0
        val trs = mutableListOf<Double>()
        for (i in 1 until bars.size) {
            val h = bars[i].high
            val l = bars[i].low
            val prevC = bars[i - 1].close
            val tr = max(h - l, max(abs(h - prevC), abs(l - prevC)))
            trs.add(tr)
        }
        val count = trs.size.coerceAtMost(period)
        return trs.takeLast(count).average()
    }

    /**
     * Computes stop and target prices preserving the original repository rules:
     * - Stops: or-mid, or-low, atr:K, pct:X, fixed price
     * - Target: 1R, 1.5R, 2R, 3R, or fixed price
     * - Enforces minimum risk >= 0.05% of entry price to survive market spread
     */
    fun buildExitPlan(
        instrument: MarketInstrument,
        action: SignalAction,
        entryPrice: Double,
        orHigh: Double,
        orLow: Double,
        bars: List<Candle>,
        stopRule: String = "or_mid",
        stopParam: Double = 1.5,
        targetR: Double = 2.0
    ): ExitPlan {
        val decimals = instrument.priceDecimals
        val isLong = action == SignalAction.BUY

        var rawStop = when (stopRule) {
            "or_mid" -> (orHigh + orLow) / 2.0
            "or_low" -> if (isLong) orLow else orHigh
            "atr" -> {
                val atr = calculateAtr(bars, 14).coerceAtLeast(entryPrice * 0.001)
                if (isLong) entryPrice - (stopParam * atr) else entryPrice + (stopParam * atr)
            }
            "pct" -> {
                val frac = stopParam / 100.0
                if (isLong) entryPrice * (1.0 - frac) else entryPrice * (1.0 + frac)
            }
            else -> if (isLong) orLow else orHigh
        }

        // Round stop away from entry (down for longs, up for shorts)
        val stopBd = BigDecimal(rawStop).setScale(decimals, if (isLong) RoundingMode.FLOOR else RoundingMode.CEILING)
        val stopLoss = stopBd.toDouble()

        val risk = if (isLong) entryPrice - stopLoss else stopLoss - entryPrice

        // Minimum risk check: at least 0.05% of price
        val minRiskThreshold = entryPrice * 0.0005
        if (risk < minRiskThreshold) {
            return ExitPlan(
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = entryPrice,
                riskPerUnit = risk,
                rewardRiskRatio = 0.0,
                stopRuleLabel = stopRule,
                targetRuleLabel = "${targetR}R",
                isValid = false,
                rejectionReason = "Risk ($risk) is under minimum spread noise threshold (0.05% = $minRiskThreshold). Widen stop."
            )
        }

        // Calculate Target: entry + R * risk (or entry - R * risk for short)
        val rawTarget = if (isLong) entryPrice + (targetR * risk) else entryPrice - (targetR * risk)
        val targetBd = BigDecimal(rawTarget).setScale(decimals, RoundingMode.HALF_UP)
        val takeProfit = targetBd.toDouble()

        val reward = if (isLong) takeProfit - entryPrice else entryPrice - takeProfit
        val rrRatio = if (risk > 0) reward / risk else 0.0

        val stopLabel = when (stopRule) {
            "or_mid" -> "Opening Range Midpoint"
            "or_low" -> if (isLong) "Opening Range Low" else "Opening Range High"
            "atr" -> "${stopParam}x ATR(14) Band"
            "pct" -> "$stopParam% Dynamic Risk"
            else -> "Price Level Stop"
        }

        return ExitPlan(
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            riskPerUnit = risk,
            rewardRiskRatio = BigDecimal(rrRatio).setScale(2, RoundingMode.HALF_UP).toDouble(),
            stopRuleLabel = stopLabel,
            targetRuleLabel = "${targetR}R Bracket",
            isValid = true
        )
    }

    fun generateSignal(
        instrument: MarketInstrument,
        bars5m: List<Candle>,
        historicalConfidenceScore: Int = 62,
        isDelayed: Boolean = false,
        source: String = "Universal Live Feeds"
    ): TradingSignal {
        val entryEval = EntryEngine.evaluateSessionEntry(bars5m, 15, 1.0, true)
        val currentPrice = bars5m.lastOrNull()?.close ?: 1.0

        val exitPlan = buildExitPlan(
            instrument = instrument,
            action = entryEval.action,
            entryPrice = if (entryEval.entryPrice > 0) entryEval.entryPrice else currentPrice,
            orHigh = entryEval.orHigh,
            orLow = entryEval.orLow,
            bars = bars5m,
            stopRule = "or_mid",
            stopParam = 1.5,
            targetR = 2.0
        )

        val timestampStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'ET'", Locale.US).format(Date())

        return TradingSignal(
            instrument = instrument,
            action = entryEval.action,
            setup = SetupType.ORB,
            status = entryEval.status,
            entryPrice = exitPlan.entryPrice,
            stopLossPrice = exitPlan.stopLoss,
            takeProfitPrice = exitPlan.takeProfit,
            stopRule = exitPlan.stopRuleLabel,
            targetR = 2.0,
            riskRewardRatio = exitPlan.rewardRiskRatio,
            confidenceScore = if (entryEval.action == SignalAction.WAIT) 0 else historicalConfidenceScore,
            volumeSurgeMultiplier = entryEval.triggerVolumeMultiplier,
            timestamp = timestampStr,
            rationale = entryEval.reason,
            isDelayed = isDelayed,
            providerSource = source,
            sessionPhase = entryEval.status
        )
    }
}
