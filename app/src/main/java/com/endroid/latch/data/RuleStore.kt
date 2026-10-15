package com.endroid.latch.data

import android.content.Context
import org.json.JSONArray
import com.endroid.latch.engine.TimeScheduler
import java.io.File

/** On-device JSON store — no cloud, no account. */
class RuleStore(private val context: Context) {
    private val file: File
        get() = File(context.filesDir, "rules.json")

    @Synchronized
    fun load(): List<Rule> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    add(Rule.fromJson(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(it.toJson()) }
        file.writeText(arr.toString())
        TimeScheduler.rescheduleAll(context)
    }

    fun upsert(rule: Rule) {
        val list = load().toMutableList()
        val idx = list.indexOfFirst { it.id == rule.id }
        if (idx >= 0) list[idx] = rule else list.add(0, rule)
        save(list)
    }

    fun delete(id: String) {
        save(load().filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        save(load().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    /** Export all rules as plain JSON text for sharing / backup. */
    fun exportJson(): String {
        val arr = JSONArray()
        load().forEach { arr.put(it.toJson()) }
        return arr.toString(2)
    }

    fun importJson(text: String): Int {
        val arr = JSONArray(text)
        val incoming = buildList {
            for (i in 0 until arr.length()) {
                add(Rule.fromJson(arr.getJSONObject(i)))
            }
        }
        val merged = load().toMutableList()
        var added = 0
        for (r in incoming) {
            if (merged.none { it.id == r.id }) {
                merged.add(r)
                added++
            }
        }
        save(merged)
        return added
    }
}
