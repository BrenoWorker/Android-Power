package com.brenoworker.androidpower.data

import android.content.Context
import android.os.Build
import android.os.PowerManager

enum class ThermalLevel(val label: String) {
    None("Normal"),
    Light("Light"),
    Moderate("Moderate"),
    Severe("Severe"),
    Critical("Critical"),
    Emergency("Emergency"),
    Shutdown("Shutdown"),
    Unsupported("Unavailable")
}

data class PowerState(
    val powerSaveMode: Boolean,
    val deviceIdle: Boolean,
    val interactive: Boolean,
    val ignoringOptimizations: Boolean,
    val thermal: ThermalLevel,
    val sustainedPerformance: Boolean,
    val locationMode: String
) {
    val isThrottling: Boolean
        get() = thermal != ThermalLevel.Unsupported && thermal.ordinal >= ThermalLevel.Moderate.ordinal
}

object PowerStateReader {

    fun read(context: Context): PowerState {
        val manager = context.getSystemService(PowerManager::class.java)
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThermalLevel.entries.getOrElse(manager.currentThermalStatus) { ThermalLevel.Unsupported }
        } else {
            ThermalLevel.Unsupported
        }
        val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            when (manager.locationPowerSaveMode) {
                PowerManager.LOCATION_MODE_NO_CHANGE -> "No change"
                PowerManager.LOCATION_MODE_GPS_DISABLED_WHEN_SCREEN_OFF -> "GPS off when screen off"
                PowerManager.LOCATION_MODE_ALL_DISABLED_WHEN_SCREEN_OFF -> "All off when screen off"
                PowerManager.LOCATION_MODE_FOREGROUND_ONLY -> "Foreground only"
                PowerManager.LOCATION_MODE_THROTTLE_REQUESTS_WHEN_SCREEN_OFF -> "Throttled when screen off"
                else -> "Unknown"
            }
        } else {
            "Unavailable"
        }
        return PowerState(
            powerSaveMode = manager.isPowerSaveMode,
            deviceIdle = manager.isDeviceIdleMode,
            interactive = manager.isInteractive,
            ignoringOptimizations = manager.isIgnoringBatteryOptimizations(context.packageName),
            thermal = thermal,
            sustainedPerformance = manager.isSustainedPerformanceModeSupported,
            locationMode = location
        )
    }
}
