package com.brenoworker.androidpower.data

import android.content.Context

data class MonitorSettings(
    val monitorEnabled: Boolean = false,
    val chargeLimitEnabled: Boolean = true,
    val chargeLimit: Int = 80,
    val lowBatteryEnabled: Boolean = true,
    val lowBatteryLevel: Int = 20,
    val temperatureEnabled: Boolean = true,
    val temperatureLimit: Int = 42
)

class PowerPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("power_prefs", Context.MODE_PRIVATE)

    fun load(): MonitorSettings {
        val defaults = MonitorSettings()
        return MonitorSettings(
            monitorEnabled = prefs.getBoolean(KEY_MONITOR, defaults.monitorEnabled),
            chargeLimitEnabled = prefs.getBoolean(KEY_CHARGE_ENABLED, defaults.chargeLimitEnabled),
            chargeLimit = prefs.getInt(KEY_CHARGE_LIMIT, defaults.chargeLimit),
            lowBatteryEnabled = prefs.getBoolean(KEY_LOW_ENABLED, defaults.lowBatteryEnabled),
            lowBatteryLevel = prefs.getInt(KEY_LOW_LEVEL, defaults.lowBatteryLevel),
            temperatureEnabled = prefs.getBoolean(KEY_TEMP_ENABLED, defaults.temperatureEnabled),
            temperatureLimit = prefs.getInt(KEY_TEMP_LIMIT, defaults.temperatureLimit)
        )
    }

    fun save(settings: MonitorSettings) {
        prefs.edit()
            .putBoolean(KEY_MONITOR, settings.monitorEnabled)
            .putBoolean(KEY_CHARGE_ENABLED, settings.chargeLimitEnabled)
            .putInt(KEY_CHARGE_LIMIT, settings.chargeLimit)
            .putBoolean(KEY_LOW_ENABLED, settings.lowBatteryEnabled)
            .putInt(KEY_LOW_LEVEL, settings.lowBatteryLevel)
            .putBoolean(KEY_TEMP_ENABLED, settings.temperatureEnabled)
            .putInt(KEY_TEMP_LIMIT, settings.temperatureLimit)
            .apply()
    }

    fun setMonitorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITOR, enabled).apply()
    }

    private companion object {
        const val KEY_MONITOR = "monitor_enabled"
        const val KEY_CHARGE_ENABLED = "charge_limit_enabled"
        const val KEY_CHARGE_LIMIT = "charge_limit"
        const val KEY_LOW_ENABLED = "low_battery_enabled"
        const val KEY_LOW_LEVEL = "low_battery_level"
        const val KEY_TEMP_ENABLED = "temperature_enabled"
        const val KEY_TEMP_LIMIT = "temperature_limit"
    }
}
