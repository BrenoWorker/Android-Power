package com.brenoworker.androidpower.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.math.abs

object BatteryReader {

    fun stickyIntent(context: Context): Intent? = ContextCompat.registerReceiver(
        context,
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED
    )

    fun read(context: Context, intent: Intent?): BatterySnapshot {
        val manager = context.getSystemService(BatteryManager::class.java)
        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (rawLevel >= 0 && scale > 0) {
            rawLevel * 100 / scale
        } else {
            manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        }
        val status = mapStatus(intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1)
        val plug = mapPlug(intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0)
        val health = mapHealth(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1)
        val temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = normalizeVoltage(intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0)
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty().ifBlank { "Unknown" }
        val current = normalizeCurrent(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
        val average = normalizeCurrent(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
        val counter = normalizeCounter(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
        val capacity = if (counter != null && level >= 5) counter * 100 / level else null
        val remaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.computeChargeTimeRemaining().takeIf { it > 0 }
        } else {
            null
        }
        return BatterySnapshot(
            level = level,
            status = status,
            plug = plug,
            health = health,
            temperatureC = temperature,
            voltageMv = voltage,
            currentMa = signCurrent(current ?: 0, status, plug),
            averageCurrentMa = average?.let { signCurrent(it, status, plug) },
            chargeCounterMah = counter,
            estimatedCapacityMah = capacity,
            technology = technology,
            chargeTimeRemainingMs = remaining,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun normalizeVoltage(raw: Int): Int = if (raw in 1..100) raw * 1000 else raw

    private fun normalizeCurrent(raw: Int): Int? = when {
        raw == Int.MIN_VALUE -> null
        abs(raw) >= 10_000 -> raw / 1000
        else -> raw
    }

    private fun normalizeCounter(raw: Int): Int? = when {
        raw <= 0 -> null
        raw >= 100_000 -> raw / 1000
        else -> raw
    }

    private fun signCurrent(value: Int, status: ChargeStatus, plug: PlugType): Int = when {
        status == ChargeStatus.Charging -> abs(value)
        status == ChargeStatus.Discharging -> -abs(value)
        plug == PlugType.None -> -abs(value)
        else -> value
    }

    private fun mapStatus(value: Int): ChargeStatus = when (value) {
        BatteryManager.BATTERY_STATUS_CHARGING -> ChargeStatus.Charging
        BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeStatus.Discharging
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeStatus.NotCharging
        BatteryManager.BATTERY_STATUS_FULL -> ChargeStatus.Full
        else -> ChargeStatus.Unknown
    }

    private fun mapPlug(value: Int): PlugType = when (value) {
        BatteryManager.BATTERY_PLUGGED_AC -> PlugType.Ac
        BatteryManager.BATTERY_PLUGGED_USB -> PlugType.Usb
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.Wireless
        8 -> PlugType.Dock
        else -> PlugType.None
    }

    private fun mapHealth(value: Int): BatteryHealth = when (value) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.Good
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.Overheat
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.Dead
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OverVoltage
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.Cold
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.Failure
        else -> BatteryHealth.Unknown
    }
}
