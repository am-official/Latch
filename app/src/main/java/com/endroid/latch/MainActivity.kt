package com.endroid.latch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.endroid.latch.data.SettingsStore
import com.endroid.latch.engine.AppWatchService
import com.endroid.latch.engine.Elevated
import com.endroid.latch.engine.LatchMonitorService
import com.endroid.latch.engine.TimeScheduler
import com.endroid.latch.ui.LatchApp
import com.endroid.latch.ui.LatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Elevated.attachListeners()
        TimeScheduler.rescheduleAll(this)
        LatchMonitorService.refresh(this)
        AppWatchService.refresh(this)
        setContent {
            val store = remember { SettingsStore(this) }
            var themeMode by remember { mutableStateOf(store.themeMode) }
            var seed by remember { mutableStateOf(store.seedColor) }
            LatchTheme(themeMode = themeMode, seedColor = seed) {
                LatchApp(
                    onThemeChanged = {
                        themeMode = store.themeMode
                        seed = store.seedColor
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LatchMonitorService.refresh(this)
    }
}
