package com.brenoworker.androidpower.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brenoworker.androidpower.data.PowerPrefs

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (PowerPrefs(context).load().monitorEnabled) {
            runCatching { PowerMonitorService.start(context) }
        }
    }
}
