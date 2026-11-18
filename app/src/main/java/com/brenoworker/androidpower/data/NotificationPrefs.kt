package com.brenoworker.androidpower.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NotificationSettings(
    val detailedStatus: Boolean = true,
    val alertSound: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStart: Int = 23,
    val quietEnd: Int = 7
) {
    fun isQuietNow(): Boolean {
        if (!quietHoursEnabled || quietStart == quietEnd) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (quietStart < quietEnd) hour in quietStart until quietEnd else hour >= quietStart || hour < quietEnd
    }
}

class NotificationPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
    private val stats = context.applicationContext.getSharedPreferences("notification_stats", Context.MODE_PRIVATE)

    fun load(): NotificationSettings {
        val defaults = NotificationSettings()
        return NotificationSettings(
            detailedStatus = prefs.getBoolean(KEY_DETAILED, defaults.detailedStatus),
            alertSound = prefs.getBoolean(KEY_SOUND, defaults.alertSound),
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET, defaults.quietHoursEnabled),
            quietStart = prefs.getInt(KEY_QUIET_START, defaults.quietStart),
            quietEnd = prefs.getInt(KEY_QUIET_END, defaults.quietEnd)
        )
    }

    fun save(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_DETAILED, settings.detailedStatus)
            .putBoolean(KEY_SOUND, settings.alertSound)
            .putBoolean(KEY_QUIET, settings.quietHoursEnabled)
            .putInt(KEY_QUIET_START, settings.quietStart)
            .putInt(KEY_QUIET_END, settings.quietEnd)
            .apply()
    }

    fun mutedPackages(): Set<String> = prefs.getStringSet(KEY_MUTED, emptySet()).orEmpty().toSet()

    fun setMuted(packageName: String, muted: Boolean): Set<String> {
        val updated = mutedPackages().toMutableSet().apply {
            if (muted) add(packageName) else remove(packageName)
        }
        prefs.edit().putStringSet(KEY_MUTED, updated).apply()
        return updated
    }

    @Synchronized
    fun record(packageName: String, blocked: Boolean): Map<String, AppNotificationCount> {
        rollDay()
        val posted = stats.getInt("$PREFIX_POSTED$packageName", 0) + 1
        val editor = stats.edit().putInt("$PREFIX_POSTED$packageName", posted)
        if (blocked) {
            editor.putInt("$PREFIX_BLOCKED$packageName", stats.getInt("$PREFIX_BLOCKED$packageName", 0) + 1)
        }
        editor.commit()
        return todayCounts()
    }

    @Synchronized
    fun todayCounts(): Map<String, AppNotificationCount> {
        rollDay()
        val result = mutableMapOf<String, AppNotificationCount>()
        stats.all.forEach { (key, value) ->
            val count = value as? Int ?: return@forEach
            when {
                key.startsWith(PREFIX_POSTED) -> {
                    val pkg = key.removePrefix(PREFIX_POSTED)
                    result[pkg] = (result[pkg] ?: AppNotificationCount()).copy(posted = count)
                }
                key.startsWith(PREFIX_BLOCKED) -> {
                    val pkg = key.removePrefix(PREFIX_BLOCKED)
                    result[pkg] = (result[pkg] ?: AppNotificationCount()).copy(blocked = count)
                }
            }
        }
        return result
    }

    @Synchronized
    fun resetCounts() {
        stats.edit().clear().putString(KEY_DAY, today()).commit()
    }

    private fun rollDay() {
        val day = today()
        if (stats.getString(KEY_DAY, null) != day) {
            stats.edit().clear().putString(KEY_DAY, day).commit()
        }
    }

    private fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private companion object {
        const val KEY_DETAILED = "detailed_status"
        const val KEY_SOUND = "alert_sound"
        const val KEY_QUIET = "quiet_hours"
        const val KEY_QUIET_START = "quiet_start"
        const val KEY_QUIET_END = "quiet_end"
        const val KEY_MUTED = "muted_packages"
        const val KEY_DAY = "day"
        const val PREFIX_POSTED = "posted:"
        const val PREFIX_BLOCKED = "blocked:"
    }
}

data class AppNotificationCount(val posted: Int = 0, val blocked: Int = 0)
