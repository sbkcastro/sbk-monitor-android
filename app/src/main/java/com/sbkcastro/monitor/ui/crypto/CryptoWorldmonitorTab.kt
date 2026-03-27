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
import com.sbkcastro.monitor.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CryptoWorldmonitorTab : Fragment() {

    private val viewModel: CryptoViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_crypto_worldmonitor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeWorldmonitor)
        val tvSentiment = view.findViewById<TextView>(R.id.tvSentiment)
        val tvConfidence = view.findViewById<TextView>(R.id.tvConfidence)
        val tvRecommendation = view.findViewById<TextView>(R.id.tvRecommendation)
        val tvSummary = view.findViewById<TextView>(R.id.tvSummary)
        val tvReasoning = view.findViewById<TextView>(R.id.tvReasoning)
        val tvFearGreed = view.findViewById<TextView>(R.id.tvFearGreed)
        val tvBtcDominance = view.findViewById<TextView>(R.id.tvBtcDominance)
        val tvFundingRate = view.findViewById<TextView>(R.id.tvFundingRate)
        val tvTimestamp = view.findViewById<TextView>(R.id.tvAnalysisTimestamp)
        val llHistory = view.findViewById<LinearLayout>(R.id.llHistory)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        swipe.setOnRefreshListener { viewModel.loadWorldmonitor() }

        viewModel.loadingWorldmonitor.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
        }

        viewModel.worldmonitor.observe(viewLifecycleOwner) { analysis ->
            tvError.visibility = View.GONE

            tvSentiment.text = (analysis.sentiment ?: "—").uppercase()
            tvSentiment.setTextColor(sentimentColor(analysis.sentiment))

            tvConfidence.text = String.format("%.0f%%", (analysis.confidence ?: 0.0) * 100)
            tvRecommendation.text = (analysis.recommendation ?: "—").replace("_", " ").uppercase()
            tvSummary.text = analysis.summary ?: "—"
            tvReasoning.text = analysis.reasoning ?: "—"

            tvFearGreed.text = "${analysis.fear_greed ?: "—"}/100"
            tvBtcDominance.text = String.format("%.1f%%", analysis.btc_dominance ?: 0.0)
            tvFundingRate.text = String.format("%.4f%%", (analysis.funding_rate ?: 0.0) * 100)

            val ts = analysis.timestamp ?: 0L
            tvTimestamp.text = if (ts > 0) formatTimestamp(ts) else "—"
        }

        viewModel.worldmonitorHistory.observe(viewLifecycleOwner) { history ->
            llHistory.removeAllViews()
            for (item in history.take(8)) {
                val tv = TextView(requireContext()).apply {
                    val ts = formatTimestamp(item.timestamp ?: 0L)
                    val sent = (item.sentiment ?: "—").uppercase()
                    val conf = String.format("%.0f%%", (item.confidence ?: 0.0) * 100)
                    text = "$ts  |  $sent ($conf)"
                    textSize = 12f
                    setPadding(0, 4, 0, 4)
                    setTextColor(sentimentColor(item.sentiment))
                }
                llHistory.addView(tv)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.worldmonitor.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadWorldmonitor()
    }

    private fun sentimentColor(sentiment: String?): Int = when (sentiment?.lowercase()) {
        "bullish" -> Color.parseColor("#4CAF50")
        "bearish" -> Color.parseColor("#F44336")
        else -> Color.parseColor("#FF9800")
    }

    private fun formatTimestamp(ts: Long): String {
        if (ts <= 0) return "—"
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}
