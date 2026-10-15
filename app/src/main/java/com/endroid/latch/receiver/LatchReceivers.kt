package com.endroid.latch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.endroid.latch.data.RuleStore
import com.endroid.latch.engine.RuleEngine
import com.endroid.latch.engine.TimeScheduler
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            RuleEngine.onBoot(context.applicationContext)
            TimeScheduler.rescheduleAll(context.applicationContext)
        }
    }
}

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> RuleEngine.onPowerConnected(app, true)
            Intent.ACTION_POWER_DISCONNECTED -> RuleEngine.onPowerConnected(app, false)
            Intent.ACTION_BATTERY_CHANGED, Intent.ACTION_BATTERY_LOW, Intent.ACTION_BATTERY_OKAY -> {
                val pct = batteryPercent(context, intent)
                if (pct >= 0) {
                    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    RuleEngine.onBattery(app, pct, charging)
                }
            }
        }
    }

    companion object {
        fun batteryPercent(context: Context, intent: Intent?): Int {
            if (intent != null && intent.hasExtra(BatteryManager.EXTRA_LEVEL)) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) return (level * 100) / scale
            }
            val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
            return if (level >= 0) (level * 100) / scale else -1
        }
    }
}

class HeadsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_HEADSET_PLUG) return
        val state = intent.getIntExtra("state", -1)
        if (state == 1) RuleEngine.onHeadphones(context.applicationContext, true)
        if (state == 0) RuleEngine.onHeadphones(context.applicationContext, false)
    }
}

class TimeTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_TIME_TICK &&
            intent?.action != Intent.ACTION_TIME_CHANGED &&
            intent?.action != Intent.ACTION_TIMEZONE_CHANGED
        ) return
        val cal = Calendar.getInstance()
        RuleEngine.onTimeTick(
            context.applicationContext,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        )
    }
}

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        val app = context.applicationContext
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID)
        val store = RuleStore(app)
        if (!ruleId.isNullOrBlank()) {
            store.load().find { it.id == ruleId && it.enabled }?.let {
                RuleEngine.run(app, it)
            }
        } else {
            val cal = Calendar.getInstance()
            RuleEngine.onTimeTick(
                app,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
            )
        }
        TimeScheduler.rescheduleAll(app)
    }

    companion object {
        const val ACTION = "com.endroid.latch.DAILY_ALARM"
        const val EXTRA_RULE_ID = "rule_id"
    }
}
