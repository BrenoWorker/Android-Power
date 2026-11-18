package com.brenoworker.androidpower.data

import android.content.Context
import java.io.File

data class HistorySample(
    val timestamp: Long,
    val level: Int,
    val currentMa: Int,
    val temperatureC: Float,
    val plugged: Boolean
)

object HistoryStore {

    private const val FILE_NAME = "power_history.csv"
    private const val MAX_SAMPLES = 720

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun load(context: Context): List<HistorySample> {
        val target = file(context)
        if (!target.exists()) return emptyList()
        return target.readLines().mapNotNull(::parse)
    }

    @Synchronized
    fun append(context: Context, sample: HistorySample) {
        val samples = (load(context) + sample).takeLast(MAX_SAMPLES)
        file(context).writeText(samples.joinToString("\n", transform = ::format))
    }

    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
    }

    fun fromSnapshot(snapshot: BatterySnapshot) = HistorySample(
        timestamp = snapshot.timestamp,
        level = snapshot.level,
        currentMa = snapshot.currentMa,
        temperatureC = snapshot.temperatureC,
        plugged = snapshot.isPluggedIn
    )

    private fun format(sample: HistorySample): String =
        "${sample.timestamp},${sample.level},${sample.currentMa},${sample.temperatureC},${if (sample.plugged) 1 else 0}"

    private fun parse(line: String): HistorySample? {
        val parts = line.split(',')
        if (parts.size != 5) return null
        return HistorySample(
            timestamp = parts[0].toLongOrNull() ?: return null,
            level = parts[1].toIntOrNull() ?: return null,
            currentMa = parts[2].toIntOrNull() ?: return null,
            temperatureC = parts[3].toFloatOrNull() ?: return null,
            plugged = parts[4] == "1"
        )
    }
}

object HistoryStats {

    fun drainPercentPerHour(samples: List<HistorySample>): Float? {
        val run = samples.takeLastWhile { !it.plugged }
        if (run.size < 2) return null
        val hours = (run.last().timestamp - run.first().timestamp) / 3_600_000f
        if (hours < 0.25f) return null
        val drop = run.first().level - run.last().level
        return if (drop > 0) drop / hours else null
    }

    fun chargePercentPerHour(samples: List<HistorySample>): Float? {
        val run = samples.takeLastWhile { it.plugged }
        if (run.size < 2) return null
        val hours = (run.last().timestamp - run.first().timestamp) / 3_600_000f
        if (hours < 0.1f) return null
        val gain = run.last().level - run.first().level
        return if (gain > 0) gain / hours else null
    }
}
