package com.sbkcastro.monitor.data.repository

import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.TradeHealthResponse
import com.sbkcastro.monitor.api.TradePriceItem
import com.sbkcastro.monitor.api.TradeStatusResponse
import com.sbkcastro.monitor.api.TradeSignalsResponse
import com.sbkcastro.monitor.api.TradePortfolioResponse
import com.sbkcastro.monitor.api.TradeItem
import com.sbkcastro.monitor.api.TradesListResponse
import com.sbkcastro.monitor.api.TradeRiskResponse
import com.sbkcastro.monitor.api.WorldmonitorAnalysis

class CryptoRepository {
    private val api = ApiClient.getService()

    suspend fun getHealth(): TradeHealthResponse = api.getTradeHealth()

    suspend fun getPrices(): List<TradePriceItem> = api.getTradePrices()

    suspend fun getStatus(): TradeStatusResponse = api.getTradeStatus()

    suspend fun getSignals(): TradeSignalsResponse = api.getTradeSignals()

    suspend fun getPortfolio(): TradePortfolioResponse = api.getTradePortfolio()

    suspend fun getTrades(): TradesListResponse = api.getTradeTrades()

    suspend fun getActiveTrades(): List<TradeItem> = api.getTradeTradesActive()

    suspend fun getRisk(): TradeRiskResponse = api.getTradeRisk()

    suspend fun getWorldmonitor(): WorldmonitorAnalysis = api.getTradeWorldmonitor()

    suspend fun getWorldmonitorHistory(limit: Int = 24): List<WorldmonitorAnalysis> =
        api.getTradeWorldmonitorHistory(limit)
}
