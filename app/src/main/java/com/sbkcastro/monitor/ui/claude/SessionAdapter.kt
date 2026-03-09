package com.sbkcastro.monitor.ui.claude

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.api.ClaudeSession

class SessionAdapter(
    private val onTap: (ClaudeSession) -> Unit
) : ListAdapter<ClaudeSession, SessionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val density = parent.context.resources.displayMetrics.density
        val tv = TextView(parent.context).apply {
            val w = (150 * density).toInt()
            val h = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams = ViewGroup.MarginLayoutParams(w, h).also { it.setMargins(4, 4, 4, 4) }
            setPadding(10, 8, 10, 8)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            maxLines = 4
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val session = getItem(position)
        val tv = holder.itemView as TextView
        val badge = when (session.status) {
            "running" -> "🟢"
            "waiting_approval" -> "🟠"
            "completed" -> "⚪"
            else -> "🔴"
        }
        tv.text = "$badge ${session.firstMessage}"
        tv.setBackgroundColor(
            if (session.status == "running" || session.status == "waiting_approval")
                0xFF0a2040.toInt() else 0xFF1a1a2e.toInt()
        )
        tv.setTextColor(Color.parseColor("#CCCCCC"))
        tv.setOnClickListener { onTap(session) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ClaudeSession>() {
            override fun areItemsTheSame(a: ClaudeSession, b: ClaudeSession) = a.jobId == b.jobId
            override fun areContentsTheSame(a: ClaudeSession, b: ClaudeSession) = a == b
        }
    }
}
