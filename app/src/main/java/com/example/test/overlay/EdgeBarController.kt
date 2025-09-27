package com.example.test.overlay

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.animation.addListener
import com.example.test.R
import com.example.test.overlay.util.blendColor
import com.example.test.overlay.util.dp
import com.example.test.overlay.util.dpF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class EdgeBarController(
    private val wm: WindowManager,
    private val container: FrameLayout,
    private val params: WindowManager.LayoutParams,
    // настройки
    private val edgeOffsetDp: Float,
    private val topMarginDp: Float,
    private val bottomMarginDp: Float,
    private val touchWidthDp: Float,
    private val touchHeightDp: Float,
    private val touchMaxHeightDp: Float,
    private val lineWidthDp: Float,
    private val lineHeightDp: Float,
    private val lineMaxHeightDp: Float,
    private val startFromBottomGapDp: Float,
    private val stretchRatio: Float,
    private val maxStretchDp: Float,
    private val hLockThresholdDp: Float,
    private val heightGrowStartDp: Float,
    private val entryAnimMs: Long,
    private val autoTriggerDp: Float,
    private val autoAnimMs: Long,
    private val colorStart: Int,
    private val colorTarget: Int,
    private val onAutoCompleted: (padStartPx: Int, lineW: Int, lineH: Int) -> Unit
) {

    private var entryAnimating = true
    private var autoCompleting = false
    private var stretching = false
    private var autoAnim: ValueAnimator? = null

    private lateinit var line: View

    fun attach() {
        val edgeOffsetPx      = container.context.dp(edgeOffsetDp)
        val touchWidthPx      = container.context.dp(touchWidthDp)
        val baseTouchHeightPx = container.context.dp(touchHeightDp)
        val baseLineWidthPx   = container.context.dp(lineWidthDp)
        val baseLineHeightPx  = container.context.dp(lineHeightDp)

        val maxTouchHeightPx  = container.context.dp(touchMaxHeightDp)
        val maxLineHeightPx   = container.context.dp(lineMaxHeightDp)
        val maxStretchPx      = container.context.dp(maxStretchDp)
        val growStartPx       = container.context.dp(heightGrowStartDp)
        val hLockThresholdPx  = container.context.dpF(hLockThresholdDp)
        val autoTriggerPx     = container.context.dp(autoTriggerDp)

        val baseContainerWidthPx = edgeOffsetPx + touchWidthPx
        val fullContainerWidthPx = baseContainerWidthPx + maxStretchPx
        val fullLineWidthPx      = baseLineWidthPx + maxStretchPx

        params.width  = baseContainerWidthPx
        params.height = baseTouchHeightPx
        params.gravity = Gravity.START or Gravity.TOP
        params.format = PixelFormat.TRANSLUCENT
        params.x = 0
        params.y = 0
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)

        // стартовая позиция по Y (от низа)
        run {
            val triple = screenHeightAndInsets()
            val screenH = triple.first
            val insetBottom = triple.third
            val (minY, maxY) = computeYBounds(params, baseTouchHeightPx)
            val targetY = screenH - insetBottom - container.context.dp(startFromBottomGapDp) - params.height
            params.y = clamp(targetY, minY, maxY)
        }
        wm.updateViewLayout(container, params)

        container.removeAllViews()
        // сама тонкая линия внутри контейнера
        val lp = FrameLayout.LayoutParams(baseLineWidthPx, baseLineHeightPx).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            marginStart = edgeOffsetPx
        }
        line = View(container.context).apply {
            layoutParams = lp
            setBackgroundResource(R.drawable.bg_edgebar)
            (background.mutate() as? GradientDrawable)?.setColor(colorStart)
        }
        container.addView(line)

        // жесты
        var startY = 0
        var touchStartY = 0f
        var touchStartX = 0f

        container.setOnTouchListener { _, e ->
            if (entryAnimating || autoCompleting) return@setOnTouchListener true

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = params.y
                    touchStartY = e.rawY
                    touchStartX = e.rawX
                    stretching = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawDx = e.rawX - touchStartX
                    val dy = (e.rawY - touchStartY).toInt()

                    if (!stretching && rawDx > hLockThresholdPx && abs(rawDx) > abs(dy))
                        stretching = true

                    if (!stretching) {
                        val (minY, maxY) = computeYBounds(params, params.height)
                        var newY = startY + dy
                        params.y = clamp(newY, minY, maxY)
                    }

                    val extra = max(0f, rawDx * stretchRatio)
                    params.width = clamp(baseContainerWidthPx + extra.toInt(),
                        baseContainerWidthPx, fullContainerWidthPx)

                    val tColor = (extra / maxStretchPx).coerceIn(0f, 1f)
                    (line.background.mutate() as? GradientDrawable)
                        ?.setColor(blendColor(colorStart, colorTarget, tColor))

                    val heightT = if (maxStretchPx > growStartPx)
                        ((extra - growStartPx) / (maxStretchPx - growStartPx)).coerceIn(0f, 1f)
                    else 0f
                    val newTouchH = (baseTouchHeightPx +
                            heightT * (maxTouchHeightPx - baseTouchHeightPx)).toInt()
                    if (newTouchH != params.height) {
                        params.height = newTouchH
                        val (minY, maxY) = computeYBounds(params, newTouchH)
                        params.y = clamp(params.y, minY, maxY)
                    }

                    val lineLp = line.layoutParams as FrameLayout.LayoutParams
                    lineLp.width = clamp(baseLineWidthPx + extra.toInt(), baseLineWidthPx, fullLineWidthPx)
                    lineLp.height = (baseLineHeightPx +
                            heightT * (maxLineHeightPx - baseLineHeightPx)).toInt()
                    line.layoutParams = lineLp

                    wm.updateViewLayout(container, params)

                    if (extra >= autoTriggerPx && !autoCompleting) {
                        startAutoComplete(
                            baseContainerWidthPx, fullContainerWidthPx,
                            baseTouchHeightPx, maxTouchHeightPx,
                            baseLineWidthPx, fullLineWidthPx,
                            baseLineHeightPx, maxLineHeightPx,
                            edgeOffsetPx
                        )
                    }
                    true
                }
                else -> false
            }
        }

        // анимация въезда
        container.translationX = -(edgeOffsetPx + baseLineWidthPx).toFloat()
        ValueAnimator.ofFloat(container.translationX, 0f).apply {
            duration = entryAnimMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { container.translationX = it.animatedValue as Float }
            addListener(onEnd = { entryAnimating = false })
            start()
        }
    }

    private fun startAutoComplete(
        baseW: Int, fullW: Int,
        baseH: Int, maxH: Int,
        baseLW: Int, fullLW: Int,
        baseLH: Int, maxLH: Int,
        padStartPx: Int
    ) {
        autoCompleting = true
        val startW = params.width
        val startH = params.height
        val lp0 = line.layoutParams as FrameLayout.LayoutParams
        val startLW = lp0.width
        val startLH = lp0.height

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = autoAnimMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedFraction
                params.width  = (startW + (fullW  - startW) * t).toInt()
                params.height = (startH + (maxH  - startH) * t).toInt()
                val lp = line.layoutParams as FrameLayout.LayoutParams
                lp.width  = (startLW + (fullLW - startLW) * t).toInt()
                lp.height = (startLH + (maxLH - startLH) * t).toInt()
                line.layoutParams = lp
                val (minY, maxY) = computeYBounds(params, params.height)
                params.y = clamp(params.y, minY, maxY)
                wm.updateViewLayout(container, params)
            }
            addListener(onEnd = {
                autoCompleting = false
                val finalLp = line.layoutParams as FrameLayout.LayoutParams
                onAutoCompleted(padStartPx, finalLp.width, finalLp.height)
            })
            start()
        }
    }

    fun detach() {
        try { wm.removeView(container) } catch (_: Exception) {}
    }

    private fun clamp(v: Int, lo: Int, hi: Int) = max(lo, min(hi, v))

    private fun computeYBounds(p: WindowManager.LayoutParams, heightNow: Int): Pair<Int, Int> {
        val triple = screenHeightAndInsets()
        val screenH = triple.first
        val insetTop = triple.second
        val insetBottom = triple.third
        val minY = insetTop + container.context.dp(topMarginDp)
        val maxY = screenH - insetBottom - container.context.dp(bottomMarginDp) - heightNow
        return minY to maxY
    }

    private fun screenHeightAndInsets(): Triple<Int, Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val m = wm.currentWindowMetrics
            val ins = m.windowInsets.getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            Triple(m.bounds.height(), ins.top, ins.bottom)
        } else {
            @Suppress("DEPRECATION")
            val d = wm.defaultDisplay
            val size = Point()
            @Suppress("DEPRECATION") d.getSize(size)
            Triple(size.y, 0, 0)
        }
    }
}
