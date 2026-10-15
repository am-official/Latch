package com.endroid.latch.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TriggerType {
    BATTERY_BELOW, BATTERY_ABOVE,
    CHARGER_CONNECTED, CHARGER_DISCONNECTED,
    HEADPHONES_CONNECTED, HEADPHONES_DISCONNECTED,
    BOOT_COMPLETED, TIME_DAILY,
    WIFI_CONNECTED, WIFI_DISCONNECTED, WIFI_SSID,
    BLUETOOTH_CONNECTED, BLUETOOTH_DISCONNECTED,
    APP_OPENED, SCREEN_ON, SCREEN_OFF,
    CUSTOM_BROADCAST,
    AIRPLANE_ON, AIRPLANE_OFF,
    MEDIA_MOUNTED,
    TIME_WINDOW,          // active between HH:MM and HH:MM (checked on events)
    DAY_OF_WEEK,          // triggerValue bitmask Sun=1 Mon=2 ...
}

enum class ActionType {
    NOTIFY, RINGER_MODE, TOAST,
    OPEN_APP, OPEN_URL, SET_BRIGHTNESS,
    SHELL, WAIT_MS, PLUGIN_BROADCAST,
    MEDIA_VOLUME,         // 0-100
    VIBRATE,              // duration ms in param
    CLIPBOARD,            // text
    FLASHLIGHT,           // on/off in param
    WIFI_ENABLE,          // param true/false (best-effort)
    BLUETOOTH_ENABLE,
    SKIP_TO_NEXT,         // media next
    FORCE_STOP,           // elevated am force-stop
    MOBILE_DATA,          // svc data
    AIRPLANE_MODE,        // settings + broadcast
}

enum class RingerModeKind { NORMAL, VIBRATE, SILENT }

/** Extra condition that must also hold (AND). */
enum class ConditionType {
    NONE,
    WIFI_ON,
    WIFI_OFF,
    CHARGING,
    NOT_CHARGING,
    SCREEN_ON,
    SCREEN_OFF,
    BATTERY_BELOW,
    BATTERY_ABOVE,
    DAY_WEEKDAY,
    DAY_WEEKEND,
}

