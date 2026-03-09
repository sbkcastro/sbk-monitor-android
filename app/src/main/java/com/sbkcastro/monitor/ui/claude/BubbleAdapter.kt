package com.sbkcastro.monitor.ui.claude

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class BubbleAdapter : ListAdapter<ChatBubble, BubbleAdapter.VH>(DIFF) {

    private var streamingText: String? = null

    fun setStreamingText(text: String?) {
        streamingText = text
        notifyDataSetChanged()
    }

    override fun getItemCount() = super.getItemCount() + if (streamingText != null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(8, 4, 8, 4) }
            setPadding(16, 10, 16, 10)
            textSize = 13f
            maxLines = 40
            ellipsize = TextUtils.TruncateAt.END
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tv = holder.itemView as TextView
        val listSize = super.getItemCount()
        if (position >= listSize && streamingText != null) {
            tv.text = "🤖 ${streamingText}▋"
            tv.setBackgroundColor(0xFF1a1a2e.toInt())
            tv.setTextColor(Color.parseColor("#CCCCCC"))
            tv.typeface = Typeface.DEFAULT
            return
        }
        val item = getItem(position)
        when (item.role) {
            "user" -> {
                tv.text = "Tú: ${item.text}"
                tv.setBackgroundColor(0xFF0a1628.toInt())
                tv.setTextColor(Color.parseColor("#88AAFF"))
                tv.typeface = Typeface.DEFAULT
            }
            "assistant" -> {
                tv.text = "🤖 ${item.text}"
                tv.setBackgroundColor(0xFF1a1a2e.toInt())
                tv.setTextColor(Color.parseColor("#CCCCCC"))
                tv.typeface = Typeface.DEFAULT
            }
            "tool" -> {
                val icon = when (item.toolState) {
                    "pending" -> "⏳"; "approved" -> "✅"; "denied" -> "❌"; else -> "⚙️"
                }
                tv.text = "$icon ${item.text}"
                tv.setBackgroundColor(0xFF111122.toInt())
                tv.setTextColor(Color.parseColor("#88DDAA"))
                tv.typeface = Typeface.MONOSPACE
            }
            "system" -> {
                tv.text = item.text
                tv.setBackgroundColor(0xFF0a0a1a.toInt())
                tv.setTextColor(Color.parseColor("#888899"))
                tv.typeface = Typeface.DEFAULT
            }
            else -> {
                tv.text = item.text
                tv.setTextColor(Color.parseColor("#CCCCCC"))
            }
        }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChatBubble>() {
            override fun areItemsTheSame(a: ChatBubble, b: ChatBubble) = a === b
            override fun areContentsTheSame(a: ChatBubble, b: ChatBubble) = a == b
        }
    }
}
