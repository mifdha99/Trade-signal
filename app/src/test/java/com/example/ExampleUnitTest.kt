package com.example

import com.example.data.model.AssetClass
import com.example.data.model.Candle
import com.example.data.model.MarketInstrument
import com.example.data.model.SetupType
import com.example.data.model.SignalAction
import com.example.domain.EntryEngine
import com.example.domain.ExitEngine
import com.example.domain.RiskEngine
import com.example.domain.ScannerEngine
import com.example.domain.TimeMachineEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testWilderRsiCalculation() {
        val closes = listOf(
            10.0, 10.5, 10.2, 10.8, 11.0, 11.2, 10.9, 11.5,
            11.8, 12.0, 12.2, 12.1, 12.5, 12.8, 13.0, 13.2
        )
        val rsi = ScannerEngine.calculateWilderRsi(closes, 14)
        assertNotNull(rsi)
        assertTrue("RSI should be above 50 on uptrend", rsi!! > 50.0)
    }

    @Test
    fun testBreakout20dDetection() {
        val closes = MutableList(25) { 100.0 + (it * 0.5) }
        // Last close is higher than all previous
        closes.add(150.0)

        val (setups, _) = ScannerEngine.evaluateDailySetups(closes)
        assertTrue("Should detect 20-day high breakout", setups.contains(SetupType.BREAKOUT_20D))
    }

    @Test
    fun testOrbEntryEvaluation() {
        val bars = listOf(
            Candle(1000L, 354.5, 360.0, 354.5, 358.0, 100000L),
            Candle(2000L, 358.0, 362.18, 356.0, 360.0, 120000L),
            Candle(3000L, 360.0, 362.0, 358.0, 359.0, 80000L),
            // Breakout bar closing above range high (362.18)
            Candle(4000L, 359.0, 364.5, 358.5, 364.18, 250000L)
        )

        val eval = EntryEngine.evaluateSessionEntry(bars, orMinutes = 15, minVolumeMultiplier = 1.0)
        assertEquals("Triggered", eval.status)
        assertEquals(SignalAction.BUY, eval.action)
        assertEquals(364.18, eval.entryPrice, 0.01)
        assertEquals(362.18, eval.orHigh, 0.01)
    }

    @Test
    fun testExitPlanCalculation() {
        val inst = MarketInstrument("TSLA", "Tesla", AssetClass.STOCKS, priceDecimals = 2)
        val exitPlan = ExitEngine.buildExitPlan(
            instrument = inst,
            action = SignalAction.BUY,
            entryPrice = 364.18,
            orHigh = 362.18,
            orLow = 354.47,
            bars = emptyList(),
            stopRule = "or_mid",
            targetR = 2.0
        )

        assertTrue(exitPlan.isValid)
        // Midpoint of 362.18 and 354.47 is 358.325 -> floor to 358.32
        assertEquals(358.32, exitPlan.stopLoss, 0.01)
        val risk = 364.18 - 358.32 // 5.86
        // 2R target = 364.18 + 2 * 5.86 = 375.90
        assertEquals(364.18 + 2.0 * 5.86, exitPlan.takeProfit, 0.01)
    }

    @Test
    fun testRiskPositionSizing() {
        val inst = MarketInstrument("TSLA", "Tesla", AssetClass.STOCKS, priceDecimals = 2)
        val risk = RiskEngine.calculatePositionSize(
            instrument = inst,
            budget = 5000.0,
            maxLossUsd = 50.0,
            entryPrice = 364.18,
            stopPrice = 358.32,
            targetPrice = 370.04
        )

        // Risk per share = 5.86. 50 / 5.86 = 8 shares ($46.88 loss)
        assertEquals(8.0, risk.calculatedPositionSize, 0.01)
        assertTrue(risk.passRiskCheck)
    }

    @Test
    fun testMonteCarloSimulation() {
        val sessions = listOf(
            listOf(
                Candle(0L, 100.0, 101.0, 99.5, 100.5, 1000L),
                Candle(300L, 100.5, 102.0, 100.2, 101.8, 1000L),
                Candle(600L, 101.8, 103.0, 101.5, 102.5, 1000L)
            )
        )
        val result = TimeMachineEngine.runMonteCarloSimulation(
            entryPrice = 100.0,
            stopLoss = 98.0,
            takeProfit = 104.0,
            action = SignalAction.BUY,
            historicalSessions = sessions,
            replays = 100
        )

        assertEquals(100, result.replays)
        assertTrue(result.pTarget >= 0.0)
        assertTrue(result.pStop >= 0.0)
        assertNotNull(result.verdict)
    }
}
