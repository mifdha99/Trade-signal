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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.AssetClass
import com.example.data.model.SignalAction
import com.example.ui.components.CandlestickChart
import com.example.ui.components.MetricCard
import com.example.ui.components.SignalActionBadge
import com.example.ui.components.TerminalHeader
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBuyGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGold
import com.example.ui.theme.TerminalSellRed
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.viewmodel.TradingTerminalViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TradingTerminalViewModel,
    onNavigateToSignal: () -> Unit,
    onNavigateToRisk: () -> Unit,
    onNavigateToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val quote by viewModel.currentQuote.collectAsState()
    val bars by viewModel.currentBars5m.collectAsState()
    val signal by viewModel.currentSignal.collectAsState()

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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Asset Class Fast Switcher
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AssetClass.values()) { assetClass ->
                        val isSelected = selectedInstrument.assetClass == assetClass
                        Box(
                            modifier = Modifier
                                .testTag("asset_filter_${assetClass.name}")
                                .background(
                                    if (isSelected) TerminalCyan.copy(alpha = 0.2f) else TerminalSurface,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) TerminalCyan else TerminalBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    val firstOfClass = viewModel.supportedInstruments.firstOrNull { it.assetClass == assetClass }
                                    if (firstOfClass != null) viewModel.selectInstrument(firstOfClass)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${assetClass.icon} ${assetClass.displayName}",
                                color = if (isSelected) TerminalCyan else TerminalTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Market Price & Telemetry Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("market_price_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedInstrument.name,
                                    color = TerminalTextSecondary,
                                    fontSize = 12.sp
                                )
                                val priceFormatted = String.format(
                                    Locale.US,
                                    "%.${selectedInstrument.priceDecimals}f",
                                    quote?.price ?: 0.0
                                )
                                Text(
                                    text = "$$priceFormatted",
                                    color = TerminalTextPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            val change = quote?.change24hPct ?: 0.0
                            val isPos = change >= 0
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isPos) TerminalBuyGreen.copy(alpha = 0.15f) else TerminalSellRed.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", change)}%",
                                    color = if (isPos) TerminalBuyGreen else TerminalSellRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Data Source & Latency Note
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Data Info",
                                tint = TerminalTextMuted,
                                modifier = Modifier.padding(end = 4.dp).height(12.dp)
                            )
                            Text(
                                text = "${quote?.dataSource ?: "Interbank Feed"} • ${if (quote?.isDelayed == true) "15m Delayed" else "Real-Time"} • Last Sync: ${quote?.updateTimestamp ?: "--:--"}",
                                color = TerminalTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // 5-Minute Candlestick Chart
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "5-MIN SESSION BARS (ORB ENGINE)",
                            color = TerminalTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("● Target", color = TerminalBuyGreen, fontSize = 10.sp)
                            Text("● Entry", color = TerminalCyan, fontSize = 10.sp)
                            Text("● Stop", color = TerminalSellRed, fontSize = 10.sp)
                        }
                    }

                    CandlestickChart(
                        bars = bars,
                        entryPrice = signal?.entryPrice,
                        stopPrice = signal?.stopLossPrice,
                        targetPrice = signal?.takeProfitPrice
                    )
                }
            }

            // Signal Snapshot Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("signal_snapshot_card"),
                    colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "QUANTITATIVE SIGNAL",
                                    color = TerminalTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = signal?.setup?.displayName ?: "Opening-Range Breakout",
                                    color = TerminalTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            SignalActionBadge(action = signal?.action ?: SignalAction.WAIT)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = signal?.rationale ?: "Analyzing market structure...",
                            color = TerminalTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Metric Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                title = "Entry",
                                value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.entryPrice ?: 0.0),
                                highlightColor = TerminalCyan,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Stop Loss",
                                value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.stopLossPrice ?: 0.0),
                                highlightColor = TerminalSellRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Take Profit",
                                value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.takeProfitPrice ?: 0.0),
                                highlightColor = TerminalBuyGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToSignal,
                                modifier = Modifier.weight(1f).testTag("view_signal_details_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.CandlestickChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Full Signal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onNavigateToRisk,
                                modifier = Modifier.weight(1f).testTag("size_position_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalTextPrimary)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Risk Sizing", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Summary Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "R:R Ratio",
                        value = "${signal?.riskRewardRatio ?: 2.0}R",
                        subValue = "Calculated Bracket",
                        highlightColor = TerminalCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Confidence",
                        value = "${signal?.confidenceScore ?: 68}%",
                        subValue = "Historical Edge",
                        highlightColor = TerminalBuyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Volume X",
                        value = "${String.format(Locale.US, "%.1f", signal?.volumeSurgeMultiplier ?: 1.2)}x",
                        subValue = "vs Range Avg",
                        highlightColor = TerminalGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
