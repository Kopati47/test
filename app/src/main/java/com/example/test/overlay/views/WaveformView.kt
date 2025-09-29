package com.example.test.overlay.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barW = 2f * resources.displayMetrics.density
    private val gap  = 3f * resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = barW
        strokeCap = Paint.Cap.ROUND
    }

    private var maxBars = 0
    private val values = ArrayDeque<Float>()

    fun push(level: Float) {
        if (maxBars <= 0) return
        val v = level.coerceIn(0f, 1f)
        while (values.size >= maxBars) values.removeFirst()
        values.addLast(v)
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        maxBars = min(1000, (w / (barW + gap)).toInt().coerceAtLeast(1))
        while (values.size > maxBars) values.removeFirst()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cy = height / 2f
        var x = barW / 2f
        for (v in values) {
            val half = (height * 0.9f * v) / 2f
            c.drawLine(x, cy - half, x, cy + half, paint)
            x += barW + gap
        }
    }
}
