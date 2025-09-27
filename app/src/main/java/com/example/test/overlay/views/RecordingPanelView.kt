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

    // слева бегущая волна
    val waveform = WaveformView(context)

    // справа кнопки: синяя ✓ и красная Cancel
    private val confirmBtn = TextView(context)
    private val cancelBtn  = TextView(context)

    private var anim: ViewPropertyAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_record_panel)
        setPadding(dp(10f), dp(6f), dp(10f), dp(6f))

        // панель сама по себе кликабельна (чтобы события не «уходили вниз»)
        isClickable = true
        isLongClickable = false

        // --- WAVEFORM ---
        // занимает всё оставшееся пространство слева,
        // и полностью "глотает" любые касания, чтобы не было реакций на рабочем столе
        addView(waveform, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        waveform.isClickable = true
        waveform.isLongClickable = false
        waveform.setOnTouchListener { _, _ -> true } // глотаем все тапы/лонгтапы

        // --- CONFIRM (синяя с галочкой) ---
        confirmBtn.apply {
            text = "✓"
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
            minWidth = dp(40f)
            minHeight = dp(28f)
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(Color.parseColor("#1976D2"))
            }
        }
        addView(
            confirmBtn,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(8f)
            }
        )

        // --- CANCEL (красная) ---
        cancelBtn.apply {
            text = "Cancel"
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
        addView(
            cancelBtn,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(8f)
            }
        )

        alpha = 0f // для fade-in
    }

    fun setOnConfirm(action: () -> Unit) { confirmBtn.setOnClickListener { action() } }
    fun setOnCancel(action: () -> Unit)  { cancelBtn.setOnClickListener  { action() } }

    fun fadeIn(duration: Long = 200L) {
        anim?.cancel()
        anim = animate().alpha(1f).setDuration(duration)
        anim?.start()
    }
}
