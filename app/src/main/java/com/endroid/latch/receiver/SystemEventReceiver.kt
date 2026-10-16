package com.endroid.latch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.endroid.latch.data.TriggerType
import com.endroid.latch.engine.RuleEngine

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> RuleEngine.fireType(app, TriggerType.USER_PRESENT)
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
                RuleEngine.fireType(
                    app,
                    if (pm.isPowerSaveMode) TriggerType.POWER_SAVE_ON else TriggerType.POWER_SAVE_OFF,
                )
            }
            Intent.ACTION_DEVICE_STORAGE_LOW -> RuleEngine.fireType(app, TriggerType.STORAGE_LOW)
            Intent.ACTION_DOCK_EVENT -> RuleEngine.fireType(app, TriggerType.DOCK_EVENT)
            Intent.ACTION_SHUTDOWN -> RuleEngine.fireType(app, TriggerType.SHUTDOWN)
            Intent.ACTION_PACKAGE_ADDED -> {
                val pkg = intent.data?.schemeSpecificPart.orEmpty()
                RuleEngine.onPackageEvent(app, TriggerType.PACKAGE_ADDED, pkg)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val pkg = intent.data?.schemeSpecificPart.orEmpty()
                RuleEngine.onPackageEvent(app, TriggerType.PACKAGE_REMOVED, pkg)
            }
        }
    }
}

class WatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        com.endroid.latch.engine.LatchMonitorService.refresh(context.applicationContext)
    }

    companion object {
        const val ACTION = "com.endroid.latch.WATCHDOG"
    }
}
