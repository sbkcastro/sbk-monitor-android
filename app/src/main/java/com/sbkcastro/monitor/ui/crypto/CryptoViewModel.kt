package com.sbkcastro.monitor.ui.crypto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.*
import com.sbkcastro.monitor.data.repository.CryptoRepository
import kotlinx.coroutines.launch

class CryptoViewModel : ViewModel() {
    private val repository = CryptoRepository()

    private val _prices = MutableLiveData<List<TradePriceItem>>()
    val prices: LiveData<List<TradePriceItem>> = _prices

    private val _status = MutableLiveData<TradeStatusResponse>()
    val status: LiveData<TradeStatusResponse> = _status

    private val _portfolio = MutableLiveData<TradePortfolioResponse>()
    val portfolio: LiveData<TradePortfolioResponse> = _portfolio

    private val _risk = MutableLiveData<TradeRiskResponse>()
    val risk: LiveData<TradeRiskResponse> = _risk

    private val _trades = MutableLiveData<List<TradeItem>>()
    val trades: LiveData<List<TradeItem>> = _trades

    private val _activeTrades = MutableLiveData<List<TradeItem>>()
    val activeTrades: LiveData<List<TradeItem>> = _activeTrades

    private val _signals = MutableLiveData<TradeSignalsResponse>()
    val signals: LiveData<TradeSignalsResponse> = _signals

    private val _worldmonitor = MutableLiveData<WorldmonitorAnalysis>()
    val worldmonitor: LiveData<WorldmonitorAnalysis> = _worldmonitor

    private val _worldmonitorHistory = MutableLiveData<List<WorldmonitorAnalysis>>()
    val worldmonitorHistory: LiveData<List<WorldmonitorAnalysis>> = _worldmonitorHistory

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loadingDashboard = MutableLiveData(false)
    val loadingDashboard: LiveData<Boolean> = _loadingDashboard

    private val _loadingTrades = MutableLiveData(false)
    val loadingTrades: LiveData<Boolean> = _loadingTrades

    private val _loadingSignals = MutableLiveData(false)
    val loadingSignals: LiveData<Boolean> = _loadingSignals

    private val _loadingWorldmonitor = MutableLiveData(false)
    val loadingWorldmonitor: LiveData<Boolean> = _loadingWorldmonitor

    fun loadDashboard() {
        viewModelScope.launch {
            _loadingDashboard.value = true
            try {
                _prices.value = repository.getPrices()
                _status.value = repository.getStatus()
                _portfolio.value = repository.getPortfolio()
                _risk.value = repository.getRisk()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando dashboard"
            } finally {
                _loadingDashboard.value = false
            }
        }
    }

    fun loadTrades() {
        viewModelScope.launch {
            _loadingTrades.value = true
            try {
                _trades.value = repository.getTrades().trades
                _activeTrades.value = repository.getActiveTrades()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando trades"
            } finally {
                _loadingTrades.value = false
            }
        }
    }

    fun loadSignals() {
        viewModelScope.launch {
            _loadingSignals.value = true
            try {
                _signals.value = repository.getSignals()
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando signals"
            } finally {
                _loadingSignals.value = false
            }
        }
    }

    fun loadWorldmonitor() {
        viewModelScope.launch {
            _loadingWorldmonitor.value = true
            try {
                _worldmonitor.value = repository.getWorldmonitor()
                _worldmonitorHistory.value = repository.getWorldmonitorHistory(12)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error cargando worldmonitor"
            } finally {
                _loadingWorldmonitor.value = false
            }
        }
    }
}
