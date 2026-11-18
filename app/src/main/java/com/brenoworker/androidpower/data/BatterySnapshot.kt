package com.brenoworker.androidpower.data

enum class ChargeStatus(val label: String) {
    Charging("Charging"),
    Discharging("Discharging"),
    NotCharging("Not charging"),
    Full("Full"),
    Unknown("Unknown")
}

enum class PlugType(val label: String) {
    None("Unplugged"),
    Ac("AC charger"),
    Usb("USB"),
    Wireless("Wireless"),
    Dock("Dock")
}

enum class BatteryHealth(val label: String) {
    Good("Good"),
    Overheat("Overheat"),
    Dead("Dead"),
    OverVoltage("Over voltage"),
    Cold("Cold"),
    Failure("Failure"),
    Unknown("Unknown")
}

data class BatterySnapshot(
    val level: Int,
    val status: ChargeStatus,
    val plug: PlugType,
    val health: BatteryHealth,
    val temperatureC: Float,
    val voltageMv: Int,
    val currentMa: Int,
    val averageCurrentMa: Int?,
    val chargeCounterMah: Int?,
    val estimatedCapacityMah: Int?,
    val technology: String,
    val chargeTimeRemainingMs: Long?,
    val timestamp: Long
) {
    val isPluggedIn: Boolean get() = plug != PlugType.None
    val powerW: Float get() = voltageMv / 1000f * currentMa / 1000f
}
