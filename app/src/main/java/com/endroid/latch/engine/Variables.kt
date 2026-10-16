package com.endroid.latch.engine

import android.content.Context
import android.net.wifi.WifiManager
import com.endroid.latch.receiver.PowerReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Variables {
    fun expand(context: Context, input: String): String {
        if (input.isEmpty() || !input.contains('%')) return input
        val battery = PowerReceiver.batteryPercent(context, null).coerceAtLeast(0)
        val ssid = try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wm.connectionInfo?.ssid?.trim('"')?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "unknown"
        } catch (_: Exception) { "unknown" }
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return input
            .replace("%battery", battery.toString(), ignoreCase = true)
            .replace("%ssid", ssid, ignoreCase = true)
            .replace("%time", time, ignoreCase = true)
            .replace("%date", date, ignoreCase = true)
            .replace("%profile", SettingsActive.profileName(context), ignoreCase = true)
    }
}

object SettingsActive {
    fun profileName(context: Context): String {
        val id = com.endroid.latch.data.SettingsStore(context).activeProfileId
        return com.endroid.latch.data.ProfileStore(context).load().find { it.id == id }?.name ?: id
    }
}
