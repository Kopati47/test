package com.example.test

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test.overlay.OverlayService
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val askNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore */ }

    private val askOverlayPerm = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* вернёмся и проверим в UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val canOverlay = remember { mutableStateOf(Settings.canDrawOverlays(this)) }

                fun startOverlay() {
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
                    }
                    val intent = Intent(this, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
                    canOverlay.value = true
                }

                fun stopOverlay() {
                    stopService(Intent(this, OverlayService::class.java))
                }

                LaunchedEffect(Unit) {
                    // автозапуск если уже выдано
                    if (Settings.canDrawOverlays(this@MainActivity)) startOverlay()
                }

                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { startOverlay() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Запустить Edge Line")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { stopOverlay() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Остановить")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (Settings.canDrawOverlays(this@MainActivity))
                                "Статус: разрешение на оверлей есть"
                            else
                                "Статус: нет разрешения на оверлей"
                        )
                    }
                }
            }
        }
    }
}
