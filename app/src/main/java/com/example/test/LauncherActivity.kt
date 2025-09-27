package com.example.test

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.test.overlay.OverlayService
import androidx.core.net.toUri

class LauncherActivity : ComponentActivity() {

    private val askNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startOverlayAndFinish() }

    private val askOverlayPerm = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { startOverlayAndFinish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startOverlayAndFinish()
    }

    private fun startOverlayAndFinish() {
        if (!Settings.canDrawOverlays(this)) {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            askOverlayPerm.launch(i)
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            askNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        finish()
    }
}
