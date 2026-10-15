package com.endroid.latch.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.endroid.latch.data.RuleStore
import com.endroid.latch.ui.LatchTheme

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }
        setResult(RESULT_CANCELED)
        val rules = RuleStore(this).load()
        setContent {
            LatchTheme {
                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text(
                            "Pick a rule for the widget",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    items(rules) { rule ->
                        ListItem(
                            headlineContent = { Text(rule.name) },
                            supportingContent = { Text(rule.triggerSummary()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LatchWidgetProvider.bindRule(this@WidgetConfigActivity, widgetId, rule.id)
                                    val res = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                    setResult(RESULT_OK, res)
                                    finish()
                                },
                        )
                    }
                }
            }
        }
    }
}
