package com.example.test

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.example.test.overlay.OverlayService

class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Стартуем сервис оверлея и сразу закрываемся, без UI/лямбд:
        val svc = Intent(applicationContext, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(svc)
        } else {
            @Suppress("DEPRECATION")
            applicationContext.startService(svc)
        }
        finish()
    }
}
