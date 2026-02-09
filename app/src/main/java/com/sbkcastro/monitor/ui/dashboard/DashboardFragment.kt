package com.sbkcastro.monitor.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.sbkcastro.monitor.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            viewModel.loadMetrics()
            handler.postDelayed(this, 30000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMetrics()
            viewModel.loadServices()
        }

        viewModel.metrics.observe(viewLifecycleOwner) { metrics ->
            binding.apply {
                cpuProgress.progress = metrics.cpu.usage.toInt()
                cpuText.text = "${metrics.cpu.usage}% (${metrics.cpu.cores} cores)"

                ramProgress.progress = metrics.memory.usagePercent.toInt()
                val ramUsedGB = metrics.memory.used / (1024.0 * 1024 * 1024)
                val ramTotalGB = metrics.memory.total / (1024.0 * 1024 * 1024)
                ramText.text = "${metrics.memory.usagePercent}% (${String.format("%.1f", ramUsedGB)}/${String.format("%.1f", ramTotalGB)} GB)"

                diskProgress.progress = metrics.disk.usagePercent.toInt()
                val diskUsedGB = metrics.disk.used / (1024.0 * 1024 * 1024)
                val diskTotalGB = metrics.disk.total / (1024.0 * 1024 * 1024)
                diskText.text = "${metrics.disk.usagePercent}% (${String.format("%.1f", diskUsedGB)}/${String.format("%.1f", diskTotalGB)} GB)"

                swapProgress.progress = metrics.swap.usagePercent.toInt()
                swapText.text = "${metrics.swap.usagePercent}%"

                val days = metrics.uptime / 86400
                val hours = (metrics.uptime % 86400) / 3600
                val mins = (metrics.uptime % 3600) / 60
                uptimeText.text = "${days}d ${hours}h ${mins}m"

                loadText.text = metrics.loadAvg.joinToString(" / ") { String.format("%.2f", it) }
                hostnameText.text = metrics.hostname
            }
        }

        viewModel.services.observe(viewLifecycleOwner) { servicesResp ->
            val sb = StringBuilder()
            servicesResp.services.forEach { svc ->
                val icon = if (svc.up) "\u2705" else "\u274C"
                sb.appendLine("$icon ${svc.name} (${svc.status})")
            }
            binding.servicesText.text = sb.toString().trim()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.errorText.visibility = if (error != null) View.VISIBLE else View.GONE
            binding.errorText.text = error ?: ""
        }

        viewModel.loadMetrics()
        viewModel.loadServices()
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(refreshRunnable, 30000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
