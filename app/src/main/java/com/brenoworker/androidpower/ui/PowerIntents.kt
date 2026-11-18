package com.brenoworker.androidpower.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object PowerIntents {

    fun batterySaver(): Intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)

    fun batteryUsage(): Intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)

    fun optimizationList(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    @SuppressLint("BatteryLife")
    fun requestUnrestricted(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    fun appDetails(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    fun display(): Intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)

    fun notifications(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun open(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            val launched = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
            if (launched) return
        }
        Toast.makeText(context, "This screen is not available on your device", Toast.LENGTH_SHORT).show()
    }
}
