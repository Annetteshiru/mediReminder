package com.example.medireminder

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class WeeklyBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class DayData(val day: String, val percentage: Float)

    private var data = listOf<DayData>()
    private val dp = context.resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pointOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val pointInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EBF2FF")
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        textAlign = Paint.Align.CENTER
    }
    private val todayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setData(days: List<DayData>) {
        data = days
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val textSz = 10f * dp
        labelPaint.textSize = textSz
        todayLabelPaint.textSize = textSz
        percentPaint.textSize = 9f * dp
        linePaint.strokeWidth = 2.5f * dp
        gridPaint.strokeWidth = 1f * dp

        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = 20f * dp
        val percentH = 18f * dp
        val chartTop = percentH + 4f * dp
        val chartBottom = h - labelH - 4f * dp
        val chartH = chartBottom - chartTop

        val count = data.size
        // Evenly distribute points with horizontal padding
        val hPad = 24f * dp
        val xStep = if (count > 1) (w - hPad * 2f) / (count - 1).toFloat() else 0f
        val xs = FloatArray(count) { i -> hPad + i * xStep }
        val ys = FloatArray(count) { i ->
            val pct = data[i].percentage.coerceIn(0f, 100f)
            chartBottom - (pct / 100f) * chartH
        }

        // Horizontal grid lines at 0, 25, 50, 75, 100%
        listOf(0f, 25f, 50f, 75f, 100f).forEach { pct ->
            val y = chartBottom - (pct / 100f) * chartH
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        if (count >= 2) {
            // Filled area under the line
            val fillPath = buildSmoothPath(xs, ys, count)
            fillPath.lineTo(xs[count - 1], chartBottom)
            fillPath.lineTo(xs[0], chartBottom)
            fillPath.close()
            fillPaint.shader = LinearGradient(
                0f, chartTop, 0f, chartBottom,
                intArrayOf(Color.parseColor("#661976D2"), Color.parseColor("#001976D2")),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(fillPath, fillPaint)

            // Smooth line
            val linePath = buildSmoothPath(xs, ys, count)
            canvas.drawPath(linePath, linePaint)
        }

        // Data point dots + labels
        val outerR = 5f * dp
        val innerR = 3f * dp
        data.forEachIndexed { i, dayData ->
            val x = xs[i]
            val y = ys[i]

            // Outer white circle
            canvas.drawCircle(x, y, outerR, pointOuterPaint)
            // Inner colored circle
            pointInnerPaint.color = if (dayData.percentage >= 100f)
                Color.parseColor("#43A047") else Color.parseColor("#1565C0")
            canvas.drawCircle(x, y, innerR, pointInnerPaint)

            // Percentage label above point
            if (dayData.percentage > 0f) {
                canvas.drawText(
                    "${dayData.percentage.toInt()}%",
                    x,
                    y - outerR - 3f * dp,
                    percentPaint
                )
            }

            // Day label below chart
            val isToday = i == count - 1
            canvas.drawText(
                if (isToday) "Today" else dayData.day,
                x,
                h,
                if (isToday) todayLabelPaint else labelPaint
            )
        }
    }

    /** Builds a smooth cubic-bezier path through the given points */
    private fun buildSmoothPath(xs: FloatArray, ys: FloatArray, count: Int): Path {
        val path = Path()
        path.moveTo(xs[0], ys[0])
        for (i in 1 until count) {
            val cpX = (xs[i - 1] + xs[i]) / 2f
            path.cubicTo(cpX, ys[i - 1], cpX, ys[i], xs[i], ys[i])
        }
        return path
    }
}
