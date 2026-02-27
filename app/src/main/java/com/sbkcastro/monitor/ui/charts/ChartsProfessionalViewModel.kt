package com.sbkcastro.monitor.ui.charts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sbkcastro.monitor.api.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChartsProfessionalViewModel(application: Application) : AndroidViewModel(application) {

    private val _metrics = MutableLiveData<List<MetricPoint>>()
    val metrics: LiveData<List<MetricPoint>> = _metrics

    private val _timeRange = MutableLiveData(TimeRange.FOUR_HOURS)
    val timeRange: LiveData<TimeRange> = _timeRange

    private val _fetchStatus = MutableLiveData<FetchStatus>()
    val fetchStatus: LiveData<FetchStatus> = _fetchStatus

    sealed class FetchStatus {
        object Loading : FetchStatus()
        object Success : FetchStatus()
        data class Error(val message: String, val isAuthError: Boolean = false) : FetchStatus()
    }

    init {
        fetchHistory()
        startAutoUpdate()
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        fetchHistory()
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _fetchStatus.value = FetchStatus.Loading

            try {
                val range = _timeRange.value ?: TimeRange.FOUR_HOURS
                val rangeParam = when (range) {
                    TimeRange.ONE_HOUR -> "1h"
                    TimeRange.FOUR_HOURS -> "4h"
                    TimeRange.TWENTY_FOUR_HOURS -> "24h"
                }

                val apiService = ApiClient.getService()
                val response = apiService.getMetricsHistory(rangeParam)

                val points = response.points.map { p ->
                    MetricPoint(
                        timestamp = p.ts,
                        cpuUsage = p.cpu.toFloat(),
                        ramUsage = p.ram.toFloat(),
                        diskUsage = p.disk.toFloat()
                    )
                }

                _metrics.value = points
                _fetchStatus.value = FetchStatus.Success

            } catch (e: Exception) {
                android.util.Log.e("ChartsProfessional", "Error fetching history: ${e.message}", e)

                val isAuthError = e.message?.contains("401") == true ||
                                  e.message?.contains("Unauthorized") == true

                val errorMsg = when {
                    isAuthError -> "Error de autenticación. Cierra sesión y vuelve a iniciar."
                    e.message?.contains("timeout") == true -> "Timeout conectando al servidor."
                    e.message?.contains("Unable to resolve host") == true -> "Sin conexión a internet."
                    else -> "Error obteniendo historial: ${e.message}"
                }

                _fetchStatus.value = FetchStatus.Error(errorMsg, isAuthError)
            }
        }
    }

    private fun startAutoUpdate() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 minutos
                fetchHistory()
            }
        }
    }

    fun getMetricsCount(): Int = _metrics.value?.size ?: 0
}
