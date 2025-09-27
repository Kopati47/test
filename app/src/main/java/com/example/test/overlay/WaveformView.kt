package com.example.test.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.util.ArrayDeque
import kotlin.math.min

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private val barWidth = dp(2f)
    private val gap = dp(3f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = barWidth
        strokeCap = Paint.Cap.ROUND
    }

    private var maxBars = 0
    private val bars = ArrayDeque<Float>() // значения 0..1

    fun pushLevel(level: Float) {
        if (maxBars <= 0) return
        val v = level.coerceIn(0f, 1f)
        while (bars.size >= maxBars) bars.removeFirst()
        bars.addLast(v)
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxBars = min(1000, ((w) / (barWidth + gap)).toInt().coerceAtLeast(1))
        while (bars.size > maxBars) bars.removeFirst()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        var x = barWidth / 2f
        for (v in bars) {
            val half = (height * 0.9f * v) / 2f
            canvas.drawLine(x, centerY - half, x, centerY + half, paint)
            x += barWidth + gap
        }
    }
}
