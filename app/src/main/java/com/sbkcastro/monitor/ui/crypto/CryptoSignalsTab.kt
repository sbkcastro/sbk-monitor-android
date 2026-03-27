package com.sbkcastro.monitor.ui.crypto

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.sbkcastro.monitor.R

class CryptoSignalsTab : Fragment() {

    private val viewModel: CryptoViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_crypto_signals, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeSignals)
        val tvBotState = view.findViewById<TextView>(R.id.tvSignalBotState)
        val tvStrategy = view.findViewById<TextView>(R.id.tvSignalStrategy)
        val tvActiveCount = view.findViewById<TextView>(R.id.tvSignalActiveCount)
        val llSignalsList = view.findViewById<LinearLayout>(R.id.llSignalsList)
        val tvNoSignals = view.findViewById<TextView>(R.id.tvNoSignals)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        swipe.setOnRefreshListener { viewModel.loadSignals() }

        viewModel.loadingSignals.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
        }

        viewModel.signals.observe(viewLifecycleOwner) { signals ->
            tvError.visibility = View.GONE
            tvBotState.text = signals.bot_state ?: "unknown"
            tvStrategy.text = signals.strategy ?: "—"
            tvActiveCount.text = "${signals.active_signals ?: 0} activas"

            llSignalsList.removeAllViews()
            val trades = signals.trades ?: emptyList()
            if (trades.isEmpty()) {
                tvNoSignals.visibility = View.VISIBLE
            } else {
                tvNoSignals.visibility = View.GONE
                for (trade in trades) {
                    val tv = TextView(requireContext()).apply {
                        text = "${trade.pair ?: "—"} | P&L: ${String.format("%+.2f%%", (trade.profit ?: 0.0) * 100)}"
                        textSize = 14f
                        setPadding(0, 4, 0, 4)
                        val profitColor = if ((trade.profit ?: 0.0) >= 0) "#4CAF50" else "#F44336"
                        setTextColor(Color.parseColor(profitColor))
                    }
                    llSignalsList.addView(tv)
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.signals.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadSignals()
    }
}
