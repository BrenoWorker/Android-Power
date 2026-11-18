package com.brenoworker.androidpower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brenoworker.androidpower.ui.PowerViewModel
import com.brenoworker.androidpower.ui.screens.AppsScreen
import com.brenoworker.androidpower.ui.screens.DashboardScreen
import com.brenoworker.androidpower.ui.screens.MonitorScreen
import com.brenoworker.androidpower.ui.screens.PowerScreen
import com.brenoworker.androidpower.ui.theme.AndroidPowerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidPowerTheme {
                AndroidPowerApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Battery("Battery", Icons.Filled.Home),
    Power("Power", Icons.Filled.Settings),
    Apps("Apps", Icons.AutoMirrored.Filled.List),
    Monitor("Monitor", Icons.Filled.Notifications)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AndroidPowerApp(viewModel: PowerViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.Battery) }
    val battery by viewModel.battery.collectAsStateWithLifecycle()
    val power by viewModel.power.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSettings()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Android Power") }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                Tab.Battery -> DashboardScreen(battery)
                Tab.Power -> PowerScreen(power, battery)
                Tab.Apps -> AppsScreen(apps, viewModel::refreshApps)
                Tab.Monitor -> MonitorScreen(
                    settings = settings,
                    history = history,
                    onMonitorChange = viewModel::setMonitorEnabled,
                    onSettingsChange = viewModel::updateSettings,
                    onRefreshHistory = viewModel::refreshHistory,
                    onClearHistory = viewModel::clearHistory
                )
            }
        }
    }
}
