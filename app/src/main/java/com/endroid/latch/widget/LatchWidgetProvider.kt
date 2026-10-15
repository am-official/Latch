package com.endroid.latch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.endroid.latch.MainActivity
import com.endroid.latch.R
import com.endroid.latch.data.RuleStore
import com.endroid.latch.receiver.WidgetToggleReceiver

class LatchWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, LatchWidgetProvider::class.java))
            for (id in ids) update(context, mgr, id)
        }

        private fun update(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences("latch_widget", Context.MODE_PRIVATE)
            val ruleId = prefs.getString("w_$widgetId", null)
            val rule = ruleId?.let { id -> RuleStore(context).load().find { it.id == id } }
            val views = RemoteViews(context.packageName, R.layout.widget_rule)
            if (rule == null) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_pick))
                views.setTextViewText(R.id.widget_status, "—")
                val open = PendingIntent.getActivity(
                    context, widgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, open)
            } else {
                views.setTextViewText(R.id.widget_title, rule.name)
                views.setTextViewText(
                    R.id.widget_status,
                    if (rule.enabled) context.getString(R.string.widget_on)
                    else context.getString(R.string.widget_off),
                )
                val toggle = Intent(context, WidgetToggleReceiver::class.java).apply {
                    action = WidgetToggleReceiver.ACTION_TOGGLE
                    putExtra("rule_id", rule.id)
                }
                val pi = PendingIntent.getBroadcast(
                    context, widgetId, toggle,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pi)
            }
            mgr.updateAppWidget(widgetId, views)
        }

        fun bindRule(context: Context, widgetId: Int, ruleId: String) {
            context.getSharedPreferences("latch_widget", Context.MODE_PRIVATE)
                .edit().putString("w_$widgetId", ruleId).apply()
            val mgr = AppWidgetManager.getInstance(context)
            update(context, mgr, widgetId)
        }
    }
}
