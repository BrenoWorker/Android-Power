package com.brenoworker.androidpower.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.brenoworker.androidpower.data.BatteryReader
import com.brenoworker.androidpower.data.BatterySnapshot
import com.brenoworker.androidpower.data.HistoryStore
import com.brenoworker.androidpower.data.PowerPrefs
import com.brenoworker.androidpower.ui.Format

class PowerMonitorService : Service() {

    private lateinit var prefs: PowerPrefs
    private var registered = false
    private var lastHistoryLevel = -1
    private var lastHistoryAt = 0L
    private var lastNotificationAt = 0L
    private var chargeAlerted = false
    private var lowAlerted = false
    private var heatAlerted = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handle(BatteryReader.read(context, intent))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PowerPrefs(this)
        Notifications.ensureChannels(this)
        val initial = BatteryReader.read(this, BatteryReader.stickyIntent(this))
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.STATUS_ID, Notifications.status(this, initial), type)
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            prefs.setMonitorEnabled(false)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) {
            unregisterReceiver(receiver)
            registered = false
        }
        super.onDestroy()
    }

    private fun handle(snapshot: BatterySnapshot) {
        val now = snapshot.timestamp
        if (now - lastNotificationAt >= NOTIFICATION_INTERVAL_MS) {
            lastNotificationAt = now
            getSystemService(NotificationManager::class.java)
                .notify(Notifications.STATUS_ID, Notifications.status(this, snapshot))
        }
        if (snapshot.level != lastHistoryLevel || now - lastHistoryAt >= HISTORY_INTERVAL_MS) {
            lastHistoryLevel = snapshot.level
            lastHistoryAt = now
            HistoryStore.append(this, HistoryStore.fromSnapshot(snapshot))
        }
        checkAlerts(snapshot)
    }

    private fun checkAlerts(snapshot: BatterySnapshot) {
        val settings = prefs.load()

        val chargeReached = settings.chargeLimitEnabled && snapshot.isPluggedIn && snapshot.level >= settings.chargeLimit
        if (chargeReached && !chargeAlerted) {
            chargeAlerted = true
            Notifications.alert(
                this,
                AlertKind.ChargeLimit,
                "Charge limit reached",
                "Battery is at ${snapshot.level}%. Unplug now to reduce battery wear."
            )
        } else if (!chargeReached && chargeAlerted) {
            chargeAlerted = false
            Notifications.cancelAlert(this, AlertKind.ChargeLimit)
        }

        val lowReached = settings.lowBatteryEnabled && !snapshot.isPluggedIn && snapshot.level <= settings.lowBatteryLevel
        if (lowReached && !lowAlerted) {
            lowAlerted = true
            Notifications.alert(
                this,
                AlertKind.LowBattery,
                "Battery low",
                "Battery is at ${snapshot.level}%. Plug in or turn on Battery Saver."
            )
        } else if (!lowReached && lowAlerted) {
            lowAlerted = false
            Notifications.cancelAlert(this, AlertKind.LowBattery)
        }

        val hot = settings.temperatureEnabled && snapshot.temperatureC >= settings.temperatureLimit
        val cooled = snapshot.temperatureC < settings.temperatureLimit - 2
        if (hot && !heatAlerted) {
            heatAlerted = true
            Notifications.alert(
                this,
                AlertKind.Temperature,
                "Battery is hot",
                "Battery temperature is ${Format.temperature(snapshot.temperatureC)}. Let the device cool down."
            )
        } else if ((cooled || !settings.temperatureEnabled) && heatAlerted) {
            heatAlerted = false
            Notifications.cancelAlert(this, AlertKind.Temperature)
        }
    }

    companion object {
        const val ACTION_STOP = "com.brenoworker.androidpower.action.STOP_MONITOR"
        private const val HISTORY_INTERVAL_MS = 5 * 60 * 1000L
        private const val NOTIFICATION_INTERVAL_MS = 10 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PowerMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PowerMonitorService::class.java))
        }
    }
}
