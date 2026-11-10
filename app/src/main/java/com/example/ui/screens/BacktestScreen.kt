package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BacktestTrade
import com.example.ui.components.MetricCard
import com.example.ui.components.TerminalHeader
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBuyGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGold
import com.example.ui.theme.TerminalSellRed
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.viewmodel.TradingTerminalViewModel
import java.util.Locale

@Composable
fun BacktestScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val backtestResult by viewModel.backtestResult.collectAsState()
    val isBacktesting by viewModel.isBacktesting.collectAsState()

    LaunchedEffect(selectedInstrument) {
        if (backtestResult == null || backtestResult?.instrumentSymbol != selectedInstrument.symbol) {
            viewModel.runBacktest()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        TerminalHeader(
            selectedInstrument = selectedInstrument,
            instruments = viewModel.supportedInstruments,
            onSelectInstrument = { viewModel.selectInstrument(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HISTORICAL STRATEGY REPLAY",
                            color = TerminalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Backtesting Engine",
                            color = TerminalTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Button(
                        onClick = { viewModel.runBacktest() },
                        enabled = !isBacktesting,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("run_backtest_btn")
                    ) {
                        if (isBacktesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Strategy Specs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Strategy: 15-Minute Opening Range Breakout (ORB)",
                            color = TerminalTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stop: Range Midpoint | Target: 2.0R Risk Multiple | Sessions Tested: ${backtestResult?.totalSessions ?: 30}",
                            color = TerminalTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // High Level Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Win Rate",
                        value = "${backtestResult?.winRatePct ?: 0.0}%",
                        subValue = "${backtestResult?.totalTrades ?: 0} Trades",
                        highlightColor = TerminalBuyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Profit Factor",
                        value = "${backtestResult?.profitFactor ?: 0.0}",
                        subValue = "Gains / Losses",
                        highlightColor = TerminalCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Max Drawdown",
                        value = "-${backtestResult?.maxDrawdownPct ?: 0.0}R",
                        subValue = "Peak to Trough",
                        highlightColor = TerminalSellRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Net Profit",
                        value = "+${backtestResult?.netProfitR ?: 0.0}R",
                        subValue = "R-Multiples",
                        highlightColor = TerminalBuyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Trade",
                        value = "+${backtestResult?.avgRPerTrade ?: 0.0}R",
                        subValue = "Expectancy",
                        highlightColor = TerminalGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "HISTORICAL EXECUTED TRADES",
                    color = TerminalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val trades = backtestResult?.trades ?: emptyList()
            if (trades.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No trades triggered in test sample", color = TerminalTextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(trades) { trade ->
                    HistoricalTradeCard(trade = trade, decimals = selectedInstrument.priceDecimals)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HistoricalTradeCard(
    trade: BacktestTrade,
    decimals: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${trade.id} • ${trade.date}",
                        color = TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (trade.outcome == "TARGET") TerminalBuyGreen.copy(alpha = 0.15f)
                                else if (trade.outcome == "STOP") TerminalSellRed.copy(alpha = 0.15f)
                                else TerminalCyan.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.outcome,
                            color = if (trade.outcome == "TARGET") TerminalBuyGreen
                            else if (trade.outcome == "STOP") TerminalSellRed
                            else TerminalCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Entry: $${String.format(Locale.US, "%.${decimals}f", trade.entryPrice)} → Exit: $${String.format(Locale.US, "%.${decimals}f", trade.exitPrice)}",
                    color = TerminalTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            val isPos = trade.returnR > 0
            Text(
                text = "${if (isPos) "+" else ""}${trade.returnR}R",
                color = if (isPos) TerminalBuyGreen else if (trade.returnR < 0) TerminalSellRed else TerminalTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
