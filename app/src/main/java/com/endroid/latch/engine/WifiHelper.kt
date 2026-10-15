package com.endroid.latch.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

object WifiHelper {
    /** Current + configured network names (best-effort; needs location on many OEMs). */
    fun knownSsids(context: Context): List<String> {
        val out = linkedSetOf<String>()
        val wm = try {
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        } catch (_: Exception) {
            return emptyList()
        }
        try {
            @Suppress("DEPRECATION")
            val cur = wm.connectionInfo?.ssid?.trim('"')
            if (!cur.isNullOrBlank() && cur != "<unknown ssid>") out += cur
        } catch (_: Exception) { }
        try {
            @Suppress("DEPRECATION")
            wm.configuredNetworks?.forEach { cfg ->
                val s = cfg.SSID?.trim('"')
                if (!s.isNullOrBlank()) out += s
            }
        } catch (_: Exception) { }
        try {
            @Suppress("DEPRECATION")
            wm.scanResults?.forEach { r ->
                val s = r.SSID?.trim()
                if (!s.isNullOrBlank()) out += s
            }
        } catch (_: Exception) { }
        return out.toList().sorted()
    }

    fun hasLocation(context: Context): Boolean {
        val f = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val c = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return f == PackageManager.PERMISSION_GRANTED || c == PackageManager.PERMISSION_GRANTED
    }
}
