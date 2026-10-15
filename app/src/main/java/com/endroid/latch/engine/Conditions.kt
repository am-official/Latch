package com.endroid.latch.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.endroid.latch.data.ConditionType
import com.endroid.latch.data.Rule
import java.util.Calendar

object Conditions {
    fun holds(context: Context, rule: Rule): Boolean {
        return when (rule.condition) {
            ConditionType.NONE -> true
            ConditionType.WIFI_ON -> wifiOn(context)
            ConditionType.WIFI_OFF -> !wifiOn(context)
            ConditionType.CHARGING -> charging(context)
            ConditionType.NOT_CHARGING -> !charging(context)
            ConditionType.SCREEN_ON -> screenOn(context)
            ConditionType.SCREEN_OFF -> !screenOn(context)
            ConditionType.BATTERY_BELOW -> batteryPct(context) <= rule.conditionValue.coerceAtLeast(1)
            ConditionType.BATTERY_ABOVE -> batteryPct(context) >= rule.conditionValue.coerceAtLeast(1)
            ConditionType.DAY_WEEKDAY -> {
                val d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                d in Calendar.MONDAY..Calendar.FRIDAY
            }
            ConditionType.DAY_WEEKEND -> {
                val d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                d == Calendar.SATURDAY || d == Calendar.SUNDAY
            }
        }
    }

    fun wifiOn(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= 23) {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        @Suppress("DEPRECATION")
        return cm.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
    }

    fun charging(context: Context): Boolean {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun batteryPct(context: Context): Int {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        return if (level >= 0) (level * 100) / scale else 50
    }

    fun screenOn(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }
}
