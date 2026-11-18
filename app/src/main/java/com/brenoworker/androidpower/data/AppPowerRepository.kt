package com.brenoworker.androidpower.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

data class AppPowerInfo(
    val packageName: String,
    val label: String,
    val unrestricted: Boolean,
    val system: Boolean
)

object AppPowerRepository {

    fun load(context: Context): List<AppPowerInfo> {
        val packageManager = context.packageManager
        val power = context.getSystemService(PowerManager::class.java)
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        return installed
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { info ->
                AppPowerInfo(
                    packageName = info.packageName,
                    label = info.loadLabel(packageManager).toString(),
                    unrestricted = power.isIgnoringBatteryOptimizations(info.packageName),
                    system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }
            .sortedWith(compareBy<AppPowerInfo>({ !it.unrestricted }, { it.label.lowercase() }))
            .toList()
    }
}
