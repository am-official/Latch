package com.endroid.latch

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.endroid.latch.engine.AppWatchService
import com.endroid.latch.engine.LatchMonitorService
import com.endroid.latch.engine.TimeScheduler
import com.endroid.latch.receiver.ConnectivityReceiver
import com.endroid.latch.receiver.HeadsetReceiver
import com.endroid.latch.receiver.PowerReceiver
import com.endroid.latch.receiver.TimeTickReceiver
import com.endroid.latch.ui.LatchApp
import com.endroid.latch.ui.LatchTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val powerReceiver = PowerReceiver()
    private val headsetReceiver = HeadsetReceiver()
    private val timeTickReceiver = TimeTickReceiver()
    private val connectivityReceiver = ConnectivityReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerRuntimeReceivers()
        TimeScheduler.rescheduleAll(this)
        AppWatchService.refresh(this)
        LatchMonitorService.refresh(this)
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // user enables from Settings
            }
        } catch (_: Throwable) { }
        setContent {
            LatchTheme { LatchApp() }
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
        ContextCompat.registerReceiver(this, powerReceiver, powerFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        ContextCompat.registerReceiver(
            this, headsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, timeTickReceiver, timeFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val netFilter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addDataScheme("file")
        }
        ContextCompat.registerReceiver(this, connectivityReceiver, netFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(powerReceiver) }
        runCatching { unregisterReceiver(headsetReceiver) }
        runCatching { unregisterReceiver(timeTickReceiver) }
        runCatching { unregisterReceiver(connectivityReceiver) }
        super.onDestroy()
    }
}
