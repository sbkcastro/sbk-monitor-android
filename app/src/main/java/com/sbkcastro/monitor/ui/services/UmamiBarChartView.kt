package com.sbkcastro.monitor.ui.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sbkcastro.monitor.api.UmamiDayPoint

class UmamiBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#388bfd") }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#484f58")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8b949e")
        textSize = 28f
    }

    private var data: List<UmamiDayPoint> = emptyList()

    fun setData(points: List<UmamiDayPoint>) {
        data = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val paddingTop = 40f
        val paddingBottom = 40f
        val paddingH = 16f

        canvas.drawText("pageviews 7d — sbkcastro.com", paddingH, 30f, titlePaint)

        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
        val barCount = data.size
        val totalPaddingH = paddingH * 2
        val gap = 6f
        val barWidth = (w - totalPaddingH - gap * (barCount - 1)) / barCount
        val chartH = h - paddingTop - paddingBottom

        data.forEachIndexed { i, point ->
            val left = paddingH + i * (barWidth + gap)
            val barH = (point.count.toFloat() / maxCount) * chartH
            val top = paddingTop + chartH - barH
            val right = left + barWidth
            val bottom = paddingTop + chartH

            canvas.drawRoundRect(RectF(left, top, right, bottom), 4f, 4f, barPaint)

            // Day label (last 2 chars of date, e.g. "21")
            val dayLabel = point.date.takeLast(2)
            canvas.drawText(dayLabel, left + barWidth / 2, h - 8f, labelPaint)
        }
    }
}
