package com.example.domain

import com.example.data.model.Candle
import com.example.data.model.MarketInstrument
import com.example.data.model.ScannerItem
import com.example.data.model.SetupType
import com.example.data.model.SignalAction
import kotlin.math.max

object ScannerEngine {

    /**
     * Wilder's Smoothed RSI calculation on closes.
     * Preserves the exact algorithm from trader-scanner/scan.py:
     * ag = (ag * (n - 1) + g[i]) / n
     * al = (al * (n - 1) + lo[i]) / n
     * rsi = 100 - 100 / (1 + ag / al)
     */
    fun calculateWilderRsi(closes: List<Double>, period: Int = 14): Double? {
        if (closes.size < period + 1) return null

        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()

        for (i in 1 until closes.size) {
            val change = closes[i] - closes[i - 1]
            gains.add(max(0.0, change))
            losses.add(max(0.0, -change))
        }

        var ag = gains.take(period).sum() / period
        var al = losses.take(period).sum() / period

        for (i in period until gains.size) {
            ag = (ag * (period - 1) + gains[i]) / period
            al = (al * (period - 1) + losses[i]) / period
        }

        return if (al == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + ag / al))
    }

    /**
     * Analyzes daily or aggregated bars to identify active setups.
     * Preserves the exact 5 setup conditions from scan.py:
     * 1. breakout_20d: last close > max(c[-21:-1])
     * 2. bull_flag: run (c[-6]/c[-16] - 1) > 0.10 && range5 (max(c[-5:]) - min(c[-5:]))/last < 0.05
     * 3. reclaim_ma20: c[-2] < ma20_prev && last > ma20
     * 4. oversold: rsi < 30
     * 5. oversold_bounce: rsi_prev < 30 && last > c[-2]
     */
    fun evaluateDailySetups(closes: List<Double>): Pair<List<SetupType>, Double?> {
        if (closes.size < 22) return emptyList<SetupType>() to null

        val last = closes.last()
        val prev = closes[closes.size - 2]

        // 20-day high (excluding today's close)
        val hi20 = closes.subList(closes.size - 21, closes.size - 1).maxOrNull() ?: last

        // 20-day moving averages
        val ma20 = closes.takeLast(20).average()
        val ma20Prev = closes.subList(closes.size - 21, closes.size - 1).average()

        val setups = mutableListOf<SetupType>()

        // 1. 20-day Breakout
        if (last > hi20) {
            setups.add(SetupType.BREAKOUT_20D)
        }

        // 2. Bull Flag: requires at least 16 bars
        if (closes.size >= 16) {
            val cMinus6 = closes[closes.size - 6]
            val cMinus16 = closes[closes.size - 16]
            val run = (cMinus6 / cMinus16) - 1.0

            val last5 = closes.takeLast(5)
            val range5 = (last5.maxOrNull()!! - last5.minOrNull()!!) / last

            if (run > 0.10 && range5 < 0.05) {
                setups.add(SetupType.BULL_FLAG)
            }
        }

        // 3. Reclaim MA20
        if (prev < ma20Prev && last > ma20) {
            setups.add(SetupType.RECLAIM_MA20)
        }

        // 4 & 5. RSI Wilder
        val rsiCurrent = calculateWilderRsi(closes, 14)
        val rsiPrev = calculateWilderRsi(closes.dropLast(1), 14)

        if (rsiCurrent != null && rsiCurrent < 30.0) {
            setups.add(SetupType.OVERSOLD)
        }
        if (rsiPrev != null && rsiPrev < 30.0 && last > prev) {
            setups.add(SetupType.OVERSOLD_BOUNCE)
        }

        return setups to rsiCurrent
    }

    /**
     * Opening Range Breakout (ORB) on 5-minute bars.
     * The first n completed bars form the range (e.g. 3 bars = 15 minutes).
     * The setup triggers when a subsequent bar closes above range high (or below range low).
     */
    fun checkOrbSetup(bars5m: List<Candle>, orBarsCount: Int = 3): Pair<SetupType?, SignalAction> {
        if (bars5m.size <= orBarsCount) return null to SignalAction.WAIT

        val orBars = bars5m.take(orBarsCount)
        val orHigh = orBars.maxOf { it.high }
        val orLow = orBars.minOf { it.low }

        val subsequentBars = bars5m.drop(orBarsCount)
        for (bar in subsequentBars) {
            if (bar.close > orHigh) {
                return SetupType.ORB to SignalAction.BUY
            } else if (bar.close < orLow) {
                return SetupType.ORB to SignalAction.SELL
            }
        }
        return null to SignalAction.WAIT
    }

    /**
     * Scans an instrument and computes ranking score.
     */
    fun scanInstrument(
        instrument: MarketInstrument,
        bars5m: List<Candle>,
        dailyCloses: List<Double>
    ): ScannerItem {
        val currentPrice = bars5m.lastOrNull()?.close ?: dailyCloses.lastOrNull() ?: 0.0
        val prevClose = dailyCloses.dropLast(1).lastOrNull() ?: currentPrice
        val changePct = if (prevClose > 0) ((currentPrice - prevClose) / prevClose) * 100.0 else 0.0
        val totalVolume = bars5m.sumOf { it.volume }

        val (dailySetups, rsi) = evaluateDailySetups(dailyCloses)
        val (orbSetup, orbAction) = checkOrbSetup(bars5m, 3)

        val allSetups = mutableListOf<SetupType>()
        allSetups.addAll(dailySetups)
        if (orbSetup != null && !allSetups.contains(orbSetup)) {
            allSetups.add(orbSetup)
        }

        val primaryAction = when {
            orbAction != SignalAction.WAIT -> orbAction
            allSetups.contains(SetupType.BREAKOUT_20D) || allSetups.contains(SetupType.BULL_FLAG) || allSetups.contains(SetupType.OVERSOLD_BOUNCE) -> SignalAction.BUY
            allSetups.contains(SetupType.OVERSOLD) -> SignalAction.WAIT
            else -> SignalAction.WAIT
        }

        // Rank score: combination of setup count, volume magnitude, and momentum
        val setupWeight = allSetups.size * 25.0
        val momentumWeight = kotlin.math.abs(changePct) * 10.0
        val rankScore = setupWeight + momentumWeight

        return ScannerItem(
            instrument = instrument,
            currentPrice = currentPrice,
            change24hPct = changePct,
            volume = totalVolume,
            matchedSetups = allSetups,
            rsi14 = rsi ?: 50.0,
            primarySignal = primaryAction,
            rankScore = rankScore
        )
    }
}
