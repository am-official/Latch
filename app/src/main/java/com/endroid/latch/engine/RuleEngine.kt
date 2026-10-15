package com.endroid.latch.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.endroid.latch.R
import com.endroid.latch.data.ActionType
import com.endroid.latch.data.RingerModeKind
import com.endroid.latch.data.Rule
import com.endroid.latch.data.RuleStore
import com.endroid.latch.data.SettingsStore
import com.endroid.latch.data.TriggerType
import kotlin.concurrent.thread

object RuleEngine {
    const val CHANNEL_ID = "latch_actions"
    private const val PREFS = "latch_engine"
    private const val TAG = "LatchEngine"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_actions),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.channel_actions_desc) },
            )
        }
    }

    private fun cooldownOk(context: Context, rule: Rule): Boolean {
        if (rule.cooldownSec <= 0) return true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong("cd_${rule.id}", 0L)
        val now = System.currentTimeMillis()
        if (now - last < rule.cooldownSec * 1000L) return false
        prefs.edit().putLong("cd_${rule.id}", now).apply()
        return true
    }

    fun onBattery(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val bucket = percent / 5
        if (prefs.getInt("bat_bucket", -1) == bucket) return
        prefs.edit().putInt("bat_bucket", bucket).apply()
        match(context) { r ->
            when (r.trigger) {
                TriggerType.BATTERY_BELOW -> percent <= r.triggerValue
                TriggerType.BATTERY_ABOVE -> percent >= r.triggerValue
                else -> false
            }
        }
    }

    fun onPowerConnected(context: Context, connected: Boolean) {
        fire(context, if (connected) TriggerType.CHARGER_CONNECTED else TriggerType.CHARGER_DISCONNECTED)
    }

    fun onHeadphones(context: Context, connected: Boolean) {
        fire(context, if (connected) TriggerType.HEADPHONES_CONNECTED else TriggerType.HEADPHONES_DISCONNECTED)
    }

    fun onBoot(context: Context) = fire(context, TriggerType.BOOT_COMPLETED)

    fun onWifi(context: Context, connected: Boolean, ssid: String?) {
        if (connected) {
            fire(context, TriggerType.WIFI_CONNECTED)
            if (!ssid.isNullOrBlank()) {
                match(context) {
                    it.trigger == TriggerType.WIFI_SSID &&
                        it.triggerParam.isNotBlank() &&
                        ssid.trim('"').equals(it.triggerParam.trim('"'), ignoreCase = true)
                }
            }
        } else {
            fire(context, TriggerType.WIFI_DISCONNECTED)
        }
    }

    fun onBluetooth(context: Context, connected: Boolean) {
        fire(context, if (connected) TriggerType.BLUETOOTH_CONNECTED else TriggerType.BLUETOOTH_DISCONNECTED)
    }

    fun onScreen(context: Context, on: Boolean) {
        fire(context, if (on) TriggerType.SCREEN_ON else TriggerType.SCREEN_OFF)
    }

    fun onAppOpened(context: Context, packageName: String) {
        match(context) {
            it.trigger == TriggerType.APP_OPENED &&
                it.triggerParam.isNotBlank() &&
                it.triggerParam.equals(packageName, ignoreCase = true)
        }
    }

    fun onCustomBroadcast(context: Context, action: String) {
        match(context) {
            it.trigger == TriggerType.CUSTOM_BROADCAST &&
                it.triggerParam.isNotBlank() &&
                it.triggerParam == action
        }
    }

    fun onTimeTick(context: Context, hour: Int, minute: Int) {
        match(context) {
            it.trigger == TriggerType.TIME_DAILY &&
                it.triggerValue == hour && it.triggerValue2 == minute
        }
    }

    private fun fire(context: Context, type: TriggerType) {
        match(context) { it.trigger == type }
    }

    private fun match(context: Context, pred: (Rule) -> Boolean) {
        RuleStore(context).load()
            .filter { it.enabled && pred(it) && cooldownOk(context, it) }
            .forEach { run(context, it) }
    }

    fun run(context: Context, rule: Rule) {
        ensureChannel(context)
        val elevated = SettingsStore(context).elevatedShell
        thread(name = "latch-rule-${rule.id.take(6)}") {
            for (action in rule.actions) {
                try {
                    execute(context, rule, action, elevated)
                } catch (e: Exception) {
                    Log.w(TAG, "action failed: ${action.type}", e)
                }
            }
        }
    }

    private fun execute(context: Context, rule: Rule, action: com.endroid.latch.data.RuleAction, elevated: Boolean) {
        when (action.type) {
            ActionType.WAIT_MS -> {
                val ms = action.param.toLongOrNull()?.coerceIn(0, 60_000) ?: 0L
                if (ms > 0) Thread.sleep(ms)
            }
            ActionType.TOAST -> {
                val msg = action.text.ifBlank { rule.name }
                android.os.Handler(context.mainLooper).post {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            ActionType.NOTIFY -> {
                val title = action.title.ifBlank { rule.name }
                val body = action.text.ifBlank { rule.triggerSummary() }
                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .build()
                try {
                    NotificationManagerCompat.from(context)
                        .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
                } catch (_: SecurityException) { }
            }
            ActionType.RINGER_MODE -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val mode = when (action.ringerMode) {
                    RingerModeKind.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                    RingerModeKind.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                    RingerModeKind.SILENT -> AudioManager.RINGER_MODE_SILENT
                }
                try { am.ringerMode = mode } catch (_: SecurityException) { }
            }
            ActionType.OPEN_APP -> {
                val pkg = action.param.trim()
                if (pkg.isEmpty()) return
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                }
            }
            ActionType.OPEN_URL -> {
                val url = action.param.trim().ifBlank { action.text.trim() }
                if (url.isEmpty()) return
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
            ActionType.SET_BRIGHTNESS -> {
                val pct = action.param.toIntOrNull()?.coerceIn(1, 100) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.System.canWrite(context)
                ) return
                val value = (pct / 100f * 255).toInt().coerceIn(1, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    value,
                )
            }
            ActionType.SHELL -> {
                val cmd = action.param.trim().ifBlank { action.text.trim() }
                if (cmd.isEmpty()) return
                runShell(cmd, elevated)
            }
            ActionType.PLUGIN_BROADCAST -> {
                if (!SettingsStore(context).pluginApiEnabled) return
                val act = action.param.trim().ifBlank { "com.endroid.latch.PLUGIN_EVENT" }
                val i = Intent(act).apply {
                    putExtra("rule_id", rule.id)
                    putExtra("rule_name", rule.name)
                    putExtra("text", action.text)
                    setPackage(null)
                }
                context.sendBroadcast(i)
            }
        }
    }

    private fun runShell(cmd: String, elevated: Boolean) {
        val full = if (elevated) arrayOf("su", "-c", cmd) else arrayOf("sh", "-c", cmd)
        try {
            val p = Runtime.getRuntime().exec(full)
            p.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "shell failed elevated=$elevated", e)
            if (elevated) {
                try {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
                } catch (_: Exception) { }
            }
        }
    }
}
