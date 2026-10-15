package com.endroid.latch.data

import android.content.Context

/** App-level options (not per-rule). */
class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("latch_settings", Context.MODE_PRIVATE)

    var elevatedShell: Boolean
        get() = p.getBoolean("elevated_shell", false)
        set(v) { p.edit().putBoolean("elevated_shell", v).apply() }

    var pluginApiEnabled: Boolean
        get() = p.getBoolean("plugin_api", true)
        set(v) { p.edit().putBoolean("plugin_api", v).apply() }

    var defaultCooldownSec: Int
        get() = p.getInt("default_cooldown", 0)
        set(v) { p.edit().putInt("default_cooldown", v.coerceAtLeast(0)).apply() }
}
