package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PriceAlertEntity
import com.example.data.local.TradeJournalEntity
import com.example.data.model.BacktestResult
import com.example.data.model.Candle
import com.example.data.model.MarketInstrument
import com.example.data.model.MonteCarloResult
import com.example.data.model.NewsArticle
import com.example.data.model.RiskAnalysis
import com.example.data.model.ScannerItem
import com.example.data.model.TradingSignal
import com.example.data.repository.MarketDataRepository
import com.example.domain.ExitEngine
import com.example.domain.NewsEngine
import com.example.domain.RiskEngine
import com.example.domain.ScannerEngine
import com.example.domain.TimeMachineEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TradingTerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MarketDataRepository()
    private val database = AppDatabase.getDatabase(application)
    private val journalDao = database.tradeJournalDao()
    private val alertDao = database.priceAlertDao()

    val supportedInstruments = repository.supportedInstruments

    private val _selectedInstrument = MutableStateFlow(supportedInstruments.first())
    val selectedInstrument: StateFlow<MarketInstrument> = _selectedInstrument.asStateFlow()

    private val _currentQuote = MutableStateFlow<MarketDataRepository.LiveMarketQuote?>(null)
    val currentQuote: StateFlow<MarketDataRepository.LiveMarketQuote?> = _currentQuote.asStateFlow()

    private val _currentBars5m = MutableStateFlow<List<Candle>>(emptyList())
    val currentBars5m: StateFlow<List<Candle>> = _currentBars5m.asStateFlow()

    private val _currentSignal = MutableStateFlow<TradingSignal?>(null)
    val currentSignal: StateFlow<TradingSignal?> = _currentSignal.asStateFlow()

    private val _scannerItems = MutableStateFlow<List<ScannerItem>>(emptyList())
    val scannerItems: StateFlow<List<ScannerItem>> = _scannerItems.asStateFlow()
    val isScanning = MutableStateFlow(false)

    // Risk Parameters
    val accountBudget = MutableStateFlow(5000.0)
    val maxLossPerTrade = MutableStateFlow(50.0)
    val maxDailyLoss = MutableStateFlow(150.0)
    val cashBufferPct = MutableStateFlow(10.0)
    val maxPositionPct = MutableStateFlow(75.0)
    val customStopRule = MutableStateFlow("or_mid")
    val customTargetR = MutableStateFlow(2.0)

    private val _riskAnalysis = MutableStateFlow<RiskAnalysis?>(null)
    val riskAnalysis: StateFlow<RiskAnalysis?> = _riskAnalysis.asStateFlow()

    // Monte Carlo & Backtest
    private val _monteCarloResult = MutableStateFlow<MonteCarloResult?>(null)
    val monteCarloResult: StateFlow<MonteCarloResult?> = _monteCarloResult.asStateFlow()
    val isSimulating = MutableStateFlow(false)
    val mcReplaysCount = MutableStateFlow(5000)

    private val _backtestResult = MutableStateFlow<BacktestResult?>(null)
    val backtestResult: StateFlow<BacktestResult?> = _backtestResult.asStateFlow()
    val isBacktesting = MutableStateFlow(false)

    // News
    private val _newsFeed = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsFeed: StateFlow<List<NewsArticle>> = _newsFeed.asStateFlow()

    // Room Flows
    val journalTrades: StateFlow<List<TradeJournalEntity>> = journalDao.getAllTrades()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val priceAlerts: StateFlow<List<PriceAlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // API Config Settings
    val finnhubKey = MutableStateFlow("")
    val alphaVantageKey = MutableStateFlow("")

    init {
        loadInstrumentData(_selectedInstrument.value)
        runFullMarketScan()
        loadNews()
    }

    fun selectInstrument(instrument: MarketInstrument) {
        _selectedInstrument.value = instrument
        loadInstrumentData(instrument)
    }

    fun loadInstrumentData(instrument: MarketInstrument) {
        viewModelScope.launch(Dispatchers.IO) {
            val quote = repository.fetchLatestQuote(instrument)
            _currentQuote.value = quote

            val bars = repository.get5MinuteSessionBars(instrument)
            _currentBars5m.value = bars

            val signal = ExitEngine.generateSignal(
                instrument = instrument,
                bars5m = bars,
                historicalConfidenceScore = 68,
                isDelayed = quote.isDelayed,
                source = quote.dataSource
            )
            _currentSignal.value = signal

            recalculateRisk()
        }
    }

    fun runFullMarketScan() {
        viewModelScope.launch(Dispatchers.Default) {
            isScanning.value = true
            val items = mutableListOf<ScannerItem>()
            for (inst in supportedInstruments) {
                val bars = repository.get5MinuteSessionBars(inst)
                val dailyCloses = repository.getDailyCloses(inst)
                val scanned = ScannerEngine.scanInstrument(inst, bars, dailyCloses)
                items.add(scanned)
            }
            items.sortByDescending { it.rankScore }
            _scannerItems.value = items
            isScanning.value = false
        }
    }

    fun recalculateRisk() {
        val signal = _currentSignal.value ?: return
        val inst = _selectedInstrument.value

        val risk = RiskEngine.calculatePositionSize(
            instrument = inst,
            budget = accountBudget.value,
            maxLossUsd = maxLossPerTrade.value,
            entryPrice = signal.entryPrice,
            stopPrice = signal.stopLossPrice,
            targetPrice = signal.takeProfitPrice,
            cashBufferPct = cashBufferPct.value,
            maxPositionPct = maxPositionPct.value,
            minRewardRisk = 1.0,
            todayLossSoFarUsd = 0.0,
            maxDailyLossUsd = maxDailyLoss.value
        )
        _riskAnalysis.value = risk
    }

    fun runMonteCarlo(replays: Int = 5000) {
        mcReplaysCount.value = replays
        viewModelScope.launch(Dispatchers.Default) {
            isSimulating.value = true
            val inst = _selectedInstrument.value
            val signal = _currentSignal.value
            val sessions = repository.getHistoricalSessions(inst).map { it.second }

            val entry = signal?.entryPrice ?: _currentQuote.value?.price ?: 100.0
            val stop = signal?.stopLossPrice ?: (entry * 0.98)
            val target = signal?.takeProfitPrice ?: (entry * 1.04)
            val action = signal?.action ?: com.example.data.model.SignalAction.BUY

            val result = TimeMachineEngine.runMonteCarloSimulation(
                entryPrice = entry,
                stopLoss = stop,
                takeProfit = target,
                action = action,
                historicalSessions = sessions,
                replays = replays,
                seed = 19L
            )
            _monteCarloResult.value = result
            isSimulating.value = false
        }
    }

    fun runBacktest() {
        viewModelScope.launch(Dispatchers.Default) {
            isBacktesting.value = true
            val inst = _selectedInstrument.value
            val sessions = repository.getHistoricalSessions(inst)
            val signal = _currentSignal.value

            val result = TimeMachineEngine.backtestStrategy(
                instrumentSymbol = inst.symbol,
                sessions = sessions,
                action = signal?.action ?: com.example.data.model.SignalAction.BUY,
                orMinutes = 15,
                targetR = 2.0
            )
            _backtestResult.value = result
            isBacktesting.value = false
        }
    }

    fun loadNews() {
        _newsFeed.value = NewsEngine.getSampleNews()
    }

    fun saveSignalToJournal() {
        val signal = _currentSignal.value ?: return
        val risk = _riskAnalysis.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val trade = TradeJournalEntity(
                symbol = signal.instrument.symbol,
                assetClass = signal.instrument.assetClass.displayName,
                action = signal.action.name,
                setup = signal.setup.displayName,
                entryPrice = signal.entryPrice,
                stopLoss = signal.stopLossPrice,
                takeProfit = signal.takeProfitPrice,
                positionSize = risk.calculatedPositionSize,
                unitLabel = risk.unitLabel,
                status = "OPEN",
                notes = "Saved from live signal trigger. ${signal.rationale}"
            )
            journalDao.insertTrade(trade)
        }
    }

    fun closeTradeInJournal(trade: TradeJournalEntity, outcome: String, exitPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val isLong = trade.action == "BUY"
            val diff = if (isLong) exitPrice - trade.entryPrice else trade.entryPrice - exitPrice
            val pnlUsd = diff * trade.positionSize
            val riskPerUnit = kotlin.math.abs(trade.entryPrice - trade.stopLoss)
            val rMultiple = if (riskPerUnit > 0) diff / riskPerUnit else 0.0

            val updated = trade.copy(
                exitPrice = exitPrice,
                status = outcome,
                pnlUsd = pnlUsd,
                pnlR = rMultiple
            )
            journalDao.updateTrade(updated)
        }
    }

    fun deleteJournalTrade(trade: TradeJournalEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            journalDao.deleteTrade(trade)
        }
    }

    fun addPriceAlert(symbol: String, condition: String, targetPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val inst = supportedInstruments.find { it.symbol == symbol } ?: _selectedInstrument.value
            val alert = PriceAlertEntity(
                symbol = symbol,
                assetClass = inst.assetClass.displayName,
                conditionType = condition,
                targetValue = targetPrice,
                note = "Alert when $symbol crosses $targetPrice"
            )
            alertDao.insertAlert(alert)
        }
    }

    fun deletePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            alertDao.deleteAlert(alert)
        }
    }

    fun checkAndTriggerAlerts(currentPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val alerts = priceAlerts.value
            val nowTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            for (alert in alerts) {
                if (alert.isEnabled && !alert.isTriggered && alert.symbol == _selectedInstrument.value.symbol) {
                    val hit = when (alert.conditionType) {
                        "ABOVE" -> currentPrice >= alert.targetValue
                        "BELOW" -> currentPrice <= alert.targetValue
                        else -> false
                    }
                    if (hit) {
                        alertDao.setTriggered(alert.id, true, nowTime)
                    }
                }
            }
        }
    }
}
