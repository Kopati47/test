package com.example.test.overlay.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewPropertyAnimator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.R
import com.example.test.overlay.util.dp

class PermissionPanelView(context: Context) : LinearLayout(context) {

    private val allowBtn = TextView(context)
    private var anim: ViewPropertyAnimator? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_record_panel)
        setPadding(dp(10f), dp(6f), dp(10f), dp(6f))

        val tip = TextView(context).apply {
            text = "Нужно разрешение на микрофон"
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        addView(tip, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        allowBtn.apply {
            text = "Разрешить"
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            minWidth = dp(56f)
            minHeight = dp(28f)
            background = GradientDrawable().apply {
                cornerRadius = dp(10f).toFloat()
                setColor(Color.parseColor("#2E7D32"))
            }
        }
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        lp.leftMargin = dp(8f)
        addView(allowBtn, lp)

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
