package com.sbkcastro.monitor.ui.charts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class ChartsProfessionalViewModel : ViewModel() {

    private val _allMetrics = MutableLiveData<List<MetricPoint>>()
    private val _timeRange = MutableLiveData<TimeRange>(TimeRange.FOUR_HOURS)

    val filteredMetrics: LiveData<List<MetricPoint>> = _timeRange.map { range ->
        filterMetricsByTimeRange(_allMetrics.value ?: emptyList(), range)
    }

    init {
        // Inicializar con datos vacíos
        _allMetrics.value = emptyList()
    }

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    fun addMetric(cpu: Float, ram: Float, disk: Float) {
        val currentList = _allMetrics.value ?: emptyList()
        val newMetric = MetricPoint(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpu,
            ramUsage = ram,
            diskUsage = disk
        )

        // Mantener últimas 24 horas de datos (asumiendo 1 punto cada 5 minutos = 288 puntos)
        val maxDataPoints = 288
        val updatedList = (currentList + newMetric).takeLast(maxDataPoints)

        _allMetrics.value = updatedList

        // Forzar actualización de filteredMetrics
        _timeRange.value = _timeRange.value
    }

    private fun filterMetricsByTimeRange(metrics: List<MetricPoint>, range: TimeRange): List<MetricPoint> {
        if (metrics.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val cutoffTime = now - (range.minutes * 60 * 1000)

        return metrics.filter { it.timestamp >= cutoffTime }
    }

    fun loadSampleData() {
        // Generar datos de muestra para testing
        val sampleMetrics = mutableListOf<MetricPoint>()
        val now = System.currentTimeMillis()

        // Generar 288 puntos (24 horas, cada 5 minutos)
        for (i in 287 downTo 0) {
            val timestamp = now - (i * 5 * 60 * 1000)
            val cpu = (10..80).random().toFloat() + (0..99).random() / 100f
            val ram = (20..90).random().toFloat() + (0..99).random() / 100f
            val disk = (50..75).random().toFloat() + (0..99).random() / 100f

            sampleMetrics.add(MetricPoint(timestamp, cpu, ram, disk))
        }

        _allMetrics.value = sampleMetrics
    }

    fun clearHistory() {
        _allMetrics.value = emptyList()
    }

    fun getMetricsCount(): Int {
        return _allMetrics.value?.size ?: 0
    }
}
