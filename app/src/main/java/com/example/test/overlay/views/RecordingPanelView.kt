package com.example.test.overlay.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.R
import com.example.test.overlay.util.dp

class RecordingPanelView(context: Context) : LinearLayout(context) {

    val waveform = WaveformView(context)
    private val stopBtn = TextView(context)
    private var anim: ViewPropertyAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_record_panel)
        setPadding(dp(10f), dp(6f), dp(10f), dp(6f))

        // Внутри родитель задаёт ширину/высоту самой панели.
        // Тут: waveform заполняет всё слева, STOP справа фиксированной ширины.
        addView(waveform, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

        stopBtn.apply {
            text = "STOP"
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setTextColor(Color.WHITE)
            textSize = 13.5f
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            minWidth = dp(56f)
            minHeight = dp(28f)
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(Color.parseColor("#D32F2F"))
            }
        }
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dp(8f)
        addView(stopBtn, lp)

        alpha = 0f // fade-in
    }

    fun setOnStop(action: () -> Unit) {
        stopBtn.setOnClickListener { action() }
    }

    fun fadeIn(duration: Long = 200L) {
        anim?.cancel()
        anim = animate().alpha(1f).setDuration(duration)
        anim?.start()
    }
}
