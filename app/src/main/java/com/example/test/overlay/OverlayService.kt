package com.example.test.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.test.R
import com.example.test.overlay.views.PermissionPanelView
import com.example.test.overlay.views.RecordingPanelView

class OverlayService : Service() {

    // --- настройки
    private val EDGE_OFFSET_DP           = 9f
    private val TOP_MARGIN_DP            = 21f
    private val BOTTOM_MARGIN_DP         = 100f

    private val TOUCH_WIDTH_DP           = 28f
    private val TOUCH_HEIGHT_DP          = 60f
    private val TOUCH_MAX_HEIGHT_DP      = 120f

    private val LINE_WIDTH_DP            = 4f
    private val LINE_HEIGHT_DP           = 45f
    private val LINE_MAX_HEIGHT_DP       = 100f

    private val START_FROM_BOTTOM_GAP_DP = 215f
    private val STRETCH_RATIO            = 0.5f
    private val MAX_STRETCH_DP           = 260f
    private val H_LOCK_THRESHOLD_DP      = 8f
    private val HEIGHT_GROW_START_DP     = 20f
    private val ENTRY_ANIM_DURATION_MS   = 260L
    private val AUTO_TRIGGER_DP          = 90f
    private val AUTO_ANIM_DURATION_MS    = 180L

    private val COLOR_START_HEX          = 0x55FFFFFF
    private val COLOR_TARGET_HEX         = 0xFF404040.toInt()

    // ---
    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var container: FrameLayout
    private var controller: EdgeBarController? = null

    private val handler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null
    private var recorder: RecordingEngine? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
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
            COLOR_START_HEX, COLOR_TARGET_HEX
        ) { padStartPx, lineW, lineH ->
            // после полного растяжения подменяем линию панелью
            showRecordingOrPermissionInsideLine(padStartPx, lineW, lineH)
        }.also { it.attach() }
    }

    override fun onDestroy() {
        stopRecording()
        pollRunnable?.let { handler.removeCallbacks(it) }
        controller?.detach()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val chId = "edge_line_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(chId, "Edge Line", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n: Notification = NotificationCompat.Builder(this, chId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Edge Line активна")
            .setOngoing(true)
            .build()
        startForeground(1, n)
    }

    // ---------- кросс-фейд (убирает blink) ----------
    private fun crossFadeReplaceWithPanel(panel: View, duration: Long = 200L) {
        val old = if (container.childCount > 0) container.getChildAt(0) else null
        panel.alpha = 0f
        container.addView(panel)                 // кладём поверх линии
        wm.updateViewLayout(container, params)
        panel.animate()
            .alpha(1f)
            .setDuration(duration)
            .withEndAction { old?.let { runCatching { container.removeView(it) } } }
            .start()
    }

    // ---------- панели внутри линии ----------
    private fun showRecordingOrPermissionInsideLine(padStartPx: Int, lineW: Int, lineH: Int) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            showRecordingPanel(padStartPx, lineW, lineH)
        } else {
            showPermissionPanel(padStartPx, lineW, lineH)
        }
    }

    private fun showRecordingPanel(padStartPx: Int, lineW: Int, lineH: Int) {
        container.clipChildren = false
        container.clipToPadding = false
        container.setOnTouchListener(null)   // после раскрытия — никаких жестов перетаскивания

        val panel = RecordingPanelView(this).apply {
            // Панель строго ВНУТРИ линии
            layoutParams = FrameLayout.LayoutParams(
                lineW, lineH,
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply { marginStart = padStartPx }

            // два действия: Confirm (✓) и Cancel
            val revert: () -> Unit = {
                stopRecording()
                container.removeAllViews()
                controller = EdgeBarController(
                    wm, container, params,
                    EDGE_OFFSET_DP, TOP_MARGIN_DP, BOTTOM_MARGIN_DP,
                    TOUCH_WIDTH_DP, TOUCH_HEIGHT_DP, TOUCH_MAX_HEIGHT_DP,
                    LINE_WIDTH_DP, LINE_HEIGHT_DP, LINE_MAX_HEIGHT_DP,
                    START_FROM_BOTTOM_GAP_DP,
                    STRETCH_RATIO, MAX_STRETCH_DP, H_LOCK_THRESHOLD_DP, HEIGHT_GROW_START_DP,
                    ENTRY_ANIM_DURATION_MS, AUTO_TRIGGER_DP, AUTO_ANIM_DURATION_MS,
                    COLOR_START_HEX, COLOR_TARGET_HEX
                ) { p, w, h -> showRecordingOrPermissionInsideLine(p, w, h) }.also { it.attach() }
            }
            setOnConfirm { revert() }
            setOnCancel  { revert() }
        }

        // кросс-фейд без мигания
        crossFadeReplaceWithPanel(panel, duration = 220L)

        // запуск записи и подача уровней в waveform
        recorder = RecordingEngine { level -> panel.waveform.push(level) }.also { it.start() }
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder = null
    }

    private fun showPermissionPanel(padStartPx: Int, lineW: Int, lineH: Int) {
        container.clipChildren = false
        container.clipToPadding = false
        container.setOnTouchListener(null)   // тоже снимаем перетаскивание

        val panel = PermissionPanelView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                lineW, lineH,
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply { marginStart = padStartPx }
            setOnAllow { openAppSettingsAndPoll(padStartPx, lineW, lineH) }
        }

        crossFadeReplaceWithPanel(panel, duration = 200L)
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
                if (ContextCompat.checkSelfPermission(
                        this@OverlayService, android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED) {
                    showRecordingPanel(padStartPx, lineW, lineH)
                } else handler.postDelayed(this, 600)
            }
        }
        pollRunnable = r
        handler.postDelayed(r, 800)
    }
}
