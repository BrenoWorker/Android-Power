package com.brenoworker.androidpower.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.brenoworker.androidpower.data.AppPowerInfo
import com.brenoworker.androidpower.ui.PowerIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppFilter(val label: String) {
    All("All"),
    Unrestricted("Unrestricted"),
    Optimized("Optimized");

    fun matches(app: AppPowerInfo): Boolean = when (this) {
        All -> true
        Unrestricted -> app.unrestricted
        Optimized -> !app.unrestricted
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(apps: List<AppPowerInfo>?, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(AppFilter.All) }

    LaunchedEffect(Unit) {
        if (apps == null) onRefresh()
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) }
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }

        if (apps == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val visible = apps.filter { app ->
            filter.matches(app) && (query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true))
        }

        Text(
            text = "${visible.size} apps · ${apps.count { it.unrestricted }} unrestricted from battery optimization",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visible, key = { it.packageName }) { app ->
                AppRow(app) { PowerIntents.open(context, PowerIntents.appDetails(app.packageName)) }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppPowerInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap()
            }.getOrNull()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (app.system) "${app.packageName} · system" else app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = onClick,
                label = { Text(if (app.unrestricted) "Unrestricted" else "Optimized") },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = if (app.unrestricted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}
