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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PriceAlertEntity
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
fun AlertsScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val quote by viewModel.currentQuote.collectAsState()
    val alerts by viewModel.priceAlerts.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var targetPriceInput by remember { mutableStateOf("") }
    var conditionType by remember { mutableStateOf("ABOVE") }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRICE MONITOR & ALERTS",
                            color = TerminalCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Automated Thresholds",
                            color = TerminalTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Button(
                        onClick = {
                            val cur = quote?.price ?: 100.0
                            targetPriceInput = String.format(Locale.US, "%.${selectedInstrument.priceDecimals}f", cur * 1.01)
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black),
                        modifier = Modifier.testTag("add_alert_btn")
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TerminalTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No active price alerts set", color = TerminalTextMuted, fontSize = 14.sp)
                            Text("Tap 'Add Alert' to monitor breakouts or stops", color = TerminalTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(alerts) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onDelete = {
                            viewModel.deletePriceAlert(alert)
                            Toast.makeText(context, "Alert removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = TerminalSurface,
            title = {
                Text(
                    text = "New Alert: ${selectedInstrument.symbol}",
                    color = TerminalTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Price: $$${quote?.price ?: 0.0}",
                        color = TerminalTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { conditionType = "ABOVE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (conditionType == "ABOVE") TerminalBuyGreen else TerminalSurfaceVariant,
                                contentColor = if (conditionType == "ABOVE") Color.Black else TerminalTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Price Above", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { conditionType = "BELOW" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (conditionType == "BELOW") TerminalSellRed else TerminalSurfaceVariant,
                                contentColor = if (conditionType == "BELOW") Color.White else TerminalTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Price Below", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = targetPriceInput,
                        onValueChange = { targetPriceInput = it },
                        label = { Text("Target Threshold Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("alert_target_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TerminalCyan,
                            unfocusedBorderColor = TerminalBorder,
                            focusedTextColor = TerminalTextPrimary,
                            unfocusedTextColor = TerminalTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = targetPriceInput.toDoubleOrNull()
                        if (price != null && price > 0) {
                            viewModel.addPriceAlert(selectedInstrument.symbol, conditionType, price)
                            showAddDialog = false
                            Toast.makeText(context, "Price alert created", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black)
                ) {
                    Text("Save Alert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TerminalTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AlertItemCard(
    alert: PriceAlertEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorder))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alert.symbol,
                        color = TerminalTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(TerminalCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.assetClass,
                            color = TerminalCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val condColor = if (alert.conditionType == "ABOVE") TerminalBuyGreen else TerminalSellRed
                Text(
                    text = "${if (alert.conditionType == "ABOVE") "▲ Price ≥" else "▼ Price ≤"} $${alert.targetValue}",
                    color = condColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                if (alert.isTriggered) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Triggered at ${alert.triggeredAt ?: "earlier today"}",
                        color = TerminalGold,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alert.isTriggered) {
                    Box(
                        modifier = Modifier
                            .background(TerminalGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TRIGGERED", color = TerminalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(TerminalBuyGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", color = TerminalBuyGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alert",
                        tint = TerminalTextMuted
                    )
                }
            }
        }
    }
}
