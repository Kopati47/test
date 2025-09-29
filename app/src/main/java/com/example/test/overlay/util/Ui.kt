package com.example.test.overlay.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.View

fun Context.dp(v: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

fun Context.dpF(v: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

fun blendColor(c1: Int, c2: Int, tRaw: Float): Int {
    val t = tRaw.coerceIn(0f, 1f)
    val a = (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * t).toInt()
    val r = (Color.red(c1)   + (Color.red(c2)   - Color.red(c1))   * t).toInt()
    val g = (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt()
    val b = (Color.blue(c1)  + (Color.blue(c2)  - Color.blue(c1))  * t).toInt()
    return Color.argb(a, r, g, b)
}

fun View.dp(v: Float): Int = context.dp(v)
fun View.dpF(v: Float): Float = context.dpF(v)