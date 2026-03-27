package com.sbkcastro.monitor.ui.services

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.api.SiteTelemetry

class ServicesAdapter : ListAdapter<SiteTelemetry, ServicesAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH.create(parent)
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root) {

        private val tvName   = root.getChildAt(0) as LinearLayout
        private val tvMeta   = root.getChildAt(1) as TextView
        private val tvAnalyt = root.getChildAt(2) as TextView

        companion object {
            fun create(parent: ViewGroup): VH {
                val ctx = parent.context
                val mono = Typeface.MONOSPACE

                val root = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#161b22"))
                    setPadding(16, 12, 16, 12)
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 6 }
                }

                // Row: status dot + name + latency
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val tvDot = TextView(ctx).apply {
                    text = "●"; textSize = 10f; typeface = mono
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = 8 }
                }
                val tvNameLabel = TextView(ctx).apply {
                    textSize = 12f; typeface = mono
                    setTextColor(Color.parseColor("#e6edf3"))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvLatency = TextView(ctx).apply {
                    textSize = 9f; typeface = mono
                    setTextColor(Color.parseColor("#484f58"))
                }
                row.addView(tvDot); row.addView(tvNameLabel); row.addView(tvLatency)
                root.addView(row)

                // Meta line
                val tvMeta = TextView(ctx).apply {
                    textSize = 9f; typeface = mono
                    setTextColor(Color.parseColor("#484f58"))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = 2 }
                }
                root.addView(tvMeta)

                // Analytics line
                val tvAnalyt = TextView(ctx).apply {
                    textSize = 9f; typeface = mono
                    setTextColor(Color.parseColor("#58a6ff"))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = 2 }
                    visibility = View.GONE
                }
                root.addView(tvAnalyt)

                return VH(root)
            }
        }

        fun bind(site: SiteTelemetry) {
            val row   = root.getChildAt(0) as LinearLayout
            val tvDot   = row.getChildAt(0) as TextView
            val tvName  = row.getChildAt(1) as TextView
            val tvLat   = row.getChildAt(2) as TextView

            tvName.text = site.name
            tvLat.text  = "${site.latency}ms"

            val online = site.status == "online"
            tvDot.setTextColor(if (online) Color.parseColor("#3fb950") else Color.parseColor("#f85149"))
            tvMeta.text = "${site.url}  [${site.status.uppercase()}]"

            val a = site.analytics
            if (a != null) {
                tvAnalyt.visibility = View.VISIBLE
                val parts = buildList {
                    add("👁 activos=${a.activeNow}")
                    add("views=${a.pageviews}")
                    add("visitors=${a.visitors}")
                    if (a.bounceRate != null) add("bounce=${a.bounceRate}%")
                    if (a.avgTime != null) add("avgTime=${a.avgTime}s")
                }
                tvAnalyt.text = parts.joinToString("  ")
            } else {
                tvAnalyt.visibility = View.GONE
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<SiteTelemetry>() {
        override fun areItemsTheSame(a: SiteTelemetry, b: SiteTelemetry) = a.name == b.name
        override fun areContentsTheSame(a: SiteTelemetry, b: SiteTelemetry) = a == b
    }
}
