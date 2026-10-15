package com.endroid.latch

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.endroid.latch.receiver.HeadsetReceiver
import com.endroid.latch.receiver.PowerReceiver
import com.endroid.latch.receiver.TimeTickReceiver
import com.endroid.latch.ui.LatchApp
import com.endroid.latch.ui.LatchTheme

class MainActivity : ComponentActivity() {
    private val powerReceiver = PowerReceiver()
    private val headsetReceiver = HeadsetReceiver()
    private val timeTickReceiver = TimeTickReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerRuntimeReceivers()
        setContent {
            LatchTheme {
                LatchApp()
            }
        }
    }

    private fun registerRuntimeReceivers() {
        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        ContextCompat.registerReceiver(
            this, powerReceiver, powerFilter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            headsetReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this, timeTickReceiver, timeFilter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(powerReceiver) }
        runCatching { unregisterReceiver(headsetReceiver) }
        runCatching { unregisterReceiver(timeTickReceiver) }
        super.onDestroy()
    }
}
