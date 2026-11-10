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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.TradeJournalEntity
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
import com.example.ui.theme.TerminalWaitAmber
import com.example.ui.viewmodel.TradingTerminalViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val trades by viewModel.journalTrades.collectAsState()
    val context = LocalContext.current

    val closedTrades = trades.filter { it.status != "OPEN" }
    val totalTradesCount = closedTrades.size
    val winningTrades = closedTrades.filter { (it.pnlUsd ?: 0.0) > 0 }
    val losingTrades = closedTrades.filter { (it.pnlUsd ?: 0.0) < 0 }

    val winRate = if (totalTradesCount > 0) (winningTrades.size.toDouble() / totalTradesCount) * 100.0 else 0.0
    val totalPnlUsd = closedTrades.sumOf { it.pnlUsd ?: 0.0 }
    val grossWin = winningTrades.sumOf { it.pnlUsd ?: 0.0 }
    val grossLoss = kotlin.math.abs(losingTrades.sumOf { it.pnlUsd ?: 0.0 })
    val profitFactor = if (grossLoss > 0) grossWin / grossLoss else if (grossWin > 0) 99.0 else 0.0

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
                Column {
                    Text(
                        text = "DISCIPLINED EXECUTION LOG",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Trading Journal",
                        color = TerminalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Journal Performance Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Win Rate",
                        value = "${String.format(Locale.US, "%.1f", winRate)}%",
                        subValue = "$totalTradesCount Closed Trades",
                        highlightColor = TerminalBuyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Net PnL",
                        value = "${if (totalPnlUsd >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", totalPnlUsd)}",
                        subValue = "Realized",
                        highlightColor = if (totalPnlUsd >= 0) TerminalBuyGreen else TerminalSellRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Profit Factor",
                        value = String.format(Locale.US, "%.2f", profitFactor),
                        subValue = "Gains / Losses",
                        highlightColor = TerminalGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "RECORDED TRADES (${trades.size})",
                    color = TerminalTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (trades.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = TerminalTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No trades recorded yet", color = TerminalTextMuted, fontSize = 14.sp)
                            Text("Save signals from the Signal tab or execute paper orders", color = TerminalTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(trades) { trade ->
                    JournalTradeCard(
                        trade = trade,
                        onCloseAtTarget = {
                            viewModel.closeTradeInJournal(trade, "CLOSED_TARGET", trade.takeProfit)
                            Toast.makeText(context, "Marked trade as Target Hit (+2R)", Toast.LENGTH_SHORT).show()
                        },
                        onCloseAtStop = {
                            viewModel.closeTradeInJournal(trade, "CLOSED_STOP", trade.stopLoss)
                            Toast.makeText(context, "Marked trade as Stopped Out (-1R)", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            viewModel.deleteJournalTrade(trade)
                            Toast.makeText(context, "Trade entry deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun JournalTradeCard(
    trade: TradeJournalEntity,
    onCloseAtTarget: () -> Unit,
    onCloseAtStop: () -> Unit,
    onDelete: () -> Unit
) {
    val isOpen = trade.status == "OPEN"
    val isLong = trade.action == "BUY"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("journal_trade_${trade.id}"),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(if (isLong) TerminalBuyGreen.copy(alpha = 0.15f) else TerminalSellRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trade.action,
                            color = if (isLong) TerminalBuyGreen else TerminalSellRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trade.symbol,
                        color = TerminalTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${trade.positionSize} ${trade.unitLabel}",
                        color = TerminalTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Status or PnL Pill
                if (isOpen) {
                    Box(
                        modifier = Modifier
                            .background(TerminalCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("OPEN TRADE", color = TerminalCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val pnl = trade.pnlUsd ?: 0.0
                    val isProfit = pnl >= 0
                    Box(
                        modifier = Modifier
                            .background(if (isProfit) TerminalBuyGreen.copy(alpha = 0.15f) else TerminalSellRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${if (isProfit) "+" else ""}$${String.format(Locale.US, "%.2f", pnl)} (${String.format(Locale.US, "%.1f", trade.pnlR ?: 0.0)}R)",
                            color = if (isProfit) TerminalBuyGreen else TerminalSellRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Entry: $${trade.entryPrice} • Stop: $${trade.stopLoss} • Target: $${trade.takeProfit}",
                color = TerminalTextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            if (trade.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trade.notes,
                    color = TerminalTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(trade.timestamp))
                Text(
                    text = dateStr,
                    color = TerminalTextMuted,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isOpen) {
                        Button(
                            onClick = onCloseAtTarget,
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalBuyGreen, contentColor = Color.Black),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Hit Target", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onCloseAtStop,
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalSellRed, contentColor = Color.White),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Stopped", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Trade",
                            tint = TerminalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
