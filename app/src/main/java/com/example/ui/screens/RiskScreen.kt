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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MetricCard
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
fun RiskScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val riskAnalysis by viewModel.riskAnalysis.collectAsState()
    val budget by viewModel.accountBudget.collectAsState()
    val maxLoss by viewModel.maxLossPerTrade.collectAsState()
    val maxDailyLoss by viewModel.maxDailyLoss.collectAsState()

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
                        text = "RISK & POSITION SIZING",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Budget & Exposure Control",
                        color = TerminalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (riskAnalysis?.passRiskCheck == true) TerminalBuyGreen.copy(alpha = 0.15f) else TerminalSellRed.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (riskAnalysis?.passRiskCheck == true) TerminalBuyGreen else TerminalSellRed,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (riskAnalysis?.passRiskCheck == true) "PASS" else "BLOCK",
                        color = if (riskAnalysis?.passRiskCheck == true) TerminalBuyGreen else TerminalSellRed,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Sized Position Result Hero Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("risk_position_hero_card"),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "RECOMMENDED POSITION SIZE",
                        color = TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${riskAnalysis?.calculatedPositionSize ?: 0.0}",
                            color = TerminalCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = riskAnalysis?.unitLabel ?: "units",
                            color = TerminalTextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Binding Constraint:",
                            color = TerminalTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = riskAnalysis?.bindingConstraint ?: "Max Loss",
                            color = TerminalGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Capital Required:",
                            color = TerminalTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", riskAnalysis?.totalCostUsd ?: 0.0)}",
                            color = TerminalTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Max Loss at Stop:",
                            color = TerminalTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", riskAnalysis?.maxLossUsd ?: 0.0)} (${riskAnalysis?.riskPercentage ?: 0.0}%)",
                            color = TerminalSellRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Budget Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCOUNT BUDGET & RISK LIMITS",
                        color = TerminalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account Capital: $$budget", color = TerminalTextPrimary, fontSize = 13.sp)
                        Text("Max Loss: $$maxLoss", color = TerminalSellRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = budget.toFloat(),
                        onValueChange = {
                            viewModel.accountBudget.value = it.toDouble()
                            viewModel.recalculateRisk()
                        },
                        valueRange = 500f..50000f,
                        steps = 99,
                        colors = SliderDefaults.colors(thumbColor = TerminalCyan, activeTrackColor = TerminalCyan),
                        modifier = Modifier.testTag("budget_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Loss per Trade: $$maxLoss", color = TerminalTextPrimary, fontSize = 13.sp)
                        Text("${String.format(Locale.US, "%.1f", (maxLoss / budget) * 100)}% of Capital", color = TerminalTextSecondary, fontSize = 12.sp)
                    }

                    Slider(
                        value = maxLoss.toFloat(),
                        onValueChange = {
                            viewModel.maxLossPerTrade.value = it.toDouble()
                            viewModel.recalculateRisk()
                        },
                        valueRange = 10f..500f,
                        steps = 49,
                        colors = SliderDefaults.colors(thumbColor = TerminalSellRed, activeTrackColor = TerminalSellRed),
                        modifier = Modifier.testTag("max_loss_slider")
                    )
                }
            }

            // Pre-Trade Risk Verification Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = TerminalCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PRE-TRADE RISK AUDIT",
                            color = TerminalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    riskAnalysis?.checkMessages?.forEach { msg ->
                        val isPass = msg.startsWith("PASS")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                                contentDescription = null,
                                tint = if (isPass) TerminalBuyGreen else TerminalSellRed,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = if (isPass) TerminalTextPrimary else TerminalSellRed,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
