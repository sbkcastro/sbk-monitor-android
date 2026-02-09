package com.sbkcastro.monitor.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.sbkcastro.monitor.databinding.FragmentChartsProfessionalBinding
import java.text.SimpleDateFormat
import java.util.*

class ChartsProfessionalFragment : Fragment() {

    private var _binding: FragmentChartsProfessionalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChartsProfessionalViewModel by viewModels()

    private var selectedTimeRange = TimeRange.FOUR_HOURS

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChartsProfessionalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupTimeRangeSelector()
        observeData()

        // Botón actualizar
        binding.fabRefresh.setOnClickListener {
            viewModel.fetchRealMetrics()
        }

        // Fetch inicial de métricas reales
        viewModel.fetchRealMetrics()
    }

    private fun setupCharts() {
        listOf(binding.cpuChart, binding.ramChart, binding.diskChart).forEach { chart ->
            chart.apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)

                // Eje X (tiempo)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#E0E0E0")
                    textColor = Color.parseColor("#666666")
                    valueFormatter = TimeAxisFormatter()
                }

                // Eje Y izquierdo (porcentaje)
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#E0E0E0")
                    textColor = Color.parseColor("#666666")
                    axisMinimum = 0f
                    axisMaximum = 100f
                    valueFormatter = PercentFormatter()
                }

                // Deshabilitar eje Y derecho
                axisRight.isEnabled = false

                // Leyenda
                legend.apply {
                    textColor = Color.parseColor("#666666")
                    textSize = 11f
                }

                // Animación
                animateX(500)
            }
        }
    }

    private fun setupTimeRangeSelector() {
        binding.timeRangeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedTimeRange = when (checkedId) {
                    binding.btn1h.id -> TimeRange.ONE_HOUR
                    binding.btn4h.id -> TimeRange.FOUR_HOURS
                    binding.btn24h.id -> TimeRange.TWENTY_FOUR_HOURS
                    else -> TimeRange.FOUR_HOURS
                }
                viewModel.setTimeRange(selectedTimeRange)
            }
        }

        // Seleccionar 4h por defecto
        binding.btn4h.isChecked = true
    }

    private fun observeData() {
        viewModel.filteredMetrics.observe(viewLifecycleOwner) { metrics ->
            if (metrics.isEmpty()) return@observe

            // Actualizar valores actuales
            val latest = metrics.last()
            binding.currentCpu.text = String.format("%.1f", latest.cpuUsage)
            binding.currentRam.text = String.format("%.1f", latest.ramUsage)
            binding.currentDisk.text = String.format("%.1f", latest.diskUsage)

            // Actualizar gráficos
            updateChart(binding.cpuChart, metrics.map { it.cpuUsage }, "CPU %", Color.parseColor("#42A5F5"))
            updateChart(binding.ramChart, metrics.map { it.ramUsage }, "RAM %", Color.parseColor("#66BB6A"))
            updateChart(binding.diskChart, metrics.map { it.diskUsage }, "Disco %", Color.parseColor("#FFA726"))

            // Actualizar estadísticas
            updateStatistics(metrics)
        }
    }

    private fun updateChart(chart: com.github.mikephil.charting.charts.LineChart, values: List<Float>, label: String, color: Int) {
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2.5f
            setCircleColor(color)
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 0f  // No mostrar valores en los puntos
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER  // Línea suavizada
            cubicIntensity = 0.2f
        }

        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun updateStatistics(metrics: List<MetricPoint>) {
        val cpuAvg = metrics.map { it.cpuUsage }.average()
        val cpuMax = metrics.maxOf { it.cpuUsage }
        val ramAvg = metrics.map { it.ramUsage }.average()
        val ramMax = metrics.maxOf { it.ramUsage }
        val diskAvg = metrics.map { it.diskUsage }.average()
        val diskMax = metrics.maxOf { it.diskUsage }

        binding.cpuAvg.text = "Prom: ${String.format("%.1f", cpuAvg)}%"
        binding.cpuMax.text = "Máx: ${String.format("%.1f", cpuMax)}%"
        binding.ramAvg.text = "Prom: ${String.format("%.1f", ramAvg)}%"
        binding.ramMax.text = "Máx: ${String.format("%.1f", ramMax)}%"
        binding.diskAvg.text = "Prom: ${String.format("%.1f", diskAvg)}%"
        binding.diskMax.text = "Máx: ${String.format("%.1f", diskMax)}%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Formateadores
    class TimeAxisFormatter : ValueFormatter() {
        private val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val time = System.currentTimeMillis() - ((value.toInt()) * 60 * 1000).toLong()
            return format.format(Date(time))
        }
    }

    class PercentFormatter : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return "${value.toInt()}%"
        }
    }
}

enum class TimeRange(val minutes: Int) {
    ONE_HOUR(60),
    FOUR_HOURS(240),
    TWENTY_FOUR_HOURS(1440)
}
