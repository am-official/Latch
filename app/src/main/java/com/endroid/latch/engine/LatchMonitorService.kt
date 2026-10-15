package com.endroid.latch.engine

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.endroid.latch.R
import com.endroid.latch.data.RuleStore
import com.endroid.latch.receiver.ConnectivityReceiver
import com.endroid.latch.receiver.HeadsetReceiver
import com.endroid.latch.receiver.PowerReceiver
import com.endroid.latch.receiver.TimeTickReceiver

/**
 * Keeps system receivers registered while any rule is enabled.
 * Without this, battery/wifi/headset only worked while MainActivity was open.
 */
class LatchMonitorService : Service() {
    private val power = PowerReceiver()
    private val headset = HeadsetReceiver()
    private val timeTick = TimeTickReceiver()
    private val connectivity = ConnectivityReceiver()
    private var registered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        RuleEngine.ensureChannel(this)
        val notif: Notification = NotificationCompat.Builder(this, RuleEngine.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.monitor_title))
            .setContentText(getString(R.string.monitor_text))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(41, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(41, notif)
        }
        registerAll()
        // Evaluate battery immediately so "battery below X" works without waiting for a change
        val pct = PowerReceiver.batteryPercent(this, null)
        if (pct >= 0) RuleEngine.onBattery(this, pct, force = true)
        return START_STICKY
    }

    private fun registerAll() {
        if (registered) return
        val powerFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        ContextCompat.registerReceiver(this, power, powerFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(
            this, headset, IntentFilter(Intent.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, timeTick, timeFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val netFilter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(this, connectivity, netFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        registered = true
    }

    override fun onDestroy() {
        if (registered) {
            runCatching { unregisterReceiver(power) }
            runCatching { unregisterReceiver(headset) }
            runCatching { unregisterReceiver(timeTick) }
            runCatching { unregisterReceiver(connectivity) }
            registered = false
        }
        super.onDestroy()
    }

    companion object {
        fun refresh(context: android.content.Context) {
            val needs = RuleStore(context).load().any { it.enabled }
            val i = Intent(context, LatchMonitorService::class.java)
            if (needs) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i)
                    else context.startService(i)
                } catch (_: Exception) { }
            } else {
                context.stopService(i)
            }
        }
    }
}
