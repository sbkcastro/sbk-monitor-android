package com.sbkcastro.monitor.ui.claude

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Terminal-style message renderer — no bubbles, colored prefixed lines
 * like Ghostty / LazyGit output.
 */
class BubbleAdapter : ListAdapter<ChatBubble, BubbleAdapter.VH>(DIFF) {

    private var streamingText: String? = null

    fun setStreamingText(text: String?) {
        val changed = streamingText != text
        streamingText = text
        if (changed) notifyDataSetChanged()
    }

    override fun getItemCount() = super.getItemCount() + if (streamingText != null) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { (it as RecyclerView.LayoutParams).setMargins(0, 0, 0, 2) }
            setPadding(0, 4, 0, 4)
        }

        val prefix = TextView(parent.context).apply {
            textSize = 11f
            typeface = Typeface.MONOSPACE
            tag = "prefix"
        }

        val body = TextView(parent.context).apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.15f)
            setTextIsSelectable(true)
            tag = "body"
        }

        container.addView(prefix)
        container.addView(body)
        return VH(container)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val container = holder.itemView as LinearLayout
        val prefix = container.findViewWithTag<TextView>("prefix")
        val body = container.findViewWithTag<TextView>("body")
        val listSize = super.getItemCount()

        // Reset listeners
        body.setOnLongClickListener(null)

        // Streaming partial text
        if (position >= listSize && streamingText != null) {
            prefix.visibility = View.GONE
            body.text = SpannableStringBuilder().apply {
                append(TerminalRenderer.render(streamingText ?: ""))
                val cur = length
                append("▋")
                setSpan(ForegroundColorSpan(C_GREEN), cur, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            body.setTextColor(C_TEXT)
            container.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        val item = getItem(position)

        when (item.role) {
            "user" -> {
                prefix.visibility = View.VISIBLE
                prefix.text = "❯"
                prefix.setTextColor(C_GREEN)
                body.text = item.text
                body.setTextColor(C_GREEN)
                body.typeface = Typeface.MONOSPACE
                container.setBackgroundColor(Color.TRANSPARENT)
            }
            "assistant" -> {
                prefix.visibility = View.GONE
                body.text = TerminalRenderer.render(item.text)
                body.setTextColor(C_TEXT)
                body.typeface = Typeface.MONOSPACE
                container.setBackgroundColor(Color.TRANSPARENT)
                body.setOnLongClickListener { v -> copyToClipboard(v.context, item.text); true }
            }
            "tool" -> {
                prefix.visibility = View.VISIBLE
                val (icon, stateColor) = when (item.toolState) {
                    "pending"  -> "⏳" to C_YELLOW
                    "approved" -> "✓"  to C_DIM
                    "denied"   -> "✗"  to C_RED
                    else       -> "⚡" to C_YELLOW
                }
                prefix.text = icon
                prefix.setTextColor(stateColor)
                body.text = item.text
                body.setTextColor(C_YELLOW)
                body.typeface = Typeface.MONOSPACE
                container.setBackgroundColor(C_CODE_BG)
                body.setOnLongClickListener { v -> copyToClipboard(v.context, item.text); true }
            }
            "meta" -> {
                prefix.visibility = View.VISIBLE
                prefix.text = "🤖"
                prefix.setTextColor(C_YELLOW)
                body.text = TerminalRenderer.render(item.text)
                body.setTextColor(C_YELLOW)
                body.typeface = Typeface.MONOSPACE
                container.setBackgroundColor(C_CODE_BG)
                body.setOnLongClickListener { v -> copyToClipboard(v.context, item.text); true }
            }
            "system" -> {
                prefix.visibility = View.GONE
                body.text = item.text
                body.setTextColor(C_DIM)
                body.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
                container.setBackgroundColor(Color.TRANSPARENT)
            }
            else -> {
                prefix.visibility = View.GONE
                body.text = item.text
                body.setTextColor(C_TEXT)
                body.typeface = Typeface.MONOSPACE
                container.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("claude", text))
        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view)

    companion object {
        private val C_TEXT    = Color.parseColor("#c9d1d9")
        private val C_DIM     = Color.parseColor("#8b949e")
        private val C_GREEN   = Color.parseColor("#3fb950")
        private val C_YELLOW  = Color.parseColor("#d29922")
        private val C_RED     = Color.parseColor("#f85149")
        private val C_CODE_BG = Color.parseColor("#1c2128")

        val DIFF = object : DiffUtil.ItemCallback<ChatBubble>() {
            override fun areItemsTheSame(a: ChatBubble, b: ChatBubble) = a === b
            override fun areContentsTheSame(a: ChatBubble, b: ChatBubble) = a == b
        }
    }
}
