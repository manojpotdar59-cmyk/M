package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DirectiveCategory
import com.example.data.model.JarvisDirective
import org.json.JSONArray
import org.json.JSONObject

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CALLSIGN = "key_callsign"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_VOICE_RATE = "key_voice_rate"
        private const val KEY_DIRECTIVES_JSON = "key_directives_json"
    }

    var callsign: String
        get() = prefs.getString(KEY_CALLSIGN, "Sir") ?: "Sir"
        set(value) = prefs.edit().putString(KEY_CALLSIGN, value).apply()

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 0.95f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    var voiceRate: Float
        get() = prefs.getFloat(KEY_VOICE_RATE, 1.02f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_RATE, value).apply()

    fun getDirectives(): List<JarvisDirective> {
        val jsonStr = prefs.getString(KEY_DIRECTIVES_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            return getDefaultDirectives()
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<JarvisDirective>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    JarvisDirective(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        detail = obj.optString("detail"),
                        category = try {
                            DirectiveCategory.valueOf(obj.optString("category", "TACTICAL"))
                        } catch (e: Exception) {
                            DirectiveCategory.TACTICAL
                        },
                        isCompleted = obj.optBoolean("isCompleted", false),
                        priority = obj.optString("priority", "HIGH"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            getDefaultDirectives()
        }
    }

    fun saveDirectives(directives: List<JarvisDirective>) {
        val array = JSONArray()
        directives.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("detail", d.detail)
                put("category", d.category.name)
                put("isCompleted", d.isCompleted)
                put("priority", d.priority)
                put("createdAt", d.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_DIRECTIVES_JSON, array.toString()).apply()
    }

    private fun getDefaultDirectives(): List<JarvisDirective> {
        return listOf(
            JarvisDirective(
                title = "Mark 85 Armor Diagnostics",
                detail = "Run full system integrity check on repulsion generators & flight surfaces.",
                category = DirectiveCategory.TACTICAL,
                priority = "HIGH"
            ),
            JarvisDirective(
                title = "Stark Tower Power Grid Sync",
                detail = "Calibrate auxiliary clean energy distribution to Manhattan district.",
                category = DirectiveCategory.ROUTINE,
                priority = "MEDIUM"
            ),
            JarvisDirective(
                title = "Vibranium Nanotech Analysis",
                detail = "Simulate molecular bonding thresholds for cryogenic re-entry plating.",
                category = DirectiveCategory.RESEARCH,
                priority = "HIGH"
            ),
            JarvisDirective(
                title = "Perimeter Defense Protocol Theta",
                detail = "Autonomous radar sweeps active along airspace sector 4.",
                category = DirectiveCategory.DEFENSE,
                priority = "CRITICAL"
            )
        )
    }
}
