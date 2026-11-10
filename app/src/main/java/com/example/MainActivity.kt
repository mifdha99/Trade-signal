package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.MonteCarloScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.RiskScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SignalScreen
import com.example.ui.theme.DayTraderTheme
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalTextMuted
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.viewmodel.TradingTerminalViewModel

enum class TerminalScreen(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    SCANNER("Scanner", Icons.Default.Search, "tab_scanner"),
    SIGNAL("Signal", Icons.Default.CandlestickChart, "tab_signal"),
    RISK("Risk", Icons.Default.Shield, "tab_risk"),
    BACKTEST("Backtest", Icons.Default.History, "tab_backtest"),
    MONTE_CARLO("Time Machine", Icons.Default.QueryStats, "tab_monte_carlo"),
    NEWS("News", Icons.Default.Article, "tab_news"),
    ALERTS("Alerts", Icons.Default.Notifications, "tab_alerts"),
    JOURNAL("Journal", Icons.Default.Bookmark, "tab_journal")
}

class MainActivity : ComponentActivity() {

    private val viewModel: TradingTerminalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayTraderTheme {
                MainTerminalApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainTerminalApp(viewModel: TradingTerminalViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf(TerminalScreen.DASHBOARD) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = currentScreen.ordinal,
                containerColor = TerminalSurface,
                contentColor = TerminalCyan,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentScreen.ordinal]),
                        height = 3.dp,
                        color = TerminalCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = TerminalBorder)
            ) {
                TerminalScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    Tab(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        modifier = Modifier.testTag(screen.tag),
                        text = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) TerminalCyan else TerminalTextSecondary
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) TerminalCyan else TerminalTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBackground)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                TerminalScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSignal = { currentScreen = TerminalScreen.SIGNAL },
                    onNavigateToRisk = { currentScreen = TerminalScreen.RISK },
                    onNavigateToJournal = { currentScreen = TerminalScreen.JOURNAL }
                )
                TerminalScreen.SCANNER -> ScannerScreen(
                    viewModel = viewModel,
                    onSelectAndNavigate = { currentScreen = TerminalScreen.SIGNAL }
                )
                TerminalScreen.SIGNAL -> SignalScreen(
                    viewModel = viewModel,
                    onNavigateToRisk = { currentScreen = TerminalScreen.RISK },
                    onNavigateToJournal = { currentScreen = TerminalScreen.JOURNAL }
                )
                TerminalScreen.RISK -> RiskScreen(
                    viewModel = viewModel
                )
                TerminalScreen.BACKTEST -> BacktestScreen(
                    viewModel = viewModel
                )
                TerminalScreen.MONTE_CARLO -> MonteCarloScreen(
                    viewModel = viewModel
                )
                TerminalScreen.NEWS -> NewsScreen(
                    viewModel = viewModel
                )
                TerminalScreen.ALERTS -> AlertsScreen(
                    viewModel = viewModel
                )
                TerminalScreen.JOURNAL -> JournalScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
