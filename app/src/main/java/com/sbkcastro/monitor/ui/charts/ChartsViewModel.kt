package com.sbkcastro.monitor.ui.charts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MetricPoint(
    val timestamp: Long,
    val cpuUsage: Float,
    val ramUsage: Float,
    val diskUsage: Float
)

class ChartsViewModel : ViewModel() {

    private val _metricsHistory = MutableLiveData<List<MetricPoint>>()
    val metricsHistory: LiveData<List<MetricPoint>> = _metricsHistory

    private val maxDataPoints = 50 // Últimos 50 puntos (aprox. 4 horas si se actualiza cada 5min)

    init {
        // Iniciar con datos vacíos
        _metricsHistory.value = emptyList()
    }

    fun addMetric(cpu: Float, ram: Float, disk: Float) {
        val currentList = _metricsHistory.value ?: emptyList()
        val newMetric = MetricPoint(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpu,
            ramUsage = ram,
            diskUsage = disk
        )

        val updatedList = (currentList + newMetric).takeLast(maxDataPoints)
        _metricsHistory.value = updatedList
    }

    fun clearHistory() {
        _metricsHistory.value = emptyList()
    }
}
