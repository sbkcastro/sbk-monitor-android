package com.sbkcastro.monitor.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.LineChart
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

    // Timestamps array for X axis formatting
    private var currentTimestamps: List<Long> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChartsProfessionalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupTimeRangeSelector()
        observeData()
        setupErrorHandling()

        binding.fabRefresh.setOnClickListener {
            viewModel.fetchHistory()
        }
    }

    private fun setupErrorHandling() {
        viewModel.fetchStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is ChartsProfessionalViewModel.FetchStatus.Loading -> {
                    binding.errorCard.visibility = View.GONE
                }
                is ChartsProfessionalViewModel.FetchStatus.Success -> {
                    binding.errorCard.visibility = View.GONE
                }
                is ChartsProfessionalViewModel.FetchStatus.Error -> {
                    binding.errorMessage.text = if (status.isAuthError) {
                        "${status.message}\n\n💡 Sugerencia: Ve a Configuración → Cerrar sesión"
                    } else {
                        status.message
                    }
                    binding.errorCard.visibility = View.VISIBLE
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            viewModel.fetchHistory()
        }
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

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#E0E0E0")
                    textColor = Color.parseColor("#666666")
                    valueFormatter = TimeAxisFormatter()
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.parseColor("#E0E0E0")
                    textColor = Color.parseColor("#666666")
                    axisMinimum = 0f
                    axisMaximum = 100f
                    valueFormatter = PercentFormatter()
                }

                axisRight.isEnabled = false

                legend.apply {
                    textColor = Color.parseColor("#666666")
                    textSize = 11f
                }

                animateX(500)
            }
        }
    }

    private fun setupTimeRangeSelector() {
        binding.timeRangeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val range = when (checkedId) {
                    binding.btn1h.id -> TimeRange.ONE_HOUR
                    binding.btn4h.id -> TimeRange.FOUR_HOURS
                    binding.btn24h.id -> TimeRange.TWENTY_FOUR_HOURS
                    else -> TimeRange.FOUR_HOURS
                }
                viewModel.setTimeRange(range)
            }
        }

        binding.btn4h.isChecked = true
    }

    private fun observeData() {
        viewModel.metrics.observe(viewLifecycleOwner) { metrics ->
            if (metrics.isEmpty()) return@observe

            // Store timestamps for X axis formatter
            currentTimestamps = metrics.map { it.timestamp }

            // Current values
            val latest = metrics.last()
            binding.currentCpu.text = String.format("%.1f", latest.cpuUsage)
            binding.currentRam.text = String.format("%.1f", latest.ramUsage)
            binding.currentDisk.text = String.format("%.1f", latest.diskUsage)

            // Update charts with index as X, real timestamps for formatting
            updateChart(binding.cpuChart, metrics.map { it.cpuUsage }, "CPU %", Color.parseColor("#42A5F5"))
            updateChart(binding.ramChart, metrics.map { it.ramUsage }, "RAM %", Color.parseColor("#66BB6A"))
            updateChart(binding.diskChart, metrics.map { it.diskUsage }, "Disco %", Color.parseColor("#FFA726"))

            // Statistics
            updateStatistics(metrics)
        }
    }

    private fun updateChart(chart: LineChart, values: List<Float>, label: String, color: Int) {
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2.5f
            setCircleColor(color)
            circleRadius = if (values.size > 60) 0f else 3f
            setDrawCircleHole(false)
            valueTextSize = 0f
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
            mode = LineDataSet.Mode.CUBIC_BEZIER
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

    /**
     * Formats X axis using real server timestamps.
     * X values are indices into currentTimestamps array.
     */
    inner class TimeAxisFormatter : ValueFormatter() {
        private val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            if (index < 0 || index >= currentTimestamps.size) return ""
            return format.format(Date(currentTimestamps[index]))
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