data class RuleAction(
    val type: ActionType,
    val text: String = "",
    val title: String = "",
    val ringerMode: RingerModeKind = RingerModeKind.NORMAL,
    val param: String = "",
) {
    fun summary(): String = when (type) {
        ActionType.NOTIFY -> "Notify: ${title.ifBlank { text }.ifBlank { "…" }}"
        ActionType.RINGER_MODE -> "Ringer → ${ringerMode.name.lowercase()}"
        ActionType.TOAST -> "Toast: ${text.ifBlank { "…" }}"
        ActionType.OPEN_APP -> "Open: ${param.ifBlank { "?" }}"
        ActionType.OPEN_URL -> "URL"
        ActionType.SET_BRIGHTNESS -> "Brightness ${param}%"
        ActionType.SHELL -> "Shell: ${param.take(28)}"
        ActionType.WAIT_MS -> "Wait ${param}ms"
        ActionType.PLUGIN_BROADCAST -> "Plugin out"
        ActionType.MEDIA_VOLUME -> "Media vol ${param}%"
        ActionType.VIBRATE -> "Vibrate ${param}ms"
        ActionType.CLIPBOARD -> "Clipboard"
        ActionType.FLASHLIGHT -> "Flash ${param.ifBlank { "on" }}"
        ActionType.WIFI_ENABLE -> "Wi‑Fi ${param.ifBlank { "toggle" }}"
        ActionType.BLUETOOTH_ENABLE -> "BT ${param.ifBlank { "toggle" }}"
        ActionType.SKIP_TO_NEXT -> "Media next"
        ActionType.FORCE_STOP -> "Force-stop ${param.ifBlank { "?" }}"
        ActionType.MOBILE_DATA -> "Data ${param.ifBlank { "toggle" }}"
        ActionType.AIRPLANE_MODE -> "Airplane ${param.ifBlank { "on" }}"
    }

    fun toJson() = JSONObject()
        .put("type", type.name).put("text", text).put("title", title)
        .put("ringerMode", ringerMode.name).put("param", param)

    companion object {
        fun fromJson(o: JSONObject) = RuleAction(
            type = ActionType.valueOf(o.getString("type")),
            text = o.optString("text"), title = o.optString("title"),
            ringerMode = runCatching { RingerModeKind.valueOf(o.optString("ringerMode", "NORMAL")) }
                .getOrDefault(RingerModeKind.NORMAL),
            param = o.optString("param"),
        )
    }
}

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val trigger: TriggerType,
    val triggerValue: Int = 20,
    val triggerValue2: Int = 0,
    val triggerParam: String = "",
    val condition: ConditionType = ConditionType.NONE,
    val conditionValue: Int = 0,
    val actions: List<RuleAction> = emptyList(),
    val cooldownSec: Int = 0,
    val notes: String = "",
    val tag: String = "",
) {
    fun triggerSummary(): String {
        val base = when (trigger) {
            TriggerType.BATTERY_BELOW -> "Battery ≤ $triggerValue%"
            TriggerType.BATTERY_ABOVE -> "Battery ≥ $triggerValue%"
            TriggerType.CHARGER_CONNECTED -> "Charger in"
            TriggerType.CHARGER_DISCONNECTED -> "Charger out"
            TriggerType.HEADPHONES_CONNECTED -> "Headphones in"
            TriggerType.HEADPHONES_DISCONNECTED -> "Headphones out"
            TriggerType.BOOT_COMPLETED -> "Boot"
            TriggerType.TIME_DAILY -> "%02d:%02d daily".format(triggerValue, triggerValue2)
            TriggerType.WIFI_CONNECTED -> "Wi‑Fi on"
            TriggerType.WIFI_DISCONNECTED -> "Wi‑Fi off"
            TriggerType.WIFI_SSID -> "SSID ${triggerParam.ifBlank { "?" }}"
            TriggerType.BLUETOOTH_CONNECTED -> "BT connected"
            TriggerType.BLUETOOTH_DISCONNECTED -> "BT disconnected"
            TriggerType.APP_OPENED -> "App ${triggerParam.ifBlank { "?" }}"
            TriggerType.SCREEN_ON -> "Screen on"
            TriggerType.SCREEN_OFF -> "Screen off"
            TriggerType.CUSTOM_BROADCAST -> "Broadcast ${triggerParam.ifBlank { "?" }}"
            TriggerType.AIRPLANE_ON -> "Airplane on"
            TriggerType.AIRPLANE_OFF -> "Airplane off"
            TriggerType.MEDIA_MOUNTED -> "Media mounted"
            TriggerType.TIME_WINDOW -> "Window %02d:%02d–%02d:%02d".format(
                triggerValue, triggerValue2,
                triggerParam.substringBefore(":").toIntOrNull() ?: 0,
                triggerParam.substringAfter(":").toIntOrNull() ?: 0,
            )
            TriggerType.DAY_OF_WEEK -> "Day mask $triggerValue"
        }
        return if (condition == ConditionType.NONE) base else "$base + ${condition.name.lowercase()}"
    }

    fun toJson() = JSONObject()
        .put("id", id).put("name", name).put("enabled", enabled)
        .put("trigger", trigger.name)
        .put("triggerValue", triggerValue).put("triggerValue2", triggerValue2)
        .put("triggerParam", triggerParam)
        .put("condition", condition.name).put("conditionValue", conditionValue)
        .put("cooldownSec", cooldownSec).put("notes", notes).put("tag", tag)
        .put("actions", JSONArray().also { a -> actions.forEach { a.put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): Rule {
            val acts = mutableListOf<RuleAction>()
            o.optJSONArray("actions")?.let { arr ->
                for (i in 0 until arr.length()) acts += RuleAction.fromJson(arr.getJSONObject(i))
            }
            return Rule(
                id = o.getString("id"), name = o.getString("name"),
                enabled = o.optBoolean("enabled", true),
                trigger = TriggerType.valueOf(o.getString("trigger")),
                triggerValue = o.optInt("triggerValue", 20),
                triggerValue2 = o.optInt("triggerValue2", 0),
                triggerParam = o.optString("triggerParam"),
                condition = runCatching { ConditionType.valueOf(o.optString("condition", "NONE")) }
                    .getOrDefault(ConditionType.NONE),
                conditionValue = o.optInt("conditionValue", 0),
                actions = acts,
                cooldownSec = o.optInt("cooldownSec", 0),
                notes = o.optString("notes"), tag = o.optString("tag"),
            )
        }
    }
}
