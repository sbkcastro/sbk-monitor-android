package com.sbkcastro.monitor.ui.wazuh

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.WazuhAlert
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WazuhFragment : Fragment() {

    private lateinit var statusDot: TextView
    private lateinit var statusLabel: TextView
    private lateinit var tvCount: TextView
    private lateinit var logContainer: LinearLayout
    private lateinit var soarContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var btnRefresh: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val bg = Color.parseColor("#0d1117")
        val cardBg = Color.parseColor("#161b22")
        val border = Color.parseColor("#30363d")
        val textMuted = Color.parseColor("#8b949e")
        val mono = Typeface.MONOSPACE

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // ── Header bar ──────────────────────────────────────────────────────
        val headerBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(cardBg)
        }
        statusDot = TextView(ctx).apply {
            text = "●"
            textSize = 10f
            typeface = mono
            setTextColor(Color.parseColor("#8b949e"))
        }
        statusLabel = TextView(ctx).apply {
            text = " WAZUH SIEM"
            textSize = 11f
            typeface = mono
            setTextColor(Color.parseColor("#58a6ff"))
        }
        tvCount = TextView(ctx).apply {
            text = ""
            textSize = 10f
            typeface = mono
            setTextColor(textMuted)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.END
        }
        btnRefresh = TextView(ctx).apply {
            text = "↺"
            textSize = 14f
            typeface = mono
            setTextColor(Color.parseColor("#58a6ff"))
            setPadding(dp(12), dp(4), 0, dp(4))
            isClickable = true
            isFocusable = true
        }
        headerBar.addView(statusDot)
        headerBar.addView(statusLabel)
        headerBar.addView(tvCount)
        headerBar.addView(btnRefresh)
        root.addView(headerBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // ── Scroll body ─────────────────────────────────────────────────────
        scrollView = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
        }
        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(16))
        }

        // Alerts section
        body.addView(sectionLabel(ctx, "ALERTAS RECIENTES"))
        logContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(logContainer)

        // SOAR log section
        body.addView(sectionLabel(ctx, "SOAR LOG"))
        soarContainer = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(soarContainer)

        scrollView.addView(body)
        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        btnRefresh.setOnClickListener { loadData() }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        statusDot.setTextColor(Color.parseColor("#d29922"))
        statusLabel.text = " CARGANDO..."
        logContainer.removeAllViews()
        soarContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val resp = ApiClient.getService().getWazuhAlerts(50)
                renderAlerts(resp.alerts)
                renderSoarLog(resp.soar_log)
                val total = resp.total
                tvCount.text = "$total alertas"
                statusDot.setTextColor(Color.parseColor("#3fb950"))
                statusLabel.text = " WAZUH SIEM"
            } catch (e: Exception) {
                statusDot.setTextColor(Color.parseColor("#f85149"))
                statusLabel.text = " ERROR"
                tvCount.text = e.message?.take(40) ?: "error"
            }
        }
    }

    private fun renderAlerts(alerts: List<WazuhAlert>) {
        logContainer.removeAllViews()
        if (alerts.isEmpty()) {
            logContainer.addView(dimLine("Sin alertas recientes"))
            return
        }
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).also {
            it.timeZone = TimeZone.getDefault()
        }
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }
        for (alert in alerts) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(3), 0, dp(3))
            }
            // Level badge + timestamp + description
            val topRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val levelColor = levelColor(alert.level)
            val badge = TextView(requireContext()).apply {
                text = "L${alert.level}"
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(levelColor)
                setPadding(0, 0, dp(6), 0)
            }
            val timeStr = try {
                sdf.format(inputFmt.parse(alert.timestamp.take(19))!!)
            } catch (_: Exception) { alert.timestamp.take(8) }
            val ts = TextView(requireContext()).apply {
                text = "$timeStr "
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#484f58"))
            }
            val desc = TextView(requireContext()).apply {
                text = alert.description
                textSize = 10f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#c9d1d9"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(badge)
            topRow.addView(ts)
            topRow.addView(desc)
            row.addView(topRow)

            // Agent + srcip line
            val meta = buildString {
                append("  ${alert.agent}")
                if (!alert.srcip.isNullOrEmpty()) append("  ← ${alert.srcip}")
            }
            val metaView = TextView(requireContext()).apply {
                text = meta
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#6e7681"))
            }
            row.addView(metaView)
            logContainer.addView(row)
        }
    }

    private fun renderSoarLog(lines: List<String>) {
        soarContainer.removeAllViews()
        if (lines.isEmpty()) {
            soarContainer.addView(dimLine("Sin actividad SOAR reciente"))
            return
        }
        for (line in lines) {
            val color = when {
                line.contains("verdict=block_ip") || line.contains("verdict=emergency") -> Color.parseColor("#f85149")
                line.contains("verdict=monitor") -> Color.parseColor("#d29922")
                line.contains("verdict=false_positive") -> Color.parseColor("#3fb950")
                line.contains("[defense]") -> Color.parseColor("#58a6ff")
                else -> Color.parseColor("#6e7681")
            }
            // Strip timestamp prefix for cleaner display
            val display = line.replace(Regex("^\\[\\d{4}-[^]]+]\\s*"), "")
            val tv = TextView(requireContext()).apply {
                text = display
                textSize = 9f
                typeface = Typeface.MONOSPACE
                setTextColor(color)
                setPadding(0, dp(2), 0, dp(2))
            }
            soarContainer.addView(tv)
        }
    }

    private fun levelColor(level: Int) = when {
        level >= 12 -> Color.parseColor("#f85149")  // CRITICAL
        level >= 9  -> Color.parseColor("#ff7b72")  // HIGH
        level >= 7  -> Color.parseColor("#d29922")  // MEDIUM
        level >= 4  -> Color.parseColor("#58a6ff")  // LOW
        else        -> Color.parseColor("#6e7681")  // INFO
    }

    private fun sectionLabel(ctx: android.content.Context, text: String) =
        TextView(ctx).apply {
            this.text = "── $text ──────────"
            textSize = 9f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#30363d"))
            setPadding(0, dp(10), 0, dp(4))
        }

    private fun dimLine(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 10f
        typeface = Typeface.MONOSPACE
        setTextColor(Color.parseColor("#484f58"))
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
