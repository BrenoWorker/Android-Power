package com.brenoworker.androidpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenoworker.androidpower.data.BatterySnapshot
import com.brenoworker.androidpower.ui.Format
import com.brenoworker.androidpower.ui.LevelRing
import com.brenoworker.androidpower.ui.LineChart
import com.brenoworker.androidpower.ui.SectionCard
import com.brenoworker.androidpower.ui.StatCard

private const val LIVE_SAMPLES = 90

@Composable
fun DashboardScreen(battery: BatterySnapshot) {
    val samples = remember { mutableStateListOf<Float>() }
    LaunchedEffect(battery.timestamp) {
        samples.add(battery.currentMa.toFloat())
        while (samples.size > LIVE_SAMPLES) samples.removeAt(0)
    }

    val subtitle = buildString {
        append(battery.plug.label)
        append(" · Health ")
        append(battery.health.label.lowercase())
        battery.chargeTimeRemainingMs?.let { append(" · Full in ${Format.duration(it)}") }
    }

    val stats = listOf(
        "Current" to Format.current(battery.currentMa),
        "Power" to Format.power(battery.powerW),
        "Voltage" to Format.voltage(battery.voltageMv),
        "Temperature" to Format.temperature(battery.temperatureC),
        "Average current" to (battery.averageCurrentMa?.let(Format::current) ?: "Unavailable"),
        "Charge counter" to (battery.chargeCounterMah?.let { "$it mAh" } ?: "Unavailable"),
        "Estimated capacity" to (battery.estimatedCapacityMah?.let { "$it mAh" } ?: "Unavailable"),
        "Technology" to battery.technology
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LevelRing(level = battery.level, caption = battery.status.label)
        }
        Text(
            text = subtitle,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionCard(title = "Live current") {
            Text(
                text = "${Format.current(battery.currentMa)} · ${Format.power(battery.powerW)}",
                style = MaterialTheme.typography.headlineSmall
            )
            LineChart(
                values = samples.toList(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Text(
                text = "Positive values mean the battery is charging, negative values mean it is draining.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        stats.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    StatCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
