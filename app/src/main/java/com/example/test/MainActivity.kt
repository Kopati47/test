package com.example.test

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.test.overlay.OverlayAccessibilityService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var enabled by remember { mutableStateOf(isServiceEnabled()) }

                fun openAccessibilitySettings() {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }

                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { openAccessibilitySettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (enabled) "Open Accessibility settings"
                            else "Enable the service in Special Features")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { enabled = isServiceEnabled() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Check status")
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(if (enabled) "Status: service enabled" else "Status: service is off")
                    }
                }
            }
        }
    }

    private fun isServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, OverlayAccessibilityService::class.java)
        return flat.split(':').any { it.equals(cn.flattenToString(), ignoreCase = true) }
    }
}
