package com.brenoworker.androidpower.data

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val clearable: Boolean
)

object NotificationHub {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _active = MutableStateFlow<List<ActiveNotification>>(emptyList())
    val active: StateFlow<List<ActiveNotification>> = _active.asStateFlow()

    private val _counts = MutableStateFlow<Map<String, AppNotificationCount>>(emptyMap())
    val counts: StateFlow<Map<String, AppNotificationCount>> = _counts.asStateFlow()

    @Volatile
    private var listener: NotificationListenerService? = null

    fun attach(service: NotificationListenerService) {
        listener = service
        _connected.value = true
    }

    fun detach(service: NotificationListenerService) {
        if (listener === service) {
            listener = null
            _connected.value = false
            _active.value = emptyList()
        }
    }

    fun publishActive(items: List<ActiveNotification>) {
        _active.value = items
    }

    fun publishCounts(items: Map<String, AppNotificationCount>) {
        _counts.value = items
    }

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun dismiss(key: String) {
        runCatching { listener?.cancelNotification(key) }
    }

    fun snooze(key: String, durationMs: Long) {
        runCatching { listener?.snoozeNotification(key, durationMs) }
    }

    fun clearAll() {
        runCatching { listener?.cancelAllNotifications() }
    }

    fun requestRebind(context: Context, component: Class<*>) {
        runCatching {
            NotificationListenerService.requestRebind(ComponentName(context, component))
        }
    }
}
