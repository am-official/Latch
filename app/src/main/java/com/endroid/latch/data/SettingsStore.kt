package com.endroid.latch.data

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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

    var debugWhy: Boolean
        get() = p.getBoolean("debug_why", true)
        set(v) { p.edit().putBoolean("debug_why", v).apply() }

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(p.getString("theme_mode", "SYSTEM")!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        set(v) { p.edit().putString("theme_mode", v.name).apply() }

    /** ARGB seed without alpha emphasis; default teal */
    var seedColor: Long
        get() = p.getLong("seed_color", 0xFF006A6AL)
        set(v) { p.edit().putLong("seed_color", v).apply() }

    var activeProfileId: String
        get() = p.getString("active_profile", "default") ?: "default"
        set(v) { p.edit().putString("active_profile", v).apply() }

    var automationPaused: Boolean
        get() = p.getBoolean("automation_paused", false)
        set(v) { p.edit().putBoolean("automation_paused", v).apply() }

    var monitorHeals: Int
        get() = p.getInt("monitor_heals", 0)
        set(v) { p.edit().putInt("monitor_heals", v).apply() }

    var lastMonitorStartMs: Long
        get() = p.getLong("last_monitor_start", 0L)
        set(v) { p.edit().putLong("last_monitor_start", v).apply() }

    var runLog: String
        get() = p.getString("run_log", "") ?: ""
        set(v) { p.edit().putString("run_log", v.takeLast(12000)).apply() }

    var whyLog: String
        get() = p.getString("why_log", "") ?: ""
        set(v) { p.edit().putString("why_log", v.takeLast(12000)).apply() }

    fun appendLog(line: String) {
        if (!logRuns) return
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        runLog = "$ts  $line\n$runLog"
    }

    fun appendWhy(line: String) {
        if (!debugWhy) return
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        whyLog = "$ts  $line\n$whyLog"
    }
}
