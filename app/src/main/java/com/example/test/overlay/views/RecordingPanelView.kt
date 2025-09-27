package com.example.test.overlay.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.overlay.util.dp

class RecordingPanelView(context: Context) : LinearLayout(context) {

    val waveform = WaveformView(context)
    private val stopBtn = TextView(context)

    private var anim: ViewPropertyAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // waveform строго вписывается размерами через LayoutParams, их задаёт сервис
        addView(waveform, LayoutParams(0, 0))

        stopBtn.text = "STOP"
        stopBtn.setTextColor(Color.WHITE)
        stopBtn.textSize = 14f
        stopBtn.setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        stopBtn.background = GradientDrawable().apply {
            cornerRadius = dp(10f).toFloat()
            setColor(Color.parseColor("#D32F2F"))
        }
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dp(8f)
        addView(stopBtn, lp)

        alpha = 0f // для fade-in
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
