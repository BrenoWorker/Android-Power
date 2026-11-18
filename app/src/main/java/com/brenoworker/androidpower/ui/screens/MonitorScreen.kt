package com.brenoworker.androidpower.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.brenoworker.androidpower.data.HistorySample
import com.brenoworker.androidpower.data.HistoryStats
import com.brenoworker.androidpower.data.MonitorSettings
import com.brenoworker.androidpower.ui.Format
import com.brenoworker.androidpower.ui.LineChart
import com.brenoworker.androidpower.ui.PowerIntents
import com.brenoworker.androidpower.ui.SectionCard
import com.brenoworker.androidpower.ui.StatusRow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MonitorScreen(
    settings: MonitorSettings,
    history: List<HistorySample>,
    onMonitorChange: (Boolean) -> Unit,
    onSettingsChange: ((MonitorSettings) -> MonitorSettings) -> Unit,
    onRefreshHistory: () -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onMonitorChange(true)
    }

    LaunchedEffect(Unit) {
        while (true) {
            onRefreshHistory()
            delay(30_000)
        }
    }

    fun toggleMonitor(enabled: Boolean) {
        val needsPermission = enabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onMonitorChange(enabled)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Background monitor") {
            ToggleRow(
                title = "Run power monitor",
                subtitle = "Keeps a live status notification, records battery history and sends alerts.",
                checked = settings.monitorEnabled,
                onCheckedChange = ::toggleMonitor
            )
            TextButton(onClick = { PowerIntents.open(context, PowerIntents.notifications(context)) }) {
                Text("Notification settings")
            }
        }

        SectionCard(title = "Alerts") {
            ToggleRow(
                title = "Charge limit",
                subtitle = "Notify when charging reaches ${settings.chargeLimit}%",
                checked = settings.chargeLimitEnabled,
                onCheckedChange = { value -> onSettingsChange { it.copy(chargeLimitEnabled = value) } }
            )
            Slider(
                value = settings.chargeLimit.toFloat(),
                onValueChange = { value -> onSettingsChange { it.copy(chargeLimit = value.roundToInt()) } },
                valueRange = 50f..100f,
                steps = 9,
                enabled = settings.chargeLimitEnabled
            )
            ToggleRow(
                title = "Low battery",
                subtitle = "Notify when the battery drops to ${settings.lowBatteryLevel}%",
                checked = settings.lowBatteryEnabled,
                onCheckedChange = { value -> onSettingsChange { it.copy(lowBatteryEnabled = value) } }
            )
            Slider(
                value = settings.lowBatteryLevel.toFloat(),
                onValueChange = { value -> onSettingsChange { it.copy(lowBatteryLevel = value.roundToInt()) } },
                valueRange = 5f..50f,
                steps = 8,
                enabled = settings.lowBatteryEnabled
            )
            ToggleRow(
                title = "High temperature",
                subtitle = "Notify when the battery reaches ${settings.temperatureLimit} °C",
                checked = settings.temperatureEnabled,
                onCheckedChange = { value -> onSettingsChange { it.copy(temperatureEnabled = value) } }
            )
            Slider(
                value = settings.temperatureLimit.toFloat(),
                onValueChange = { value -> onSettingsChange { it.copy(temperatureLimit = value.roundToInt()) } },
                valueRange = 35f..55f,
                steps = 19,
                enabled = settings.temperatureEnabled
            )
            if (!settings.monitorEnabled) {
                Text(
                    text = "Alerts only work while the background monitor is running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        SectionCard(title = "Battery history") {
            if (history.size < 2) {
                Text(
                    text = "No history yet. Turn on the monitor and history will be recorded every level change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LineChart(
                    values = history.map { it.level.toFloat() },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    minValue = 0f,
                    maxValue = 100f
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Format.clock(history.first().timestamp), style = MaterialTheme.typography.labelSmall)
                    Text(Format.clock(history.last().timestamp), style = MaterialTheme.typography.labelSmall)
                }
                val drain = HistoryStats.drainPercentPerHour(history)
                val charge = HistoryStats.chargePercentPerHour(history)
                val latest = history.last()
                StatusRow("Recorded span", Format.duration(latest.timestamp - history.first().timestamp))
                StatusRow("Samples", history.size.toString())
                StatusRow("Max temperature", Format.temperature(history.maxOf { it.temperatureC }))
                drain?.let {
                    StatusRow("Drain rate", Format.rate(it))
                    if (!latest.plugged) {
                        StatusRow("Estimated time left", Format.duration((latest.level / it * 3_600_000).toLong()))
                    }
                }
                charge?.let { StatusRow("Charge rate", Format.rate(it)) }
            }
            TextButton(onClick = onClearHistory, enabled = history.isNotEmpty()) {
                Text("Clear history")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
