package com.endroid.latch.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    private fun eligible(context: Context, rule: Rule): Boolean =
        rule.enabled && Conditions.holds(context, rule) && cooldownOk(context, rule)

    fun onBattery(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val bucket = percent / 5
        if (prefs.getInt("bat_bucket", -1) == bucket) return
        prefs.edit().putInt("bat_bucket", bucket).apply()
        match(context) {
            when (it.trigger) {
                TriggerType.BATTERY_BELOW -> percent <= it.triggerValue
                TriggerType.BATTERY_ABOVE -> percent >= it.triggerValue
                else -> false
            }
        }
    }

    fun onPowerConnected(context: Context, connected: Boolean) =
        fire(context, if (connected) TriggerType.CHARGER_CONNECTED else TriggerType.CHARGER_DISCONNECTED)

    fun onHeadphones(context: Context, connected: Boolean) =
        fire(context, if (connected) TriggerType.HEADPHONES_CONNECTED else TriggerType.HEADPHONES_DISCONNECTED)

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
        } else fire(context, TriggerType.WIFI_DISCONNECTED)
    }

    fun onBluetooth(context: Context, connected: Boolean) =
        fire(context, if (connected) TriggerType.BLUETOOTH_CONNECTED else TriggerType.BLUETOOTH_DISCONNECTED)

    fun onScreen(context: Context, on: Boolean) =
        fire(context, if (on) TriggerType.SCREEN_ON else TriggerType.SCREEN_OFF)

    fun onAppOpened(context: Context, packageName: String) =
        match(context) {
            it.trigger == TriggerType.APP_OPENED &&
                it.triggerParam.equals(packageName, ignoreCase = true)
        }

    fun onCustomBroadcast(context: Context, action: String) =
        match(context) { it.trigger == TriggerType.CUSTOM_BROADCAST && it.triggerParam == action }

    fun onAirplane(context: Context, on: Boolean) =
        fire(context, if (on) TriggerType.AIRPLANE_ON else TriggerType.AIRPLANE_OFF)

    fun onMediaMounted(context: Context) = fire(context, TriggerType.MEDIA_MOUNTED)

    fun onTimeTick(context: Context, hour: Int, minute: Int) {
        match(context) {
            when (it.trigger) {
                TriggerType.TIME_DAILY ->
                    it.triggerValue == hour && it.triggerValue2 == minute
                TriggerType.TIME_WINDOW -> {
                    val endH = it.triggerParam.substringBefore(":").toIntOrNull() ?: 0
                    val endM = it.triggerParam.substringAfter(":").toIntOrNull() ?: 0
                    val now = hour * 60 + minute
                    val start = it.triggerValue * 60 + it.triggerValue2
                    val end = endH * 60 + endM
                    // fire once at window start only
                    now == start
                }
                TriggerType.DAY_OF_WEEK -> {
                    val cal = java.util.Calendar.getInstance()
                    val bit = 1 shl (cal.get(java.util.Calendar.DAY_OF_WEEK) - 1)
                    (it.triggerValue and bit) != 0 &&
                        it.triggerValue2 == hour // optional hour filter in value2; use -1?
                    // Simplify: day mask only, fire at noon once — skip unless also time
                    false
                }
                else -> false
            }
        }
    }

    private fun fire(context: Context, type: TriggerType) =
        match(context) { it.trigger == type }

    private fun match(context: Context, pred: (Rule) -> Boolean) {
        RuleStore(context).load()
            .filter { eligible(context, it) && pred(it) }
            .forEach { run(context, it) }
    }

    fun run(context: Context, rule: Rule) {
        ensureChannel(context)
        val settings = SettingsStore(context)
        settings.appendLog("RUN ${rule.name}")
        val preferShizuku = settings.preferShizuku
        val allowSu = settings.elevatedShell
        thread(name = "latch-${rule.id.take(6)}") {
            for (action in rule.actions) {
                try {
                    execute(context, rule, action, preferShizuku, allowSu)
                } catch (e: Exception) {
                    Log.w(TAG, "action ${action.type}", e)
                    settings.appendLog("ERR ${action.type}: ${e.message}")
                }
            }
        }
    }

    private fun execute(
        context: Context,
        rule: Rule,
        action: com.endroid.latch.data.RuleAction,
        preferShizuku: Boolean,
        allowSu: Boolean,
    ) {
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
                    .setContentTitle(title).setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true).build()
                try {
                    NotificationManagerCompat.from(context)
                        .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
                } catch (_: SecurityException) { }
            }
            ActionType.RINGER_MODE -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                try {
                    am.ringerMode = when (action.ringerMode) {
                        RingerModeKind.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                        RingerModeKind.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                        RingerModeKind.SILENT -> AudioManager.RINGER_MODE_SILENT
                    }
                } catch (_: SecurityException) { }
            }
            ActionType.OPEN_APP -> {
                val pkg = action.param.trim()
                context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            ActionType.OPEN_URL -> {
                val url = action.param.trim().ifBlank { action.text.trim() }
                if (url.isNotEmpty()) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            ActionType.SET_BRIGHTNESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) return
                val pct = action.param.toIntOrNull()?.coerceIn(1, 100) ?: return
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (pct / 100f * 255).toInt().coerceIn(1, 255),
                )
            }
            ActionType.SHELL -> {
                val cmd = action.param.trim().ifBlank { action.text.trim() }
                if (cmd.isEmpty()) return
                Elevated.runShell(cmd, preferShizuku, allowSu)
            }
            ActionType.PLUGIN_BROADCAST -> {
                if (!SettingsStore(context).pluginApiEnabled) return
                val act = action.param.trim().ifBlank { "com.endroid.latch.PLUGIN_EVENT" }
                context.sendBroadcast(
                    Intent(act).putExtra("rule_id", rule.id).putExtra("rule_name", rule.name)
                        .putExtra("text", action.text),
                )
            }
            ActionType.MEDIA_VOLUME -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val pct = action.param.toIntOrNull()?.coerceIn(0, 100) ?: return
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (pct / 100f * max).toInt(), 0)
            }
            ActionType.VIBRATE -> {
                val ms = action.param.toLongOrNull()?.coerceIn(50, 5000) ?: 200L
                val v = if (Build.VERSION.SDK_INT >= 31) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(ms)
                }
            }
            ActionType.CLIPBOARD -> {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(
                    android.content.ClipData.newPlainText("latch", action.text.ifBlank { action.param }),
                )
            }
            ActionType.FLASHLIGHT -> {
                val on = action.param.lowercase() != "off"
                try {
                    val cam = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val id = cam.cameraIdList.firstOrNull {
                        cam.getCameraCharacteristics(it)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } ?: return
                    cam.setTorchMode(id, on)
                } catch (_: Exception) { }
            }
            ActionType.WIFI_ENABLE -> {
                // Deprecated API — best effort via shell when elevated
                val enable = action.param.lowercase() != "false" && action.param.lowercase() != "off"
                val cmd = if (enable) "svc wifi enable" else "svc wifi disable"
                Elevated.runShell(cmd, preferShizuku, allowSu)
            }
            ActionType.BLUETOOTH_ENABLE -> {
                val enable = action.param.lowercase() != "false" && action.param.lowercase() != "off"
                val cmd = if (enable) "svc bluetooth enable" else "svc bluetooth disable"
                Elevated.runShell(cmd, preferShizuku, allowSu)
            }
            ActionType.SKIP_TO_NEXT -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val event = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
                am.dispatchMediaKeyEvent(event)
                am.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT),
                )
            }
        }
    }
}
