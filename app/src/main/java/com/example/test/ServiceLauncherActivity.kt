package com.example.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.test.overlay.OverlayService

class ServiceLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // стартуем оверлей и сразу закрываемся
        val i = Intent(this, OverlayService::class.java)
        startForegroundService(i)
        finish() // НИКАКОГО UI
    }
}
