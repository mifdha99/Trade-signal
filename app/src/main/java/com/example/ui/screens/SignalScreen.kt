package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.theme.TerminalWaitAmber
import com.example.ui.viewmodel.TradingTerminalViewModel
import java.util.Locale

@Composable
fun SignalScreen(
    viewModel: TradingTerminalViewModel,
    onNavigateToRisk: () -> Unit,
    onNavigateToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val signal by viewModel.currentSignal.collectAsState()
    val bars by viewModel.currentBars5m.collectAsState()
    val context = LocalContext.current

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
            // Signal Decision Banner
            Card(
                modifier = Modifier.fillMaxWidth().testTag("signal_main_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ORDER EXECUTION ADVISORY",
                        color = TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SignalActionBadge(
                        action = signal?.action ?: SignalAction.WAIT,
                        sizeLarge = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = signal?.rationale ?: "No active setup triggered",
                        color = TerminalTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Timestamp: ${signal?.timestamp ?: "--"}",
                        color = TerminalTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Key Bracket Parameters
            Text(
                text = "BRACKET ORDER SPECIFICATION",
                color = TerminalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Entry Price",
                    value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.entryPrice ?: 0.0),
                    subValue = "Trigger Bar Close",
                    highlightColor = TerminalCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Stop Loss",
                    value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.stopLossPrice ?: 0.0),
                    subValue = signal?.stopRule ?: "OR-Midpoint",
                    highlightColor = TerminalSellRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Take Profit",
                    value = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", signal?.takeProfitPrice ?: 0.0),
                    subValue = "${signal?.targetR ?: 2.0}R Multiple",
                    highlightColor = TerminalBuyGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Candlestick Chart with Target / Stop Overlays
            CandlestickChart(
                bars = bars,
                entryPrice = signal?.entryPrice,
                stopPrice = signal?.stopLossPrice,
                targetPrice = signal?.takeProfitPrice
            )

            // Statistical Validation Matrix
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HISTORICAL VALIDATION METRICS",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk-to-Reward Ratio:", color = TerminalTextSecondary, fontSize = 13.sp)
                        Text("${signal?.riskRewardRatio ?: 2.0} : 1.0", color = TerminalTextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Setup Historical Edge:", color = TerminalTextSecondary, fontSize = 13.sp)
                        Text("${signal?.confidenceScore ?: 68}%", color = TerminalBuyGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Volume Surge Multiplier:", color = TerminalTextSecondary, fontSize = 13.sp)
                        Text("${String.format(Locale.US, "%.1f", signal?.volumeSurgeMultiplier ?: 1.2)}x OR Avg", color = TerminalGold, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Asset Specification:", color = TerminalTextSecondary, fontSize = 13.sp)
                        Text("${selectedInstrument.contractUnit} • tick ${selectedInstrument.tickSize}", color = TerminalTextPrimary, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Actions: Save to Journal / Size in Risk Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveSignalToJournal()
                        Toast.makeText(context, "Saved signal to Trading Journal", Toast.LENGTH_SHORT).show()
                        onNavigateToJournal()
                    },
                    modifier = Modifier.weight(1f).testTag("save_journal_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Journal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onNavigateToRisk,
                    modifier = Modifier.weight(1f).testTag("goto_risk_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalSurface, contentColor = TerminalTextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = TerminalCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Size Position", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
