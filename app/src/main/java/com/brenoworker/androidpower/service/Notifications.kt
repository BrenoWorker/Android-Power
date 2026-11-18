package com.brenoworker.androidpower.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brenoworker.androidpower.MainActivity
import com.brenoworker.androidpower.R
import com.brenoworker.androidpower.data.BatterySnapshot
import com.brenoworker.androidpower.ui.Format

object Notifications {

    const val STATUS_ID = 1001
    private const val ALERT_BASE_ID = 2000
    private const val CHANNEL_STATUS = "power_status"
    private const val CHANNEL_ALERTS = "power_alerts"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Power monitor", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live battery status while the monitor is running"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Power alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Charge limit, low battery and temperature alerts"
            }
        )
    }

    fun status(context: Context, snapshot: BatterySnapshot): Notification {
        val title = "${snapshot.level}% · ${snapshot.status.label}"
        val text = listOf(
            Format.current(snapshot.currentMa),
            Format.power(snapshot.powerW),
            Format.temperature(snapshot.temperatureC)
        ).joinToString(" · ")
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, PowerMonitorService::class.java).setAction(PowerMonitorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_power)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(context))
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    fun alert(context: Context, kind: AlertKind, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_power)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ALERT_BASE_ID + kind.ordinal, notification)
    }

    fun cancelAlert(context: Context, kind: AlertKind) {
        context.getSystemService(NotificationManager::class.java).cancel(ALERT_BASE_ID + kind.ordinal)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

enum class AlertKind { ChargeLimit, LowBattery, Temperature }
