package com.sbkcastro.monitor.ui.sites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.SiteStatus

class SitesAdapter : ListAdapter<SiteStatus, SitesAdapter.ViewHolder>(DIFF) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndicator: TextView = view.findViewById(R.id.tvIndicator)
        val tvName: TextView = view.findViewById(R.id.tvSiteName)
        val tvUrl: TextView = view.findViewById(R.id.tvSiteUrl)
        val tvLatency: TextView = view.findViewById(R.id.tvLatency)
        val tvHttpCode: TextView = view.findViewById(R.id.tvHttpCode)
        val tvStatus: TextView = view.findViewById(R.id.tvSiteStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_site, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val site = getItem(position)

        holder.tvName.text = site.name
        holder.tvUrl.text = site.url
        holder.tvLatency.text = "${site.latency}ms"
        holder.tvHttpCode.text = if (site.httpCode > 0) "HTTP ${site.httpCode}" else "—"

        val ctx = holder.itemView.context
        when (site.status) {
            "online" -> {
                holder.tvIndicator.text = "●"
                holder.tvIndicator.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_light))
                holder.tvStatus.text = "ONLINE"
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_light))
            }
            "timeout" -> {
                holder.tvIndicator.text = "●"
                holder.tvIndicator.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_orange_light))
                holder.tvStatus.text = "TIMEOUT"
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_orange_light))
            }
            else -> {
                holder.tvIndicator.text = "●"
                holder.tvIndicator.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_light))
                holder.tvStatus.text = site.status.uppercase()
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_light))
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SiteStatus>() {
            override fun areItemsTheSame(a: SiteStatus, b: SiteStatus) = a.name == b.name
            override fun areContentsTheSame(a: SiteStatus, b: SiteStatus) = a == b
        }
    }
}
