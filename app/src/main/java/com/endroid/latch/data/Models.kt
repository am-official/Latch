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
    WIFI_CONNECTED,
    WIFI_DISCONNECTED,
    WIFI_SSID,          // connected to SSID matching triggerParam
    BLUETOOTH_CONNECTED,
    BLUETOOTH_DISCONNECTED,
    APP_OPENED,         // usage-stats based poll
    SCREEN_ON,
    SCREEN_OFF,
    CUSTOM_BROADCAST,   // plugin / external intent with action = triggerParam
}

enum class ActionType {
    NOTIFY,
    RINGER_MODE,
    TOAST,
    OPEN_APP,
    OPEN_URL,
    SET_BRIGHTNESS,     // 0-100 needs WRITE_SETTINGS
    SHELL,              // optional elevated via su if enabled
    WAIT_MS,            // delay before next action in chain
    PLUGIN_BROADCAST,   // send broadcast for other apps
}

enum class RingerModeKind { NORMAL, VIBRATE, SILENT }

data class RuleAction(
    val type: ActionType,
    val text: String = "",
    val title: String = "",
    val ringerMode: RingerModeKind = RingerModeKind.NORMAL,
    /** package name, URL, shell cmd, brightness, wait ms, plugin action */
    val param: String = "",
) {
    fun summary(): String = when (type) {
        ActionType.NOTIFY -> "Notify: ${title.ifBlank { text }.ifBlank { "…" }}"
        ActionType.RINGER_MODE -> "Ringer → ${ringerMode.name.lowercase()}"
        ActionType.TOAST -> "Toast: ${text.ifBlank { "…" }}"
        ActionType.OPEN_APP -> "Open app: ${param.ifBlank { "?" }}"
        ActionType.OPEN_URL -> "Open URL"
        ActionType.SET_BRIGHTNESS -> "Brightness ${param.ifBlank { "?" }}%"
        ActionType.SHELL -> "Shell: ${param.take(32)}${if (param.length > 32) "…" else ""}"
        ActionType.WAIT_MS -> "Wait ${param.ifBlank { "0" }} ms"
        ActionType.PLUGIN_BROADCAST -> "Plugin: ${param.ifBlank { text }}"
    }

    fun toJson(): JSONObject = JSONObject()
        .put("type", type.name)
        .put("text", text)
        .put("title", title)
        .put("ringerMode", ringerMode.name)
        .put("param", param)

    companion object {
        fun fromJson(o: JSONObject): RuleAction = RuleAction(
            type = ActionType.valueOf(o.getString("type")),
            text = o.optString("text"),
            title = o.optString("title"),
            ringerMode = runCatching {
                RingerModeKind.valueOf(o.optString("ringerMode", "NORMAL"))
            }.getOrDefault(RingerModeKind.NORMAL),
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
    /** SSID, package name, BT name, custom broadcast action */
    val triggerParam: String = "",
    val actions: List<RuleAction> = emptyList(),
    /** Minimum seconds between runs of this rule (0 = every match) */
    val cooldownSec: Int = 0,
    val notes: String = "",
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
        TriggerType.WIFI_CONNECTED -> "Wi‑Fi connected"
        TriggerType.WIFI_DISCONNECTED -> "Wi‑Fi disconnected"
        TriggerType.WIFI_SSID -> "Wi‑Fi SSID: ${triggerParam.ifBlank { "any" }}"
        TriggerType.BLUETOOTH_CONNECTED -> "Bluetooth connected"
        TriggerType.BLUETOOTH_DISCONNECTED -> "Bluetooth disconnected"
        TriggerType.APP_OPENED -> "App opened: ${triggerParam.ifBlank { "?" }}"
        TriggerType.SCREEN_ON -> "Screen on"
        TriggerType.SCREEN_OFF -> "Screen off"
        TriggerType.CUSTOM_BROADCAST -> "Broadcast: ${triggerParam.ifBlank { "?" }}"
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("enabled", enabled)
        .put("trigger", trigger.name)
        .put("triggerValue", triggerValue)
        .put("triggerValue2", triggerValue2)
        .put("triggerParam", triggerParam)
        .put("cooldownSec", cooldownSec)
        .put("notes", notes)
        .put("actions", JSONArray().also { arr -> actions.forEach { arr.put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): Rule {
            val acts = mutableListOf<RuleAction>()
            o.optJSONArray("actions")?.let { arr ->
                for (i in 0 until arr.length()) acts += RuleAction.fromJson(arr.getJSONObject(i))
            }
            return Rule(
                id = o.getString("id"),
                name = o.getString("name"),
                enabled = o.optBoolean("enabled", true),
                trigger = TriggerType.valueOf(o.getString("trigger")),
                triggerValue = o.optInt("triggerValue", 20),
                triggerValue2 = o.optInt("triggerValue2", 0),
                triggerParam = o.optString("triggerParam"),
                actions = acts,
                cooldownSec = o.optInt("cooldownSec", 0),
                notes = o.optString("notes"),
            )
        }
    }
}
