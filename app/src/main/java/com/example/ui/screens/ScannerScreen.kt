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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssetClass
import com.example.data.model.ScannerItem
import com.example.ui.components.SignalActionBadge
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
fun ScannerScreen(
    viewModel: TradingTerminalViewModel,
    onSelectAndNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scannerItems by viewModel.scannerItems.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    var selectedFilter by remember { mutableStateOf<AssetClass?>(null) }

    val filteredItems = remember(scannerItems, selectedFilter) {
        if (selectedFilter == null) scannerItems
        else scannerItems.filter { it.instrument.assetClass == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UNIVERSAL MARKET SCANNER",
                    color = TerminalCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Setups & Opportunities",
                    color = TerminalTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                onClick = { viewModel.runFullMarketScan() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("run_scanner_btn")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Asset Class Filters
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .testTag("filter_all")
                        .background(
                            if (selectedFilter == null) TerminalCyan.copy(alpha = 0.2f) else TerminalSurface,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (selectedFilter == null) TerminalCyan else TerminalBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All Assets (${scannerItems.size})",
                        color = if (selectedFilter == null) TerminalCyan else TerminalTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selectedFilter == null) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            items(AssetClass.values()) { assetClass ->
                val isSelected = selectedFilter == assetClass
                Box(
                    modifier = Modifier
                        .testTag("filter_${assetClass.name}")
                        .background(
                            if (isSelected) TerminalCyan.copy(alpha = 0.2f) else TerminalSurface,
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) TerminalCyan else TerminalBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = assetClass }
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

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isScanning) "Scanning universal markets..." else "No instruments match criteria",
                    color = TerminalTextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredItems) { item ->
                    ScannerRowItem(
                        item = item,
                        onClick = {
                            viewModel.selectInstrument(item.instrument)
                            onSelectAndNavigate()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerRowItem(
    item: ScannerItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanner_item_${item.instrument.symbol}")
            .clickable { onClick() },
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
                    Text(item.instrument.assetClass.icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.instrument.symbol,
                            color = TerminalTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = item.instrument.name,
                            color = TerminalTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val priceFormatted = String.format(
                        Locale.US,
                        "%.${item.instrument.priceDecimals}f",
                        item.currentPrice
                    )
                    Text(
                        text = "$$priceFormatted",
                        color = TerminalTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    val isPos = item.change24hPct >= 0
                    Text(
                        text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", item.change24hPct)}%",
                        color = if (isPos) TerminalBuyGreen else TerminalSellRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Setups badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(item.matchedSetups) { setup ->
                        Box(
                            modifier = Modifier
                                .background(TerminalCyan.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = setup.displayName,
                                color = TerminalCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    if (item.matchedSetups.isEmpty()) {
                        item {
                            Text(
                                text = "Consolidating",
                                color = TerminalTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                SignalActionBadge(action = item.primarySignal)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Wilder RSI(14): ${String.format(Locale.US, "%.1f", item.rsi14)}",
                    color = if (item.rsi14 < 30) TerminalBuyGreen else if (item.rsi14 > 70) TerminalSellRed else TerminalTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Rank Score: ${String.format(Locale.US, "%.0f", item.rankScore)}",
                    color = TerminalGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
