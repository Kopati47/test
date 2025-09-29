package com.example.test.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.test.overlay.net.WebSocketStreamer
import com.example.test.overlay.views.PermissionPanelView
import com.example.test.overlay.views.RecordingPanelView

class OverlayAccessibilityService : AccessibilityService() {

    // ---- настройки линии (твоё)
    private val EDGE_OFFSET_DP = 9f
    private val TOP_MARGIN_DP = 21f
    private val BOTTOM_MARGIN_DP = 100f
    private val TOUCH_WIDTH_DP = 28f
    private val TOUCH_HEIGHT_DP = 60f
    private val TOUCH_MAX_HEIGHT_DP = 120f
    private val LINE_WIDTH_DP = 4f
    private val LINE_HEIGHT_DP = 45f
    private val LINE_MAX_HEIGHT_DP = 100f
    private val START_FROM_BOTTOM_GAP_DP = 215f
    private val STRETCH_RATIO = 0.5f
    private val MAX_STRETCH_DP = 260f
    private val H_LOCK_THRESHOLD_DP = 8f
    private val HEIGHT_GROW_START_DP = 20f
    private val ENTRY_ANIM_DURATION_MS = 260L
    private val AUTO_TRIGGER_DP = 90f
    private val AUTO_ANIM_DURATION_MS = 180L
    private val COLOR_START_HEX = 0x55FFFFFF
    private val COLOR_TARGET_HEX = 0xFF404040.toInt()
    private val RIGHT_SIDE = false

    private val WS_URL =
        "wss://mysecretar.duckdns.org/stream?sr=44100&ch=1&token=ADAeDttSGUs7NJKd8e6pH19bXr8NlQS06vh1CcnuweaJJIg6TV"

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var container: FrameLayout
    private var controller: EdgeBarController? = null

    private var recorder: RecordingEngine? = null
    private var wsStreamer: WebSocketStreamer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    // AccessibilityService требует этот метод
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* не используется */ }
    override fun onInterrupt() { /* не используется */ }

    override fun onServiceConnected() {
        super.onServiceConnected()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // ключевое отличие: overlay спецвозможностей
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.START or Gravity.TOP }

        container = FrameLayout(this)
        wm.addView(container, params)

        controller = EdgeBarController(
            wm, container, params,
            EDGE_OFFSET_DP, TOP_MARGIN_DP, BOTTOM_MARGIN_DP,
            TOUCH_WIDTH_DP, TOUCH_HEIGHT_DP, TOUCH_MAX_HEIGHT_DP,
            LINE_WIDTH_DP, LINE_HEIGHT_DP, LINE_MAX_HEIGHT_DP,
            START_FROM_BOTTOM_GAP_DP,
            STRETCH_RATIO, MAX_STRETCH_DP, H_LOCK_THRESHOLD_DP, HEIGHT_GROW_START_DP,
            ENTRY_ANIM_DURATION_MS, AUTO_TRIGGER_DP, AUTO_ANIM_DURATION_MS,
            COLOR_START_HEX, COLOR_TARGET_HEX,
            onAutoCompleted = { p, w, h -> showRecordingOrPermissionInsideLine(p, w, h) },
            rightSide = RIGHT_SIDE   // ← ВАЖНО
        ).also { it.attach() }

    }

    override fun onDestroy() {
        stopStreaming()
        pollRunnable?.let { handler.removeCallbacks(it) }
        controller?.detach()
        runCatching { wm.removeView(container) }
        super.onDestroy()
    }

    // ---------- панели ----------
    private fun showRecordingOrPermissionInsideLine(padStartPx: Int, lineW: Int, lineH: Int) {
        val granted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) showRecordingPanel(padStartPx, lineW, lineH)
        else showPermissionPanel(padStartPx, lineW, lineH)
    }

    private fun showRecordingPanel(padStartPx: Int, lineW: Int, lineH: Int) {
        container.clipChildren = false
        container.clipToPadding = false
        container.setOnTouchListener(null)

        val panel = RecordingPanelView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                lineW, lineH, Gravity.START or Gravity.CENTER_VERTICAL
            ).apply { marginStart = padStartPx }

            val revert: () -> Unit = {
                stopStreaming()
                container.removeAllViews()
                controller = EdgeBarController(
                    wm, container, params,
                    EDGE_OFFSET_DP, TOP_MARGIN_DP, BOTTOM_MARGIN_DP,
                    TOUCH_WIDTH_DP, TOUCH_HEIGHT_DP, TOUCH_MAX_HEIGHT_DP,
                    LINE_WIDTH_DP, LINE_HEIGHT_DP, LINE_MAX_HEIGHT_DP,
                    START_FROM_BOTTOM_GAP_DP,
                    STRETCH_RATIO, MAX_STRETCH_DP, H_LOCK_THRESHOLD_DP, HEIGHT_GROW_START_DP,
                    ENTRY_ANIM_DURATION_MS, AUTO_TRIGGER_DP, AUTO_ANIM_DURATION_MS,
                    COLOR_START_HEX, COLOR_TARGET_HEX,
                    onAutoCompleted = { p, w, h ->
                        showRecordingOrPermissionInsideLine(p, w, h)
                    },
                    rightSide = RIGHT_SIDE
                ).also { it.attach() }
            }

            setOnCancel {
                wsStreamer?.sendCancel()
                handler.postDelayed({
                    stopStreaming()
                    revert()
                }, 120)
            }
            setOnConfirm { revert() }
        }

        crossFadeReplaceWithPanel(panel, 220L)

        wsStreamer = WebSocketStreamer(WS_URL).also { it.start() }
        recorder = RecordingEngine(
            onLevel = { level -> panel.waveform.push(level) },
            onChunk = { chunk, n -> wsStreamer?.sendChunk(chunk, n) },
            onError = { /* лог по желанию */ },
            appContext = applicationContext
        ).also { it.start() }
    }

    private fun stopStreaming() {
        recorder?.stop()
        recorder = null
        wsStreamer?.stop()
        wsStreamer = null
    }

    private fun showPermissionPanel(padStartPx: Int, lineW: Int, lineH: Int) {
        container.clipChildren = false
        container.clipToPadding = false
        container.setOnTouchListener(null)

        val panel = PermissionPanelView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                lineW, lineH, Gravity.START or Gravity.CENTER_VERTICAL
            ).apply { marginStart = padStartPx }
            setOnAllow { openAppSettingsAndPoll(padStartPx, lineW, lineH) }
        }
        crossFadeReplaceWithPanel(panel, 200L)
    }

    private fun openAppSettingsAndPoll(padStartPx: Int, lineW: Int, lineH: Int) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(i)

        pollRunnable?.let { handler.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                val granted = ContextCompat.checkSelfPermission(
                    this@OverlayAccessibilityService, android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) showRecordingPanel(padStartPx, lineW, lineH)
                else handler.postDelayed(this, 600)
            }
        }
        pollRunnable = r
        handler.postDelayed(r, 800)
    }

    private fun crossFadeReplaceWithPanel(panel: View, duration: Long) {
        val old = if (container.childCount > 0) container.getChildAt(0) else null
        panel.alpha = 0f
        container.addView(panel)
        wm.updateViewLayout(container, params)
        panel.animate()
            .alpha(1f)
            .setDuration(duration)
            .withEndAction { old?.let { runCatching { container.removeView(it) } } }
            .start()
    }
}
