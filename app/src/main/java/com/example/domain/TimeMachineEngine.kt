package com.example.domain

import com.example.data.model.BacktestResult
import com.example.data.model.BacktestTrade
import com.example.data.model.Candle
import com.example.data.model.MonteCarloResult
import com.example.data.model.SignalAction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TimeMachineEngine {

    data class CandleRatio(
        val openRatio: Double,
        val highRatio: Double,
        val lowRatio: Double,
        val closeRatio: Double
    )

    /**
     * Preserves the exact Monte Carlo replay algorithm from trader-timemachine/timemachine.py.
     * Replays remainder of session bars up to 10,000 times using historical session candle ratios.
     */
    fun runMonteCarloSimulation(
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        action: SignalAction,
        historicalSessions: List<List<Candle>>,
        replays: Int = 5000,
        seed: Long = 19L,
        barsLeftInSession: Int = 60,
        slotWindow: Int = 2
    ): MonteCarloResult {
        val isLong = action == SignalAction.BUY
        val risk = if (isLong) entryPrice - stopLoss else stopLoss - entryPrice
        val reward = if (isLong) takeProfit - entryPrice else entryPrice - takeProfit
        val rAtTarget = if (risk > 0) reward / risk else 2.0

        // Build slot pool of candle ratios from historical sessions
        // Each session has N bars. For slot i, store (o, h, l, c) / prevClose
        val sessionBarCount = 78
        val pool = Array(sessionBarCount) { mutableListOf<CandleRatio>() }

        for (session in historicalSessions) {
            for (i in 1 until min(session.size, sessionBarCount)) {
                val prev = session[i - 1].close.coerceAtLeast(0.0001)
                val cur = session[i]
                pool[i].add(
                    CandleRatio(
                        openRatio = cur.open / prev,
                        highRatio = cur.high / prev,
                        lowRatio = cur.low / prev,
                        closeRatio = cur.close / prev
                    )
                )
            }
        }

        // Fill empty slots with baseline neutral fluctuation if needed
        for (i in 0 until sessionBarCount) {
            if (pool[i].isEmpty()) {
                pool[i].add(CandleRatio(1.0, 1.002, 0.998, 1.0))
            }
        }

        val rng = Random(seed)
        var targetHits = 0
        var stopHits = 0
        var neitherHits = 0
        var greenHits = 0

        val terminalCloses = mutableListOf<Double>()
        val rMultiples = mutableListOf<Double>()

        // 1. Run main simulation loop
        for (n in 0 until replays) {
            var p = entryPrice
            var outcome: String? = null

            val startSlot = sessionBarCount - barsLeftInSession

            for (i in startSlot until sessionBarCount) {
                val jitter = if (slotWindow > 0) rng.nextInt(slotWindow * 2 + 1) - slotWindow else 0
                val slot = (i + jitter).coerceIn(1, sessionBarCount - 1)
                val candidates = pool[slot]
                val ratio = candidates[rng.nextInt(candidates.size)]

                val hi = p * ratio.highRatio
                val lo = p * ratio.lowRatio
                val cl = p * ratio.closeRatio

                if (outcome == null) {
                    if (isLong) {
                        // Conservative rule: check stop before target in the same bar
                        if (lo <= stopLoss) {
                            outcome = "stop"
                        } else if (hi >= takeProfit) {
                            outcome = "target"
                        }
                    } else {
                        if (hi >= stopLoss) {
                            outcome = "stop"
                        } else if (lo <= takeProfit) {
                            outcome = "target"
                        }
                    }
                }
                p = cl
            }

            terminalCloses.add(p)

            val finalOutcome = outcome ?: "neither"
            when (finalOutcome) {
                "target" -> {
                    targetHits++
                    rMultiples.add(rAtTarget)
                    greenHits++
                }
                "stop" -> {
                    stopHits++
                    rMultiples.add(-1.0)
                }
                else -> {
                    neitherHits++
                    val tradeR = if (risk > 0) (if (isLong) p - entryPrice else entryPrice - p) / risk else 0.0
                    rMultiples.add(tradeR)
                    if (tradeR > 0) greenHits++
                }
            }
        }

        terminalCloses.sort()
        val pTarget = targetHits.toDouble() / replays
        val pStop = stopHits.toDouble() / replays
        val pNeither = neitherHits.toDouble() / replays
        val pGreen = greenHits.toDouble() / replays

        fun percentile(list: List<Double>, q: Double): Double {
            val idx = (q * (list.size - 1)).toInt().coerceIn(0, list.size - 1)
            return list[idx]
        }

        val medianClose = percentile(terminalCloses, 0.50)
        val p5Close = percentile(terminalCloses, 0.05)
        val p95Close = percentile(terminalCloses, 0.95)
        val avgR = rMultiples.average()

        // 2. Generate Fan Chart Percentile Bands across remaining bars (using 500 sample paths)
        val fanPathsCount = 500
        val bandQuantiles = listOf(0.05, 0.25, 0.50, 0.75, 0.95)
        val stepPrices = Array(barsLeftInSession + 1) { mutableListOf<Double>() }

        for (n in 0 until fanPathsCount) {
            var curP = entryPrice
            stepPrices[0].add(curP)
            val startSlot = sessionBarCount - barsLeftInSession

            for (step in 1..barsLeftInSession) {
                val i = startSlot + step - 1
                val jitter = if (slotWindow > 0) rng.nextInt(slotWindow * 2 + 1) - slotWindow else 0
                val slot = (i + jitter).coerceIn(1, sessionBarCount - 1)
                val candidates = pool[slot]
                val ratio = candidates[rng.nextInt(candidates.size)]
                curP *= ratio.closeRatio
                stepPrices[step].add(curP)
            }
        }

        val fanBands = mutableListOf<List<Double>>()
        for (step in 0..barsLeftInSession) {
            val sortedStep = stepPrices[step].sorted()
            val bands = bandQuantiles.map { q -> percentile(sortedStep, q) }
            fanBands.add(bands)
        }

        // 3. Generate 24-bin terminal histogram
        val histBins = 24
        val lo1 = percentile(terminalCloses, 0.01)
        val hi99 = percentile(terminalCloses, 0.99)
        val binWidth = (hi99 - lo1) / histBins
        val bins = IntArray(histBins)

        for (c in terminalCloses) {
            if (binWidth > 0 && c in lo1..hi99) {
                val b = ((c - lo1) / binWidth).toInt().coerceIn(0, histBins - 1)
                bins[b]++
            }
        }

        // 4. Honest verdict calculation from repository logic
        val resolvedCount = targetHits + stopHits
        val targetShareOfResolved = if (resolvedCount > 0) targetHits.toDouble() / resolvedCount else 0.5
        val breakEvenShare = 1.0 / (1.0 + rAtTarget)
        val gap = (targetShareOfResolved - breakEvenShare) * 100.0

        val (verdict, detail) = when {
            abs(gap) < 5.0 -> {
                "Close to Break-Even / Coin Flip" to
                        "Target share (${String.format(java.util.Locale.US, "%.1f", targetShareOfResolved * 100)}%) is within 5% of break-even rate (${String.format(java.util.Locale.US, "%.1f", breakEvenShare * 100)}%). Trade relies strictly on disciplined edge."
            }
            gap >= 15.0 -> {
                "Strongly Toward Target (+${String.format(java.util.Locale.US, "%.1f", gap)}% edge)" to
                        "Historical return distribution heavily favors target completion (${String.format(java.util.Locale.US, "%.1f", pTarget * 100)}%) before stop ($pStop)."
            }
            gap > 0 -> {
                "Leans Toward Target (+${String.format(java.util.Locale.US, "%.1f", gap)}% edge)" to
                        "Simulation slightly favors target side over break-even threshold."
            }
            gap <= -15.0 -> {
                "Strongly Toward Stop (${String.format(java.util.Locale.US, "%.1f", gap)}% drag)" to
                        "Historical volatility is stopping out this setup before reaching the ${rAtTarget}R target."
            }
            else -> {
                "Leans Toward Stop (${String.format(java.util.Locale.US, "%.1f", gap)}% drag)" to
                        "Stop frequency is elevated compared to theoretical target probability."
            }
        }

        return MonteCarloResult(
            replays = replays,
            pTarget = BigDecimal(pTarget).setScale(4, RoundingMode.HALF_UP).toDouble(),
            pStop = BigDecimal(pStop).setScale(4, RoundingMode.HALF_UP).toDouble(),
            pNeither = BigDecimal(pNeither).setScale(4, RoundingMode.HALF_UP).toDouble(),
            pGreen = BigDecimal(pGreen).setScale(4, RoundingMode.HALF_UP).toDouble(),
            medianClose = BigDecimal(medianClose).setScale(2, RoundingMode.HALF_UP).toDouble(),
            p5Close = BigDecimal(p5Close).setScale(2, RoundingMode.HALF_UP).toDouble(),
            p95Close = BigDecimal(p95Close).setScale(2, RoundingMode.HALF_UP).toDouble(),
            avgR = BigDecimal(avgR).setScale(3, RoundingMode.HALF_UP).toDouble(),
            verdict = verdict,
            verdictDetail = detail,
            fanBands = fanBands,
            terminalHistogram = bins.toList(),
            histLo = lo1,
            histHi = hi99
        )
    }

    /**
     * Backtests the setup on historical full sessions.
     */
    fun backtestStrategy(
        instrumentSymbol: String,
        sessions: List<Pair<String, List<Candle>>>,
        action: SignalAction = SignalAction.BUY,
        orMinutes: Int = 15,
        targetR: Double = 2.0
    ): BacktestResult {
        val trades = mutableListOf<BacktestTrade>()
        val orBarCount = orMinutes / 5
        val isLong = action == SignalAction.BUY

        for ((idx, sessionData) in sessions.withIndex()) {
            val dateStr = sessionData.first
            val bars = sessionData.second
            if (bars.size <= orBarCount) continue

            val orBars = bars.take(orBarCount)
            val orHigh = orBars.maxOf { it.high }
            val orLow = orBars.minOf { it.low }
            val orMid = (orHigh + orLow) / 2.0

            var triggeredIndex: Int? = null
            var entry = 0.0

            for (i in orBarCount until bars.size) {
                if (isLong && bars[i].close > orHigh) {
                    triggeredIndex = i
                    entry = bars[i].close
                    break
                } else if (!isLong && bars[i].close < orLow) {
                    triggeredIndex = i
                    entry = bars[i].close
                    break
                }
            }

            if (triggeredIndex == null) continue

            val stop = if (isLong) orMid else orMid
            val risk = abs(entry - stop)
            if (risk <= 0.0) continue

            val target = if (isLong) entry + (targetR * risk) else entry - (targetR * risk)

            var outcome = "CLOSE"
            var exitPrice = bars.last().close

            for (b in bars.subList(triggeredIndex + 1, bars.size)) {
                if (isLong) {
                    if (b.low <= stop) {
                        outcome = "STOP"
                        exitPrice = stop
                        break
                    }
                    if (b.high >= target) {
                        outcome = "TARGET"
                        exitPrice = target
                        break
                    }
                } else {
                    if (b.high >= stop) {
                        outcome = "STOP"
                        exitPrice = stop
                        break
                    }
                    if (b.low <= target) {
                        outcome = "TARGET"
                        exitPrice = target
                        break
                    }
                }
            }

            val returnR = when (outcome) {
                "TARGET" -> targetR
                "STOP" -> -1.0
                else -> (if (isLong) exitPrice - entry else entry - exitPrice) / risk
            }

            trades.add(
                BacktestTrade(
                    id = idx + 1,
                    date = dateStr,
                    entryPrice = entry,
                    exitPrice = exitPrice,
                    stopPrice = stop,
                    targetPrice = target,
                    outcome = outcome,
                    returnR = BigDecimal(returnR).setScale(2, RoundingMode.HALF_UP).toDouble()
                )
            )
        }

        val totalTrades = trades.size
        val winningTrades = trades.filter { it.returnR > 0 }
        val losingTrades = trades.filter { it.returnR < 0 }

        val winRate = if (totalTrades > 0) (winningTrades.size.toDouble() / totalTrades) * 100.0 else 0.0
        val grossGainR = winningTrades.sumOf { it.returnR }
        val grossLossR = abs(losingTrades.sumOf { it.returnR })
        val profitFactor = if (grossLossR > 0) grossGainR / grossLossR else if (grossGainR > 0) 99.0 else 0.0
        val netProfitR = trades.sumOf { it.returnR }
        val avgR = if (totalTrades > 0) netProfitR / totalTrades else 0.0

        // Calculate max drawdown on cumulative R equity curve
        var peak = 0.0
        var currentEq = 0.0
        var maxDd = 0.0

        for (t in trades) {
            currentEq += t.returnR
            if (currentEq > peak) peak = currentEq
            val dd = peak - currentEq
            if (dd > maxDd) maxDd = dd
        }

        return BacktestResult(
            instrumentSymbol = instrumentSymbol,
            setupName = "15-min Opening Range Breakout (${targetR}R)",
            totalSessions = sessions.size,
            totalTrades = totalTrades,
            winRatePct = BigDecimal(winRate).setScale(1, RoundingMode.HALF_UP).toDouble(),
            profitFactor = BigDecimal(profitFactor).setScale(2, RoundingMode.HALF_UP).toDouble(),
            maxDrawdownPct = BigDecimal(maxDd).setScale(1, RoundingMode.HALF_UP).toDouble(),
            netProfitR = BigDecimal(netProfitR).setScale(2, RoundingMode.HALF_UP).toDouble(),
            avgRPerTrade = BigDecimal(avgR).setScale(2, RoundingMode.HALF_UP).toDouble(),
            trades = trades
        )
    }
}
