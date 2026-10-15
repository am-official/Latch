package com.endroid.latch.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TriggerType {
    BATTERY_BELOW,
    BATTERY_ABOVE,
    CHARGER_CONNECTED,
    CHARGER_DISCONNECTED,
    HEADPHONES_CONNECTED,
    HEADPHONES_DISCONNECTED,
    BOOT_COMPLETED,
    TIME_DAILY,
}

enum class ActionType {
    NOTIFY,
    RINGER_MODE,
    TOAST,
}

enum class RingerModeKind {
    NORMAL, VIBRATE, SILENT,
}

data class RuleAction(
    val type: ActionType,
    val text: String = "",
    val title: String = "",
    val ringerMode: RingerModeKind = RingerModeKind.NORMAL,
) {
    fun summary(): String = when (type) {
        ActionType.NOTIFY -> "Notify: ${title.ifBlank { text }.ifBlank { "…" }}"
        ActionType.RINGER_MODE -> "Ringer → ${ringerMode.name.lowercase()}"
        ActionType.TOAST -> "Toast: ${text.ifBlank { "…" }}"
    }

    fun toJson(): JSONObject = JSONObject()
        .put("type", type.name)
        .put("text", text)
        .put("title", title)
        .put("ringerMode", ringerMode.name)

    companion object {
        fun fromJson(o: JSONObject): RuleAction = RuleAction(
            type = ActionType.valueOf(o.getString("type")),
            text = o.optString("text"),
            title = o.optString("title"),
            ringerMode = runCatching {
                RingerModeKind.valueOf(o.optString("ringerMode", "NORMAL"))
            }.getOrDefault(RingerModeKind.NORMAL),
        )
    }
}

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val trigger: TriggerType,
    /** Battery percent 1–100 or hour for TIME_DAILY */
    val triggerValue: Int = 20,
    /** Minute for TIME_DAILY */
    val triggerValue2: Int = 0,
    val actions: List<RuleAction> = emptyList(),
) {
    fun triggerSummary(): String = when (trigger) {
        TriggerType.BATTERY_BELOW -> "Battery below $triggerValue%"
        TriggerType.BATTERY_ABOVE -> "Battery above $triggerValue%"
        TriggerType.CHARGER_CONNECTED -> "Charger connected"
        TriggerType.CHARGER_DISCONNECTED -> "Charger disconnected"
        TriggerType.HEADPHONES_CONNECTED -> "Headphones connected"
        TriggerType.HEADPHONES_DISCONNECTED -> "Headphones disconnected"
        TriggerType.BOOT_COMPLETED -> "Device boot"
        TriggerType.TIME_DAILY -> "Daily at %02d:%02d".format(triggerValue, triggerValue2)
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("enabled", enabled)
        .put("trigger", trigger.name)
        .put("triggerValue", triggerValue)
        .put("triggerValue2", triggerValue2)
        .put("actions", JSONArray().also { arr ->
            actions.forEach { arr.put(it.toJson()) }
        })

    companion object {
        fun fromJson(o: JSONObject): Rule {
            val acts = mutableListOf<RuleAction>()
            val arr = o.optJSONArray("actions")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    acts += RuleAction.fromJson(arr.getJSONObject(i))
                }
            }
            return Rule(
                id = o.getString("id"),
                name = o.getString("name"),
                enabled = o.optBoolean("enabled", true),
                trigger = TriggerType.valueOf(o.getString("trigger")),
                triggerValue = o.optInt("triggerValue", 20),
                triggerValue2 = o.optInt("triggerValue2", 0),
                actions = acts,
            )
        }
    }
}
