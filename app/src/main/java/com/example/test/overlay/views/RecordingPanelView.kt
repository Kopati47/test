package com.example.test.overlay.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.R
import com.example.test.overlay.util.dp
import androidx.core.graphics.toColorInt

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
class RecordingPanelView(context: Context) : LinearLayout(context) {
    val waveform = WaveformView(context)
    private val confirmBtn = TextView(context)
    private val cancelBtn  = TextView(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_record_panel)
        setPadding(dp(10f), dp(6f), dp(10f), dp(6f))

        isClickable = true
        isLongClickable = false

        addView(waveform, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        waveform.isClickable = true
        waveform.isLongClickable = false
        waveform.setOnTouchListener { _, _ -> true }

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
                setColor("#1976D2".toColorInt())
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
                setColor("#D32F2F".toColorInt())
            }
        }
        addView(
            cancelBtn,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(8f)
            }
        )

        alpha = 0f
    }

    fun setOnConfirm(action: () -> Unit) { confirmBtn.setOnClickListener { action() } }
    fun setOnCancel(action: () -> Unit)  { cancelBtn.setOnClickListener  { action() } }
}
