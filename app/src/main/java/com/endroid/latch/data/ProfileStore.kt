package com.endroid.latch.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Long = 0xFF006A6AL,
) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("color", colorArgb)
    companion object {
        fun fromJson(o: JSONObject) = Profile(
            id = o.getString("id"),
            name = o.getString("name"),
            colorArgb = o.optLong("color", 0xFF006A6AL),
        )
    }
}

class ProfileStore(context: Context) {
    private val p = context.getSharedPreferences("latch_profiles", Context.MODE_PRIVATE)

    fun load(): List<Profile> {
        val raw = p.getString("profiles", null) ?: return defaultProfiles()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Profile.fromJson(arr.getJSONObject(it)) }
                .ifEmpty { defaultProfiles() }
        } catch (_: Exception) {
            defaultProfiles()
        }
    }

    fun save(list: List<Profile>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        p.edit().putString("profiles", arr.toString()).apply()
    }

    private fun defaultProfiles() = listOf(
        Profile(id = "default", name = "Default"),
        Profile(id = "home", name = "Home", colorArgb = 0xFF2E7D32L),
        Profile(id = "work", name = "Work", colorArgb = 0xFF1565C0L),
        Profile(id = "sleep", name = "Sleep", colorArgb = 0xFF4527A0L),
    )
}
