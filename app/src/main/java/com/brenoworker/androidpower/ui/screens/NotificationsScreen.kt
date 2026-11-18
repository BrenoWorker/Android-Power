package com.brenoworker.androidpower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenoworker.androidpower.data.ActiveNotification
import com.brenoworker.androidpower.data.AppNotificationCount
import com.brenoworker.androidpower.data.NotificationHub
import com.brenoworker.androidpower.data.NotificationSettings
import com.brenoworker.androidpower.ui.Format
import com.brenoworker.androidpower.ui.PowerIntents
import com.brenoworker.androidpower.ui.SectionCard
import kotlin.math.roundToInt

private const val SNOOZE_MS = 60 * 60 * 1000L
private const val COLLAPSED_LIMIT = 8

@Composable
fun NotificationsScreen(
    settings: NotificationSettings,
    hasAccess: Boolean,
    active: List<ActiveNotification>,
    counts: Map<String, AppNotificationCount>,
    muted: Set<String>,
    onSettingsChange: ((NotificationSettings) -> NotificationSettings) -> Unit,
    onMuteChange: (String, Boolean) -> Unit,
    onResetCounts: () -> Unit
) {
    val context = LocalContext.current
    var showAllActive by rememberSaveable { mutableStateOf(false) }

    val labels = active.associate { it.packageName to it.appLabel }
    val ranking = (counts.keys + muted)
        .distinct()
        .map { pkg -> pkg to (counts[pkg] ?: AppNotificationCount()) }
        .sortedByDescending { it.second.posted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Android Power notifications") {
            SwitchRow(
                title = "Detailed status",
                subtitle = "Show current, power and temperature in the monitor notification",
                checked = settings.detailedStatus,
                onCheckedChange = { value -> onSettingsChange { it.copy(detailedStatus = value) } }
            )
            SwitchRow(
                title = "Alert sound",
                subtitle = "Play sound and vibration for power alerts",
                checked = settings.alertSound,
                onCheckedChange = { value -> onSettingsChange { it.copy(alertSound = value) } }
            )
            SwitchRow(
                title = "Quiet hours",
                subtitle = "Alerts arrive silently from ${hour(settings.quietStart)} to ${hour(settings.quietEnd)}",
                checked = settings.quietHoursEnabled,
                onCheckedChange = { value -> onSettingsChange { it.copy(quietHoursEnabled = value) } }
            )
            Text("Start ${hour(settings.quietStart)}", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = settings.quietStart.toFloat(),
                onValueChange = { value -> onSettingsChange { it.copy(quietStart = value.roundToInt()) } },
                valueRange = 0f..23f,
                steps = 22,
                enabled = settings.quietHoursEnabled
            )
            Text("End ${hour(settings.quietEnd)}", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = settings.quietEnd.toFloat(),
                onValueChange = { value -> onSettingsChange { it.copy(quietEnd = value.roundToInt()) } },
                valueRange = 0f..23f,
                steps = 22,
                enabled = settings.quietHoursEnabled
            )
            TextButton(onClick = { PowerIntents.open(context, PowerIntents.notifications(context)) }) {
                Text("System notification settings")
            }
        }

        if (!hasAccess) {
            SectionCard(title = "App notification control") {
                Text(
                    text = "Allow notification access to see which apps wake your phone the most, clear and snooze notifications, and auto-dismiss notifications from apps you mute.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { PowerIntents.open(context, PowerIntents.notificationAccess()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant notification access")
                }
            }
            return@Column
        }

        SectionCard(title = "Active notifications (${active.size})") {
            if (active.isEmpty()) {
                Text(
                    text = "No notifications right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val visible = if (showAllActive) active else active.take(COLLAPSED_LIMIT)
                visible.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    ActiveNotificationRow(item)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (active.size > COLLAPSED_LIMIT) {
                        TextButton(onClick = { showAllActive = !showAllActive }) {
                            Text(if (showAllActive) "Show less" else "Show all")
                        }
                    }
                    OutlinedButton(onClick = NotificationHub::clearAll) {
                        Text("Clear all")
                    }
                }
            }
        }

        SectionCard(title = "Today by app") {
            if (ranking.isEmpty()) {
                Text(
                    text = "No notifications counted today yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ranking.forEachIndexed { index, (pkg, count) ->
                if (index > 0) HorizontalDivider()
                AppNotificationRow(
                    label = labels[pkg] ?: appLabel(context, pkg),
                    count = count,
                    muted = pkg in muted,
                    onMuteChange = { onMuteChange(pkg, it) },
                    onOpenSettings = { PowerIntents.open(context, PowerIntents.appNotifications(pkg)) }
                )
            }
            Text(
                text = "Muted apps have their notifications dismissed automatically. Fewer notifications means fewer screen wake-ups.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onResetCounts, enabled = counts.isNotEmpty()) {
                Text("Reset today's counts")
            }
        }
    }
}

@Composable
private fun ActiveNotificationRow(item: ActiveNotification) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = item.appLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Format.clock(item.postTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (item.title.isNotBlank()) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.text.isNotBlank()) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.clearable) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { NotificationHub.dismiss(item.key) }) { Text("Dismiss") }
                TextButton(onClick = { NotificationHub.snooze(item.key, SNOOZE_MS) }) { Text("Snooze 1h") }
            }
        }
    }
}

@Composable
private fun AppNotificationRow(
    label: String,
    count: AppNotificationCount,
    muted: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("${count.posted} today")
                    if (count.blocked > 0) append(" · ${count.blocked} auto-dismissed")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onOpenSettings) { Text("Channels") }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(checked = muted, onCheckedChange = onMuteChange)
            Text("Mute", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SwitchRow(
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

private fun hour(value: Int): String = String.format(java.util.Locale.US, "%02d:00", value)

private fun appLabel(context: android.content.Context, packageName: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
