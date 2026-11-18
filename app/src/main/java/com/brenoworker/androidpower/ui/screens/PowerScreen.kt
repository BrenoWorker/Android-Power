package com.brenoworker.androidpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brenoworker.androidpower.data.BatterySnapshot
import com.brenoworker.androidpower.data.PowerState
import com.brenoworker.androidpower.ui.Format
import com.brenoworker.androidpower.ui.PowerIntents
import com.brenoworker.androidpower.ui.SectionCard
import com.brenoworker.androidpower.ui.StatusRow

@Composable
fun PowerScreen(
    power: PowerState,
    battery: BatterySnapshot,
    optimizer: OptimizerUiState,
    onOptimize: () -> Unit,
    onRefreshSystem: () -> Unit
) {
    val context = LocalContext.current

    val hints = buildList {
        if (battery.temperatureC >= 40f) {
            add("Battery is hot (${Format.temperature(battery.temperatureC)}). Avoid gaming and fast charging until it cools down.")
        }
        if (power.isThrottling) {
            add("The system is thermally throttling (${power.thermal.label}). Performance is being reduced to protect the device.")
        }
        if (!battery.isPluggedIn && battery.level <= 20 && !power.powerSaveMode) {
            add("Battery is low. Turning on Battery Saver will extend the remaining runtime.")
        }
        if (battery.isPluggedIn && battery.level >= 80) {
            add("Battery is above 80% while charging. Unplugging around 80% reduces long-term wear.")
        }
        if (!battery.isPluggedIn && battery.currentMa <= -800) {
            add("High drain right now (${Format.current(battery.currentMa)}). Check the Apps tab for apps running unrestricted.")
        }
        if (isEmpty()) add("Everything looks healthy.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OptimizerSection(state = optimizer, onOptimize = onOptimize, onRefresh = onRefreshSystem)

        SectionCard(title = "Power state") {
            StatusRow("Battery Saver", if (power.powerSaveMode) "On" else "Off", power.powerSaveMode)
            StatusRow("Doze (device idle)", if (power.deviceIdle) "Active" else "Inactive")
            StatusRow("Screen", if (power.interactive) "Interactive" else "Off")
            StatusRow("Thermal status", power.thermal.label, if (power.isThrottling) false else null)
            StatusRow("Location saver", power.locationMode)
            StatusRow("Sustained performance", if (power.sustainedPerformance) "Supported" else "Not supported")
            StatusRow("Android Power", if (power.ignoringOptimizations) "Unrestricted" else "Optimized", power.ignoringOptimizations)
        }

        SectionCard(title = "Recommendations") {
            hints.forEach { hint ->
                Text(text = "• $hint", style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard(title = "Quick actions") {
            Button(
                onClick = { PowerIntents.open(context, PowerIntents.batterySaver(), PowerIntents.batteryUsage()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (power.powerSaveMode) "Battery Saver settings" else "Turn on Battery Saver")
            }
            FilledTonalButton(
                onClick = { PowerIntents.open(context, PowerIntents.batteryUsage()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Battery usage")
            }
            FilledTonalButton(
                onClick = { PowerIntents.open(context, PowerIntents.optimizationList()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Battery optimization list")
            }
            FilledTonalButton(
                onClick = { PowerIntents.open(context, PowerIntents.display()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Brightness and screen timeout")
            }
            if (!power.ignoringOptimizations) {
                OutlinedButton(
                    onClick = {
                        PowerIntents.open(
                            context,
                            PowerIntents.requestUnrestricted(context),
                            PowerIntents.optimizationList()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep the monitor alive (unrestrict Android Power)")
                }
            }
        }
    }
}
