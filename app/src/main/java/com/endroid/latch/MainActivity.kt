package com.endroid.latch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.endroid.latch.engine.AppWatchService
import com.endroid.latch.engine.Elevated
import com.endroid.latch.engine.LatchMonitorService
import com.endroid.latch.engine.TimeScheduler
import com.endroid.latch.ui.LatchApp
import com.endroid.latch.ui.LatchTheme

/**
 * UI only. Trigger receivers live in [LatchMonitorService] so rules keep working
 * in the background without double-firing while the activity is open.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Elevated.attachListeners()
        TimeScheduler.rescheduleAll(this)
        LatchMonitorService.refresh(this)
        AppWatchService.refresh(this)
        setContent {
            LatchTheme { LatchApp() }
        }
    }

    override fun onResume() {
        super.onResume()
        LatchMonitorService.refresh(this)
    }
}
