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
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsArticle
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

@Composable
fun NewsScreen(
    viewModel: TradingTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedInstrument by viewModel.selectedInstrument.collectAsState()
    val newsFeed by viewModel.newsFeed.collectAsState()

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
                Column {
                    Text(
                        text = "MARKET TELEMETRY & NEWS MONITOR",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Breaking Headlines & Macro Events",
                        color = TerminalTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            items(newsFeed) { article ->
                NewsArticleCard(article = article)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NewsArticleCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_item_${article.id}"),
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
                            .background(TerminalCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = article.relatedSymbol,
                            color = TerminalCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(TerminalSurface, RoundedCornerShape(4.dp))
                            .border(1.dp, TerminalBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = article.category,
                            color = TerminalGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Volatility Impact Pill
                val impactCol = when (article.volatilityImpact) {
                    "HIGH" -> TerminalSellRed
                    "MEDIUM" -> TerminalWaitAmber
                    else -> TerminalTextMuted
                }
                Text(
                    text = "${article.volatilityImpact} IMPACT",
                    color = impactCol,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.title,
                color = TerminalTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${article.source} • ${article.timeAgo}",
                    color = TerminalTextMuted,
                    fontSize = 11.sp
                )

                val (sentIcon, sentCol) = when (article.sentiment) {
                    "BULLISH" -> Icons.Default.TrendingUp to TerminalBuyGreen
                    "BEARISH" -> Icons.Default.TrendingDown to TerminalSellRed
                    else -> Icons.Default.TrendingFlat to TerminalTextSecondary
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = sentIcon,
                        contentDescription = null,
                        tint = sentCol,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = article.sentiment,
                        color = sentCol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
