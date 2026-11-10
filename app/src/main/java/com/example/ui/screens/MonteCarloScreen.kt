package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.HistogramDistributionChart
import com.example.ui.components.MetricCard
import com.example.ui.components.MonteCarloFanChart
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
import com.example.ui.theme.TerminalWaitAmber
import com.example.ui.viewmodel.TradingTerminalViewModel
import java.util.Locale

@Composable
fun MonteCarloScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val signal by viewModel.currentSignal.collectAsState()
    val mcResult by viewModel.monteCarloResult.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val replaysCount by viewModel.mcReplaysCount.collectAsState()

    LaunchedEffect(selectedInstrument) {
        if (mcResult == null) {
            viewModel.runMonteCarlo(5000)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TIME MACHINE REPLAY ENGINE",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Monte Carlo Replay",
                        color = TerminalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1000, 5000, 10000).forEach { count ->
                        val isSelected = replaysCount == count
                        Box(
                            modifier = Modifier
                                .testTag("mc_replays_$count")
                                .background(if (isSelected) TerminalCyan else TerminalSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) TerminalCyan else TerminalBorder, RoundedCornerShape(6.dp))
                                .clickable { viewModel.runMonteCarlo(count) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${count / 1000}k",
                                color = if (isSelected) Color.Black else TerminalTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Honest Verdict Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("mc_verdict_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EMPIRICAL OUTCOME VERDICT",
                        color = TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mcResult?.verdict ?: "Calculating simulation paths...",
                        color = TerminalCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mcResult?.verdictDetail ?: "Walking 60 session bars based on historical volatility pools.",
                        color = TerminalTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Odds Breakdown Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "P(Target)",
                    value = "${String.format(Locale.US, "%.1f", (mcResult?.pTarget ?: 0.0) * 100)}%",
                    subValue = "Target First",
                    highlightColor = TerminalBuyGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "P(Stop)",
                    value = "${String.format(Locale.US, "%.1f", (mcResult?.pStop ?: 0.0) * 100)}%",
                    subValue = "Stop First",
                    highlightColor = TerminalSellRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "P(Neither)",
                    value = "${String.format(Locale.US, "%.1f", (mcResult?.pNeither ?: 0.0) * 100)}%",
                    subValue = "Open at Close",
                    highlightColor = TerminalWaitAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "P(Green)",
                    value = "${String.format(Locale.US, "%.1f", (mcResult?.pGreen ?: 0.0) * 100)}%",
                    subValue = "Closed in Profit",
                    highlightColor = TerminalBuyGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Expected R",
                    value = "${mcResult?.avgR ?: 0.0}R",
                    subValue = "Mathematical Edge",
                    highlightColor = TerminalGold,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Median Close",
                    value = "$${mcResult?.medianClose ?: 0.0}",
                    subValue = "50th Percentile",
                    highlightColor = TerminalTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Percentile Fan Band Chart
            Text(
                text = "PERCENTILE FAN BANDS (5%, 25%, 50%, 75%, 95%)",
                color = TerminalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            MonteCarloFanChart(
                fanBands = mcResult?.fanBands ?: emptyList(),
                entryPrice = signal?.entryPrice ?: 100.0,
                stopPrice = signal?.stopLossPrice ?: 98.0,
                targetPrice = signal?.takeProfitPrice ?: 104.0
            )

            // Terminal Distribution Histogram
            Text(
                text = "TERMINAL OUTCOME HISTOGRAM (24 BINS)",
                color = TerminalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )

            HistogramDistributionChart(
                bins = mcResult?.terminalHistogram ?: emptyList(),
                loPrice = mcResult?.histLo ?: 0.0,
                hiPrice = mcResult?.histHi ?: 0.0
            )

            // Transparency & Disclaimer Note
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TerminalTextMuted,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Honest Simulation Notice: The Time Machine replays recent 5-minute volatility paths. It is not a guaranteed prediction or promise of future performance.",
                    color = TerminalTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
