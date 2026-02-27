package com.sbkcastro.monitor.ui.ids

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R
import java.text.NumberFormat
import java.util.Locale

class IDSDashboardFragment : Fragment() {

    private val viewModel: IDSDashboardViewModel by viewModels()
    private val alertAdapter = IDSAlertAdapter()
    private val numFmt = NumberFormat.getNumberInstance(Locale("es", "ES"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ids_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        view.findViewById<RecyclerView>(R.id.recyclerAlerts)?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertAdapter
        }

        // SwipeRefresh
        view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)?.setOnRefreshListener {
            viewModel.fetchAll()
        }

        // Observe IDS status
        viewModel.idsStatus.observe(viewLifecycleOwner) { status ->
            val vpn = status.lxcVpn
            val host = status.lxcHostGw

            // Coverage
            view.findViewById<TextView>(R.id.tvCoveragePercent)?.text = status.total_coverage
            view.findViewById<TextView>(R.id.tvTotalEvents)?.text = "TOTAL: ${numFmt.format(status.total_events)} eventos"

            // lxc-vpn
            view.findViewById<TextView>(R.id.tvAppsStatus)?.apply {
                text = "● ${vpn.status.uppercase()}"
                setTextColor(if (vpn.status == "active") Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            }
            view.findViewById<TextView>(R.id.tvAppsVersion)?.text = "v${vpn.version}"
            view.findViewById<TextView>(R.id.tvAppsRules)?.text = "${numFmt.format(vpn.rules)} reglas"
            view.findViewById<TextView>(R.id.tvAppsInterface)?.text = "Interface: ${vpn.iface}"
            view.findViewById<TextView>(R.id.tvAppsEvents)?.text = "${numFmt.format(vpn.events)} evt"
            view.findViewById<TextView>(R.id.tvAppsPercent)?.text = vpn.coverage.substringBefore(" ")

            // lxc-host-gw
            view.findViewById<TextView>(R.id.tvHostStatus)?.apply {
                text = "● ${host.status.uppercase()}"
                setTextColor(if (host.status == "active") Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            }
            view.findViewById<TextView>(R.id.tvHostVersion)?.text = "v${host.version}"
            view.findViewById<TextView>(R.id.tvHostRules)?.text = "${numFmt.format(host.rules)} reglas"
            view.findViewById<TextView>(R.id.tvHostInterface)?.text = "Interface: ${host.iface}"
            view.findViewById<TextView>(R.id.tvHostEvents)?.text = "${numFmt.format(host.events)} evt"
            view.findViewById<TextView>(R.id.tvHostPercent)?.text = host.coverage.substringBefore(" ")
        }

        // Observe alerts
        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            view.findViewById<TextView>(R.id.tvAlertsCount)?.text = "\uD83D\uDEA8 Alertas Recientes (${alerts.size})"
            view.findViewById<TextView>(R.id.tvNoAlerts)?.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
            alertAdapter.submitList(alerts)
        }

        // Loading
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            view.findViewById<ProgressBar>(R.id.progressBar)?.visibility = if (loading) View.VISIBLE else View.GONE
            view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)?.isRefreshing = loading
        }
    }
}
