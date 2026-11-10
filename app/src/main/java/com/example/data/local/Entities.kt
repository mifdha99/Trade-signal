package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_journal")
data class TradeJournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val assetClass: String,
    val action: String, // BUY, SELL
    val setup: String,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val stopLoss: Double,
    val takeProfit: Double,
    val positionSize: Double,
    val unitLabel: String,
    val status: String, // OPEN, CLOSED_TARGET, CLOSED_STOP, CLOSED_MANUAL
    val pnlUsd: Double? = null,
    val pnlR: Double? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val assetClass: String,
    val conditionType: String, // "ABOVE", "BELOW", "PCT_MOVE", "OPENING_BELL"
    val targetValue: Double,
    val isTriggered: Boolean = false,
    val triggeredAt: String? = null,
    val isEnabled: Boolean = true,
    val note: String = ""
)
