package com.sbkcastro.monitor.ui.crypto

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R

class CryptoDashboardTab : Fragment() {

    private val viewModel: CryptoViewModel by activityViewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { viewModel.loadDashboard() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_crypto_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeDashboard)
        val tvBtcPrice = view.findViewById<TextView>(R.id.tvBtcPrice)
        val tvEthPrice = view.findViewById<TextView>(R.id.tvEthPrice)
        val tvBotState = view.findViewById<TextView>(R.id.tvBotState)
        val tvStrategy = view.findViewById<TextView>(R.id.tvStrategy)
        val tvTradingMode = view.findViewById<TextView>(R.id.tvTradingMode)
        val tvDailyPnl = view.findViewById<TextView>(R.id.tvDailyPnl)
        val tvDrawdown = view.findViewById<TextView>(R.id.tvDrawdown)
        val tvBalance = view.findViewById<TextView>(R.id.tvBalance)
        val tvRiskStatus = view.findViewById<TextView>(R.id.tvRiskStatus)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        swipe.setOnRefreshListener { viewModel.loadDashboard() }

        viewModel.loadingDashboard.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
            if (!loading) scheduleRefresh()
        }

        viewModel.prices.observe(viewLifecycleOwner) { prices ->
            tvError.visibility = View.GONE
            val btc = prices.find { it.symbol == "BTCUSDT" }
            val eth = prices.find { it.symbol == "ETHUSDT" }
            tvBtcPrice.text = String.format("$%,.2f", btc?.price ?: 0.0)
            tvEthPrice.text = String.format("$%,.2f", eth?.price ?: 0.0)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            tvBotState.text = status.state ?: "unknown"
            tvStrategy.text = status.strategy ?: "—"
            val mode = if (status.dry_run == true) "DRY RUN" else "LIVE"
            tvTradingMode.text = mode
            tvTradingMode.setTextColor(if (status.dry_run == true) Color.parseColor("#FF9800") else Color.parseColor("#F44336"))
        }

        viewModel.risk.observe(viewLifecycleOwner) { risk ->
            val pnl = risk.dailyPnl ?: 0.0
            tvDailyPnl.text = String.format("%.2f%%", pnl)
            tvDailyPnl.setTextColor(if (pnl >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

            val dd = risk.drawdown ?: 0.0
            tvDrawdown.text = String.format("%.2f%%", dd)
            tvDrawdown.setTextColor(if (dd >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

            tvBalance.text = String.format("$%,.2f", risk.balance ?: 0.0)
            tvRiskStatus.text = risk.status?.uppercase() ?: "—"
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.prices.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadDashboard()
    }

    private fun scheduleRefresh() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, 30_000)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(refreshRunnable)
        super.onDestroyView()
    }
}
