package com.focusgain.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimerMode {
    POMODORO,
    COUNTDOWN,
    STOPWATCH
}

/**
 * Central editable settings & daily focus-time store.
 * Every list, timer preset, strict-mode rule, and emergency phrase is editable from the UI
 * or via JSON export/import, making it easy to customize or extend.
 */
class FocusPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focusgain_prefs", Context.MODE_PRIVATE)

    private fun todayKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "study_seconds_${sdf.format(Date())}"
    }

    var isProtectionEnabled: Boolean
        get() = prefs.getBoolean("protection_enabled", true)
        set(value) = prefs.edit().putBoolean("protection_enabled", value).apply()

    var isYouTubeStudyModeEnabled: Boolean
        get() = prefs.getBoolean("yt_study_mode", true)
        set(value) = prefs.edit().putBoolean("yt_study_mode", value).apply()

    var isBlockShortsEnabled: Boolean
        get() = prefs.getBoolean("block_shorts", true)
        set(value) = prefs.edit().putBoolean("block_shorts", value).apply()

    var isStrictModeEnabled: Boolean
        get() = prefs.getBoolean("strict_mode", false)
        set(value) = prefs.edit().putBoolean("strict_mode", value).apply()

    var strictLockUntilMillis: Long
        get() = prefs.getLong("strict_lock_until", 0L)
        set(value) = prefs.edit().putLong("strict_lock_until", value).apply()

    fun isStrictLockActive(): Boolean {
        return isStrictModeEnabled && System.currentTimeMillis() < strictLockUntilMillis
    }

    var emergencyPledgePhrase: String
        get() = prefs.getString("emergency_phrase", "I have a real emergency and will return to study")!!
        set(value) = prefs.edit().putString("emergency_phrase", value).apply()

    var emergencyCooldownSeconds: Int
        get() = prefs.getInt("emergency_cooldown_sec", 30)
        set(value) = prefs.edit().putInt("emergency_cooldown_sec", value).apply()

    var pomodoroFocusMinutes: Int
        get() = prefs.getInt("pomo_focus_min", 25)
        set(value) = prefs.edit().putInt("pomo_focus_min", value).apply()

    var pomodoroBreakMinutes: Int
        get() = prefs.getInt("pomo_break_min", 5)
        set(value) = prefs.edit().putInt("pomo_break_min", value).apply()

    var countdownMinutes: Int
        get() = prefs.getInt("countdown_min", 45)
        set(value) = prefs.edit().putInt("countdown_min", value).apply()

    var selectedTimerMode: TimerMode
        get() = TimerMode.valueOf(prefs.getString("timer_mode", TimerMode.POMODORO.name)!!)
        set(value) = prefs.edit().putString("timer_mode", value.name).apply()

    var todayStudySeconds: Long
        get() = prefs.getLong(todayKey(), 0L)
        set(value) = prefs.edit().putLong(todayKey(), value).apply()

    fun addStudySeconds(seconds: Long) {
        todayStudySeconds = todayStudySeconds + seconds
    }

    /** Package names of blocked Android apps (e.g., com.instagram.android) */
    var blockedAppPackages: Set<String>
        get() = prefs.getStringSet("blocked_packages", emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet("blocked_packages", value).apply()

    /** Allowed YouTube channel names or @handles (case-insensitive) */
    var allowedYouTubeChannels: List<String>
        get() {
            val raw = prefs.getString("allowed_yt_channels", "[]") ?: "[]"
            val arr = JSONArray(raw)
            return List(arr.length()) { i -> arr.getString(i) }
        }
        set(value) {
            val arr = JSONArray()
            value.distinctBy { it.trim().lowercase() }.forEach { arr.put(it.trim()) }
            prefs.edit().putString("allowed_yt_channels", arr.toString()).apply()
        }

    fun exportAllAsJson(): String {
        val obj = JSONObject()
        obj.put("protection_enabled", isProtectionEnabled)
        obj.put("yt_study_mode", isYouTubeStudyModeEnabled)
        obj.put("block_shorts", isBlockShortsEnabled)
        obj.put("strict_mode", isStrictModeEnabled)
        obj.put("emergency_phrase", emergencyPledgePhrase)
        obj.put("emergency_cooldown_sec", emergencyCooldownSeconds)
        obj.put("pomo_focus_min", pomodoroFocusMinutes)
        obj.put("pomo_break_min", pomodoroBreakMinutes)
        obj.put("countdown_min", countdownMinutes)
        obj.put("blocked_packages", JSONArray(blockedAppPackages.toList()))
        obj.put("allowed_yt_channels", JSONArray(allowedYouTubeChannels))
        return obj.toString(2)
    }
}
