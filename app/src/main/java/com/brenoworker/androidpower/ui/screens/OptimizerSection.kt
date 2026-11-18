package com.brenoworker.androidpower.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brenoworker.androidpower.data.CheckId
import com.brenoworker.androidpower.data.MemoryStatus
import com.brenoworker.androidpower.data.OptimizeResult
import com.brenoworker.androidpower.data.StorageStatus
import com.brenoworker.androidpower.data.SystemCheck
import com.brenoworker.androidpower.ui.Format
import com.brenoworker.androidpower.ui.PowerIntents
import com.brenoworker.androidpower.ui.SectionCard
import java.util.Locale

data class OptimizerUiState(
    val running: Boolean = false,
    val memory: MemoryStatus? = null,
    val storage: StorageStatus? = null,
    val result: OptimizeResult? = null,
    val checks: List<SystemCheck> = emptyList()
)

@Composable
fun OptimizerSection(state: OptimizerUiState, onOptimize: () -> Unit, onRefresh: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { onRefresh() }

    SectionCard(title = "System optimizer") {
        state.memory?.let { memory ->
            UsageBar(
                label = "Memory",
                value = "${memory.availableMb} MB free of ${memory.totalMb} MB",
                fraction = memory.usedFraction
            )
        }
        state.storage?.let { storage ->
            UsageBar(
                label = "Storage",
                value = String.format(Locale.US, "%.1f GB free of %.1f GB", storage.freeGb, storage.totalGb),
                fraction = storage.usedFraction
            )
        }
        Button(
            onClick = onOptimize,
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.running) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Optimizing…")
            } else {
                Text("Optimize system")
            }
        }
        state.result?.let { result ->
            Text(
                text = "Last run at ${Format.clock(result.timestamp)}: closed background processes of ${result.appsCleaned} apps, " +
                    "freed ${result.freedMb} MB of RAM and ${result.cacheFreedKb} KB of cache.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.checks.isNotEmpty()) {
        val issues = state.checks.count { !it.ok }
        SectionCard(title = if (issues == 0) "System check · all good" else "System check · $issues to improve") {
            state.checks.forEachIndexed { index, check ->
                if (index > 0) HorizontalDivider()
                CheckRow(
                    check = check,
                    onFix = fixFor(check.id)?.let { intent -> { PowerIntents.open(context, intent) } }
                )
            }
            TextButton(onClick = onRefresh) { Text("Check again") }
        }
    }
}

@Composable
private fun UsageBar(label: String, value: String, fraction: Float) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (fraction > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CheckRow(check: SystemCheck, onFix: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (check.ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (check.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(check.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                check.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!check.ok && onFix != null) {
            TextButton(onClick = onFix) { Text("Fix") }
        }
    }
}

private fun fixFor(id: CheckId): Intent? = when (id) {
    CheckId.BatterySaver -> PowerIntents.batterySaver()
    CheckId.Brightness, CheckId.ScreenTimeout -> PowerIntents.display()
    CheckId.Storage -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
    CheckId.Unrestricted -> PowerIntents.optimizationList()
    CheckId.Memory, CheckId.Temperature -> null
}
