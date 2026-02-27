package com.sbkcastro.monitor.ui.ids

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.IDSAlert

class IDSAlertAdapter(private var alerts: List<IDSAlert> = emptyList()) :
    RecyclerView.Adapter<IDSAlertAdapter.AlertViewHolder>() {

    fun submitList(newAlerts: List<IDSAlert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ids_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(alerts[position])
    }

    override fun getItemCount() = alerts.size

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val severityBar: View = itemView.findViewById(R.id.severityBar)
        private val tvSignature: TextView = itemView.findViewById(R.id.tvSignature)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvIPs: TextView = itemView.findViewById(R.id.tvIPs)
        private val tvSource: TextView = itemView.findViewById(R.id.tvSource)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvAlertTimestamp)

        fun bind(alert: IDSAlert) {
            tvSignature.text = alert.signature
            tvCategory.text = alert.category
            tvIPs.text = "${alert.src_ip ?: "?"} → ${alert.dest_ip ?: "?"}"
            tvSource.text = alert.source
            tvTimestamp.text = alert.timestamp.substringBefore(".")

            severityBar.setBackgroundColor(when (alert.severity) {
                1 -> Color.parseColor("#F44336") // Rojo - crítica
                2 -> Color.parseColor("#FF9800") // Naranja - alta
                else -> Color.parseColor("#FFC107") // Amarillo - media/baja
            })
        }
    }
}
