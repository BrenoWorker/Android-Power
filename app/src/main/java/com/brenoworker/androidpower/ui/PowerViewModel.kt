package com.brenoworker.androidpower.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brenoworker.androidpower.data.AppPowerInfo
import com.brenoworker.androidpower.data.AppPowerRepository
import com.brenoworker.androidpower.data.BatteryReader
import com.brenoworker.androidpower.data.BatterySnapshot
import com.brenoworker.androidpower.data.HistorySample
import com.brenoworker.androidpower.data.HistoryStore
import com.brenoworker.androidpower.data.MonitorSettings
import com.brenoworker.androidpower.data.PowerPrefs
import com.brenoworker.androidpower.data.PowerState
import com.brenoworker.androidpower.data.PowerStateReader
import com.brenoworker.androidpower.service.PowerMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PowerViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val prefs = PowerPrefs(application)

    val battery: StateFlow<BatterySnapshot> = callbackFlow {
        var latest = BatteryReader.stickyIntent(app)
        trySend(BatteryReader.read(app, latest))
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                latest = intent
                trySend(BatteryReader.read(app, intent))
            }
        }
        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val ticker = launch {
            while (isActive) {
                delay(TICK_MS)
                trySend(BatteryReader.read(app, latest))
            }
        }
        awaitClose {
            ticker.cancel()
            app.unregisterReceiver(receiver)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BatteryReader.read(app, BatteryReader.stickyIntent(app))
    )

    val power: StateFlow<PowerState> = callbackFlow {
        trySend(PowerStateReader.read(app))
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(PowerStateReader.read(app))
            }
        }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val ticker = launch {
            while (isActive) {
                delay(TICK_MS)
                trySend(PowerStateReader.read(app))
            }
        }
        awaitClose {
            ticker.cancel()
            app.unregisterReceiver(receiver)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PowerStateReader.read(app))

    private val _settings = MutableStateFlow(prefs.load())
    val settings: StateFlow<MonitorSettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow<List<HistorySample>>(emptyList())
    val history: StateFlow<List<HistorySample>> = _history.asStateFlow()

    private val _apps = MutableStateFlow<List<AppPowerInfo>?>(null)
    val apps: StateFlow<List<AppPowerInfo>?> = _apps.asStateFlow()

    fun refreshSettings() {
        _settings.value = prefs.load()
    }

    fun updateSettings(transform: (MonitorSettings) -> MonitorSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        prefs.save(updated)
    }

    fun setMonitorEnabled(enabled: Boolean) {
        updateSettings { it.copy(monitorEnabled = enabled) }
        if (enabled) PowerMonitorService.start(app) else PowerMonitorService.stop(app)
    }

    fun refreshHistory() {
        viewModelScope.launch {
            _history.value = withContext(Dispatchers.IO) { HistoryStore.load(app) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { HistoryStore.clear(app) }
            _history.value = emptyList()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            _apps.value = null
            _apps.value = withContext(Dispatchers.IO) { AppPowerRepository.load(app) }
        }
    }

    private companion object {
        const val TICK_MS = 2_000L
    }
}
