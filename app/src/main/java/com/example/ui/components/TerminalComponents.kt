package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.data.model.MarketInstrument
import com.example.data.model.SignalAction
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBuyGreen
import com.example.ui.theme.TerminalBuyGreenBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalCyanBg
import com.example.ui.theme.TerminalGold
import com.example.ui.theme.TerminalSellRed
import com.example.ui.theme.TerminalSellRedBg
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceVariant
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.theme.TerminalWaitAmber
import com.example.ui.theme.TerminalWaitAmberBg

@Composable
fun TerminalHeader(
    selectedInstrument: MarketInstrument,
    instruments: List<MarketInstrument>,
    onSelectInstrument: (MarketInstrument) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TerminalBuyGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "QUANT ENGINE v2.0",
                    color = TerminalBuyGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "DAY TRADER AI",
                color = TerminalTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .testTag("instrument_selector_btn")
                    .background(TerminalSurfaceVariant, RoundedCornerShape(8.dp))
                    .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedInstrument.assetClass.icon,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = selectedInstrument.symbol,
                        color = TerminalTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = selectedInstrument.assetClass.displayName,
                        color = TerminalTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Instrument",
                    tint = TerminalCyan
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(TerminalSurface)
            ) {
                instruments.forEach { inst ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inst.assetClass.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = inst.symbol,
                                        color = TerminalTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = inst.name,
                                        color = TerminalTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectInstrument(inst)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SignalActionBadge(
    action: SignalAction,
    modifier: Modifier = Modifier,
    sizeLarge: Boolean = false
) {
    val (bgColor, textColor, borderCol, label) = when (action) {
        SignalAction.BUY -> Quad(TerminalBuyGreenBg, TerminalBuyGreen, TerminalBuyGreen, "BUY NOW")
        SignalAction.SELL -> Quad(TerminalSellRedBg, TerminalSellRed, TerminalSellRed, "SELL NOW")
        SignalAction.WAIT -> Quad(TerminalWaitAmberBg, TerminalWaitAmber, TerminalWaitAmber, "WAIT")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .padding(
                horizontal = if (sizeLarge) 16.dp else 10.dp,
                vertical = if (sizeLarge) 8.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (sizeLarge) 18.sp else 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CandlestickChart(
    bars: List<Candle>,
    stopPrice: Double? = null,
    targetPrice: Double? = null,
    entryPrice: Double? = null,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(184.dp)) {
            val high = bars.maxOf { it.high }.let { if (targetPrice != null) maxOf(it, targetPrice) else it }
            val low = bars.minOf { it.low }.let { if (stopPrice != null) minOf(it, stopPrice) else it }
            val range = (high - low).coerceAtLeast(0.0001)

            fun getY(price: Double): Float {
                return (size.height - ((price - low) / range * size.height)).toFloat().coerceIn(0f, size.height)
            }

            // Draw horizontal grid lines
            val gridCount = 4
            for (g in 0..gridCount) {
                val y = size.height * (g.toFloat() / gridCount)
                drawLine(
                    color = TerminalBorder,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Draw horizontal stop / target / entry lines if specified
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            if (targetPrice != null) {
                val ty = getY(targetPrice)
                drawLine(
                    color = TerminalBuyGreen,
                    start = Offset(0f, ty),
                    end = Offset(size.width, ty),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }

            if (stopPrice != null) {
                val sy = getY(stopPrice)
                drawLine(
                    color = TerminalSellRed,
                    start = Offset(0f, sy),
                    end = Offset(size.width, sy),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }

            if (entryPrice != null) {
                val ey = getY(entryPrice)
                drawLine(
                    color = TerminalCyan,
                    start = Offset(0f, ey),
                    end = Offset(size.width, ey),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }

            // Draw candlesticks
            val candleWidth = (size.width / bars.size).coerceAtLeast(4f)
            bars.forEachIndexed { index, bar ->
                val x = index * candleWidth + (candleWidth / 2f)
                val openY = getY(bar.open)
                val closeY = getY(bar.close)
                val highY = getY(bar.high)
                val lowY = getY(bar.low)

                val isBullish = bar.close >= bar.open
                val candleColor = if (isBullish) TerminalBuyGreen else TerminalSellRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY)
                val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(x - (candleWidth * 0.35f), bodyTop),
                    size = Size(candleWidth * 0.7f, bodyHeight)
                )
            }
        }
    }
}

@Composable
fun MonteCarloFanChart(
    fanBands: List<List<Double>>,
    entryPrice: Double,
    stopPrice: Double,
    targetPrice: Double,
    modifier: Modifier = Modifier
) {
    if (fanBands.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(184.dp)) {
            val allVals = fanBands.flatten() + listOf(entryPrice, stopPrice, targetPrice)
            val maxP = allVals.maxOrNull() ?: entryPrice
            val minP = allVals.minOrNull() ?: entryPrice
            val range = (maxP - minP).coerceAtLeast(0.0001)

            fun getY(price: Double): Float {
                return (size.height - ((price - minP) / range * size.height)).toFloat().coerceIn(0f, size.height)
            }

            val stepWidth = size.width / (fanBands.size - 1).coerceAtLeast(1)

            // Draw outer fan band (p5 to p95)
            val outerPath = Path()
            fanBands.forEachIndexed { i, b ->
                val x = i * stepWidth
                val y = getY(b[4]) // p95
                if (i == 0) outerPath.moveTo(x, y) else outerPath.lineTo(x, y)
            }
            for (i in fanBands.indices.reversed()) {
                val x = i * stepWidth
                val y = getY(fanBands[i][0]) // p5
                outerPath.lineTo(x, y)
            }
            outerPath.close()
            drawPath(outerPath, color = TerminalCyan.copy(alpha = 0.15f))

            // Draw inner fan band (p25 to p75)
            val innerPath = Path()
            fanBands.forEachIndexed { i, b ->
                val x = i * stepWidth
                val y = getY(b[3]) // p75
                if (i == 0) innerPath.moveTo(x, y) else innerPath.lineTo(x, y)
            }
            for (i in fanBands.indices.reversed()) {
                val x = i * stepWidth
                val y = getY(fanBands[i][1]) // p25
                innerPath.lineTo(x, y)
            }
            innerPath.close()
            drawPath(innerPath, color = TerminalCyan.copy(alpha = 0.3f))

            // Median line (p50)
            val medianPath = Path()
            fanBands.forEachIndexed { i, b ->
                val x = i * stepWidth
                val y = getY(b[2]) // p50
                if (i == 0) medianPath.moveTo(x, y) else medianPath.lineTo(x, y)
            }
            drawPath(medianPath, color = TerminalCyan, style = Stroke(width = 2.5f))

            // Target and Stop lines
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawLine(TerminalBuyGreen, Offset(0f, getY(targetPrice)), Offset(size.width, getY(targetPrice)), strokeWidth = 2f, pathEffect = dash)
            drawLine(TerminalSellRed, Offset(0f, getY(stopPrice)), Offset(size.width, getY(stopPrice)), strokeWidth = 2f, pathEffect = dash)
        }
    }
}

@Composable
fun HistogramDistributionChart(
    bins: List<Int>,
    loPrice: Double,
    hiPrice: Double,
    modifier: Modifier = Modifier
) {
    if (bins.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(114.dp)) {
            val maxFreq = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
            val barW = size.width / bins.size

            bins.forEachIndexed { idx, count ->
                val barH = (count.toFloat() / maxFreq) * size.height
                val x = idx * barW
                val y = size.height - barH

                drawRect(
                    color = TerminalCyan.copy(alpha = 0.65f),
                    topLeft = Offset(x + 1f, y),
                    size = Size(barW - 2f, barH)
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subValue: String? = null,
    highlightColor: Color = TerminalTextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(TerminalSurface, RoundedCornerShape(8.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                color = TerminalTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = highlightColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    color = TerminalTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
