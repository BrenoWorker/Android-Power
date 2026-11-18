package com.brenoworker.androidpower.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.brenoworker.androidpower.data.ActiveNotification
import com.brenoworker.androidpower.data.NotificationHub
import com.brenoworker.androidpower.data.NotificationPrefs

class NotificationControlService : NotificationListenerService() {

    private lateinit var prefs: NotificationPrefs
    private val labels = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        prefs = NotificationPrefs(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationHub.attach(this)
        NotificationHub.publishCounts(prefs.todayCounts())
        publishActive()
    }

    override fun onListenerDisconnected() {
        NotificationHub.detach(this)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        NotificationHub.detach(this)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || isSummary(sbn)) {
            publishActive()
            return
        }
        val blocked = sbn.isClearable && sbn.packageName in prefs.mutedPackages()
        if (blocked) cancelNotification(sbn.key)
        NotificationHub.publishCounts(prefs.record(sbn.packageName, blocked))
        publishActive()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        publishActive()
    }

    private fun publishActive() {
        val items = runCatching { activeNotifications }.getOrNull().orEmpty()
            .filter { it.packageName != packageName && !isSummary(it) }
            .sortedByDescending { it.postTime }
            .map { sbn ->
                val extras = sbn.notification.extras
                ActiveNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = labelFor(sbn.packageName),
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    postTime = sbn.postTime,
                    clearable = sbn.isClearable
                )
            }
        NotificationHub.publishActive(items)
    }

    private fun isSummary(sbn: StatusBarNotification): Boolean =
        sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

    private fun labelFor(pkg: String): String = labels.getOrPut(pkg) {
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }
}
