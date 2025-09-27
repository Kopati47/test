package com.example.test.overlay.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.overlay.util.dp

class PermissionPanelView(context: Context) : LinearLayout(context) {

    private val allowBtn = TextView(context)
    private var anim: ViewPropertyAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val tip = TextView(context).apply {
            text = "Нужно разрешение на микрофон"
            setTextColor(Color.WHITE)
            textSize = 13f
        }
        val tipLp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        addView(tip, tipLp)

        allowBtn.text = "Разрешить"
        allowBtn.setTextColor(Color.WHITE)
        allowBtn.textSize = 13f
        allowBtn.setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
        allowBtn.background = GradientDrawable().apply {
            cornerRadius = dp(10f).toFloat()
            setColor(Color.parseColor("#2E7D32"))
        }
        addView(allowBtn)

        alpha = 0f
    }

    fun setOnAllow(action: () -> Unit) {
        allowBtn.setOnClickListener { action() }
    }

    fun fadeIn(duration: Long = 200L) {
        anim?.cancel()
        anim = animate().alpha(1f).setDuration(duration)
        anim?.start()
    }
}
