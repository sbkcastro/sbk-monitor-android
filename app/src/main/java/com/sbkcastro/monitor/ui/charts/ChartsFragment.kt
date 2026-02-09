package com.sbkcastro.monitor.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.sbkcastro.monitor.databinding.FragmentChartsBinding

class ChartsFragment : Fragment() {

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChartsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ChartsViewModel::class.java]

        setupCharts()
        observeData()

        return binding.root
    }

    private fun setupCharts() {
        // Configurar gráfico de CPU
        binding.cpuChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }
            xAxis.setDrawGridLines(false)
            legend.isEnabled = true
        }

        // Configurar gráfico de RAM
        binding.ramChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }
            xAxis.setDrawGridLines(false)
            legend.isEnabled = true
        }

        // Configurar gráfico de Disco
        binding.diskChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 100f
            }
            xAxis.setDrawGridLines(false)
            legend.isEnabled = true
        }
    }

    private fun observeData() {
        viewModel.metricsHistory.observe(viewLifecycleOwner) { metrics ->
            // Actualizar gráfico de CPU
            val cpuEntries = metrics.mapIndexed { index, metric ->
                Entry(index.toFloat(), metric.cpuUsage)
            }
            updateChart(binding.cpuChart, cpuEntries, "CPU %", Color.rgb(66, 165, 245))

            // Actualizar gráfico de RAM
            val ramEntries = metrics.mapIndexed { index, metric ->
                Entry(index.toFloat(), metric.ramUsage)
            }
            updateChart(binding.ramChart, ramEntries, "RAM %", Color.rgb(102, 187, 106))

            // Actualizar gráfico de Disco
            val diskEntries = metrics.mapIndexed { index, metric ->
                Entry(index.toFloat(), metric.diskUsage)
            }
            updateChart(binding.diskChart, diskEntries, "Disco %", Color.rgb(255, 167, 38))
        }
    }

    private fun updateChart(
        chart: com.github.mikephil.charting.charts.LineChart,
        entries: List<Entry>,
        label: String,
        color: Int
    ) {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2f
            setCircleColor(color)
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
        }

        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
