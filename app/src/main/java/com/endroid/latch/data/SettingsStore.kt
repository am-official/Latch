package com.endroid.latch.data

import android.content.Context

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("latch_settings", Context.MODE_PRIVATE)

    var elevatedShell: Boolean
        get() = p.getBoolean("elevated_shell", false)
        set(v) { p.edit().putBoolean("elevated_shell", v).apply() }

    var preferShizuku: Boolean
        get() = p.getBoolean("prefer_shizuku", true)
        set(v) { p.edit().putBoolean("prefer_shizuku", v).apply() }

    var pluginApiEnabled: Boolean
        get() = p.getBoolean("plugin_api", true)
        set(v) { p.edit().putBoolean("plugin_api", v).apply() }

    var logRuns: Boolean
        get() = p.getBoolean("log_runs", true)
        set(v) { p.edit().putBoolean("log_runs", v).apply() }

    var runLog: String
        get() = p.getString("run_log", "") ?: ""
        set(v) { p.edit().putString("run_log", v.takeLast(8000)).apply() }

    fun appendLog(line: String) {
        if (!logRuns) return
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        runLog = "$ts  $line\n$runLog"
    }
}
