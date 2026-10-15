package com.endroid.latch.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.endroid.latch.R
import com.endroid.latch.data.ActionType
import com.endroid.latch.data.RingerModeKind
import com.endroid.latch.data.Rule
import com.endroid.latch.data.RuleStore
import com.endroid.latch.data.TriggerType

object RuleEngine {
    const val CHANNEL_ID = "latch_actions"
    private const val PREFS = "latch_engine"
    private const val KEY_LAST_BATTERY_BUCKET = "last_battery_bucket"
    private const val KEY_LAST_TIME_FIRE = "last_time_fire"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_actions),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_actions_desc)
            }
            nm.createNotificationChannel(ch)
        }
    }

    fun onBattery(context: Context, percent: Int, charging: Boolean) {
        val store = RuleStore(context)
        val rules = store.load().filter { it.enabled }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastBucket = prefs.getInt(KEY_LAST_BATTERY_BUCKET, -1)
        // 5% buckets to avoid spam when percent jitters
        val bucket = percent / 5
        if (bucket == lastBucket) {
            // still allow charger-edge rules below
        } else {
            prefs.edit().putInt(KEY_LAST_BATTERY_BUCKET, bucket).apply()
            for (rule in rules) {
                when (rule.trigger) {
                    TriggerType.BATTERY_BELOW ->
                        if (percent <= rule.triggerValue) run(context, rule)
                    TriggerType.BATTERY_ABOVE ->
                        if (percent >= rule.triggerValue) run(context, rule)
                    else -> Unit
                }
            }
        }
    }

    fun onPowerConnected(context: Context, connected: Boolean) {
        val type = if (connected) TriggerType.CHARGER_CONNECTED else TriggerType.CHARGER_DISCONNECTED
        fire(context, type)
    }

    fun onHeadphones(context: Context, connected: Boolean) {
        val type = if (connected) TriggerType.HEADPHONES_CONNECTED else TriggerType.HEADPHONES_DISCONNECTED
        fire(context, type)
    }

    fun onBoot(context: Context) {
        fire(context, TriggerType.BOOT_COMPLETED)
    }

    fun onTimeTick(context: Context, hour: Int, minute: Int) {
        val store = RuleStore(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "%02d%02d".format(hour, minute)
        val last = prefs.getString(KEY_LAST_TIME_FIRE, "")
        if (last == key) return
        val matches = store.load().filter {
            it.enabled &&
                it.trigger == TriggerType.TIME_DAILY &&
                it.triggerValue == hour &&
                it.triggerValue2 == minute
        }
        if (matches.isEmpty()) return
        prefs.edit().putString(KEY_LAST_TIME_FIRE, key).apply()
        matches.forEach { run(context, it) }
    }

    private fun fire(context: Context, type: TriggerType) {
        RuleStore(context).load()
            .filter { it.enabled && it.trigger == type }
            .forEach { run(context, it) }
    }

    fun run(context: Context, rule: Rule) {
        ensureChannel(context)
        for (action in rule.actions) {
            when (action.type) {
                ActionType.TOAST -> {
                    Toast.makeText(context, action.text.ifBlank { rule.name }, Toast.LENGTH_SHORT).show()
                }
                ActionType.NOTIFY -> {
                    val title = action.title.ifBlank { rule.name }
                    val body = action.text.ifBlank { rule.triggerSummary() }
                    val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()
                    try {
                        NotificationManagerCompat.from(context)
                            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
                    } catch (_: SecurityException) {
                        // POST_NOTIFICATIONS not granted
                    }
                }
                ActionType.RINGER_MODE -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val mode = when (action.ringerMode) {
                        RingerModeKind.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                        RingerModeKind.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                        RingerModeKind.SILENT -> AudioManager.RINGER_MODE_SILENT
                    }
                    try {
                        am.ringerMode = mode
                    } catch (_: SecurityException) {
                        // DND access may be required on some OEMs
                    }
                }
            }
        }
    }
}
