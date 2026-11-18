package com.brenoworker.androidpower.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {

    fun current(ma: Int): String = if (ma > 0) "+$ma mA" else "$ma mA"

    fun power(watts: Float): String = String.format(Locale.US, "%+.2f W", watts)

    fun voltage(mv: Int): String = String.format(Locale.US, "%.3f V", mv / 1000f)

    fun temperature(celsius: Float): String = String.format(Locale.US, "%.1f °C", celsius)

    fun rate(percentPerHour: Float): String = String.format(Locale.US, "%.1f %%/h", percentPerHour)

    fun duration(ms: Long): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun clock(timestamp: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
