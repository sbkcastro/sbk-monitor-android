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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.TradeItem

class CryptoTradesTab : Fragment() {

    private val viewModel: CryptoViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_crypto_trades, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeTrades)
        val rvTrades = view.findViewById<RecyclerView>(R.id.rvTrades)
        val tvActiveCount = view.findViewById<TextView>(R.id.tvActiveCount)
        val tvNoTrades = view.findViewById<TextView>(R.id.tvNoTrades)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        rvTrades.layoutManager = LinearLayoutManager(requireContext())
        val adapter = TradeAdapter()
        rvTrades.adapter = adapter

        swipe.setOnRefreshListener { viewModel.loadTrades() }

        viewModel.loadingTrades.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
        }

        viewModel.activeTrades.observe(viewLifecycleOwner) { active ->
            tvActiveCount.text = "Trades abiertos: ${active.size}"
        }

        viewModel.trades.observe(viewLifecycleOwner) { trades ->
            tvError.visibility = View.GONE
            if (trades.isEmpty()) {
                tvNoTrades.visibility = View.VISIBLE
                rvTrades.visibility = View.GONE
            } else {
                tvNoTrades.visibility = View.GONE
                rvTrades.visibility = View.VISIBLE
                adapter.submitList(trades)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.trades.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadTrades()
    }
}

class TradeAdapter : RecyclerView.Adapter<TradeAdapter.VH>() {
    private var items = listOf<TradeItem>()

    fun submitList(list: List<TradeItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trade, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPair = view.findViewById<TextView>(R.id.tvTradePair)
        private val tvProfit = view.findViewById<TextView>(R.id.tvTradeProfit)
        private val tvDate = view.findViewById<TextView>(R.id.tvTradeDate)
        private val tvStatus = view.findViewById<TextView>(R.id.tvTradeStatus)

        fun bind(trade: TradeItem) {
            tvPair.text = trade.pair ?: "—"
            val profitPct = (trade.profit_ratio ?: 0.0) * 100
            tvProfit.text = String.format("%+.2f%%", profitPct)
            tvProfit.setTextColor(if (profitPct >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            tvDate.text = trade.open_date?.take(16) ?: "—"
            tvStatus.text = if (trade.is_open == true) "OPEN" else "CLOSED"
            tvStatus.setTextColor(if (trade.is_open == true) Color.parseColor("#FF9800") else Color.parseColor("#9E9E9E"))
        }
    }
}
