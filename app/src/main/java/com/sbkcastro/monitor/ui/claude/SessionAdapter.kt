package com.sbkcastro.monitor.ui.claude

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.api.ClaudeSession

/**
 * Compact tab-style session pills — LazyGit panel tab aesthetic.
 */
class SessionAdapter(
    private val onTap: (ClaudeSession) -> Unit
) : ListAdapter<ClaudeSession, SessionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val density = parent.context.resources.displayMetrics.density
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins((4 * density).toInt(), (4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt()) }
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxEms = 16
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val session = getItem(position)
        val tv = holder.itemView as TextView
        val isActive = session.status == "running" || session.status == "waiting_approval"

        val borderColor = if (isActive) C_ACTIVE else C_BORDER
        val bgColor = if (isActive) C_ACTIVE_BG else C_BG
        val textColor = if (isActive) C_ACTIVE else C_DIM

        // Pill shape with border
        val shape = GradientDrawable().apply {
            setColor(bgColor)
            setStroke(1, borderColor)
            cornerRadius = 4f * tv.context.resources.displayMetrics.density
        }
        tv.background = shape

        val dot = when (session.status) {
            "running"          -> "●"
            "waiting_approval" -> "◉"
            "completed"        -> "○"
            else               -> "○"
        }

        // Truncate first message for tab label
        val label = session.firstMessage.take(20).replace('\n', ' ')
        tv.text = "$dot $label"
        tv.setTextColor(textColor)
        tv.setOnClickListener { onTap(session) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private val C_ACTIVE    = Color.parseColor("#58a6ff")
        private val C_ACTIVE_BG = Color.parseColor("#0d2240")
        private val C_BORDER    = Color.parseColor("#30363d")
        private val C_BG        = Color.parseColor("#161b22")
        private val C_DIM       = Color.parseColor("#8b949e")

        val DIFF = object : DiffUtil.ItemCallback<ClaudeSession>() {
            override fun areItemsTheSame(a: ClaudeSession, b: ClaudeSession) = a.jobId == b.jobId
            override fun areContentsTheSame(a: ClaudeSession, b: ClaudeSession) = a == b
        }
    }
}
