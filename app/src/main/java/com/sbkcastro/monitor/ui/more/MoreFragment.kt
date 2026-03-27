package com.sbkcastro.monitor.ui.more

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sbkcastro.monitor.R

class MoreFragment : Fragment() {

    data class NavItem(val icon: String, val label: String, val destId: Int)

    private val items = listOf(
        NavItem("🛡️", "Seguridad", R.id.securityFragment),
        NavItem("⚙️", "Servicios", R.id.servicesFragment),
        NavItem("🔧", "Procesos", R.id.processesFragment),
        NavItem("💬", "Chat", R.id.chatFragment),
        NavItem("🖥️", "Dashboard", R.id.dashboardFragment),
        NavItem("📦", "Containers", R.id.containersFragment),
        NavItem("⚙️", "Config", R.id.settingsFragment)
    )

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#0d1117"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(16, 16, 16, 16)
        }

        val title = TextView(ctx).apply {
            text = "más secciones"
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(android.graphics.Color.parseColor("#484f58"))
            setPadding(8, 8, 8, 16)
        }
        root.addView(title)

        // Grid 2 columnas
        var row: LinearLayout? = null
        items.forEachIndexed { idx, item ->
            if (idx % 2 == 0) {
                row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 12 }
                }
                root.addView(row)
            }

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#161b22"))
                setPadding(24, 32, 24, 32)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { if (idx % 2 == 0) it.marginEnd = 6 else it.marginStart = 6 }
                isClickable = true
                isFocusable = true

                // ripple effect
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val ta = ctx.obtainStyledAttributes(attrs)
                foreground = ta.getDrawable(0)
                ta.recycle()

                setOnClickListener {
                    try { findNavController().navigate(item.destId) } catch (_: Exception) {}
                }
            }

            val iconView = TextView(ctx).apply {
                text = item.icon
                textSize = 28f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }

            val labelView = TextView(ctx).apply {
                text = item.label
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(android.graphics.Color.parseColor("#8b949e"))
                gravity = Gravity.CENTER
            }

            card.addView(iconView)
            card.addView(labelView)
            row?.addView(card)
        }

        // Relleno si número impar
        if (items.size % 2 != 0) {
            row?.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    .also { it.marginStart = 6 }
            })
        }

        return root
    }
}
