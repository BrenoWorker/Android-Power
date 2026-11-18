package com.brenoworker.androidpower.data

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

data class MemoryStatus(val availableMb: Long, val totalMb: Long, val lowMemory: Boolean) {
    val usedFraction: Float get() = if (totalMb > 0) 1f - availableMb.toFloat() / totalMb else 0f
}

data class StorageStatus(val freeBytes: Long, val totalBytes: Long) {
    val usedFraction: Float get() = if (totalBytes > 0) 1f - freeBytes.toFloat() / totalBytes else 0f
    val freeGb: Float get() = freeBytes / 1_073_741_824f
    val totalGb: Float get() = totalBytes / 1_073_741_824f
}

data class OptimizeResult(
    val appsCleaned: Int,
    val freedMb: Long,
    val cacheFreedKb: Long,
    val timestamp: Long
)

enum class CheckId { BatterySaver, Brightness, ScreenTimeout, Storage, Memory, Temperature, Unrestricted }

data class SystemCheck(val id: CheckId, val title: String, val detail: String, val ok: Boolean)

object SystemOptimizer {

    fun memory(context: Context): MemoryStatus {
        val manager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return MemoryStatus(
            availableMb = info.availMem / 1_048_576,
            totalMb = info.totalMem / 1_048_576,
            lowMemory = info.lowMemory
        )
    }

    fun storage(): StorageStatus {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageStatus(freeBytes = stat.availableBytes, totalBytes = stat.totalBytes)
    }

    suspend fun optimize(context: Context): OptimizeResult = withContext(Dispatchers.IO) {
        val before = memory(context)
        val manager = context.getSystemService(ActivityManager::class.java)
        val protectedPackages = protectedPackages(context)
        var cleaned = 0
        installedApps(context)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 }
            .filter { it.packageName !in protectedPackages }
            .forEach { app ->
                if (runCatching { manager.killBackgroundProcesses(app.packageName) }.isSuccess) cleaned++
            }
        val cacheFreed = clearOwnCache(context)
        System.gc()
        delay(1_500)
        val after = memory(context)
        OptimizeResult(
            appsCleaned = cleaned,
            freedMb = (after.availableMb - before.availableMb).coerceAtLeast(0),
            cacheFreedKb = cacheFreed / 1024,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun checks(context: Context, battery: BatterySnapshot, power: PowerState): List<SystemCheck> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val memory = memory(context)
            val storage = storage()
            val autoBrightness = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val timeoutMs = Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, 30_000)
            val unrestricted = unrestrictedCount(context)
            val needsSaver = !battery.isPluggedIn && battery.level <= 30 && !power.powerSaveMode
            val hot = battery.temperatureC >= 40f || power.isThrottling
            val lowStorage = storage.totalBytes > 0 && storage.freeBytes.toFloat() / storage.totalBytes < 0.1f
            val lowMemory = memory.lowMemory || memory.usedFraction > 0.85f
            listOf(
                SystemCheck(
                    CheckId.BatterySaver,
                    "Battery Saver",
                    if (needsSaver) "Battery is at ${battery.level}% and Battery Saver is off" else "No action needed",
                    !needsSaver
                ),
                SystemCheck(
                    CheckId.Brightness,
                    "Screen brightness",
                    when {
                        autoBrightness -> "Adaptive brightness is on"
                        brightness > 200 -> "Brightness is very high and adaptive brightness is off"
                        else -> "Adaptive brightness is off"
                    },
                    autoBrightness || brightness <= 200
                ),
                SystemCheck(
                    CheckId.ScreenTimeout,
                    "Screen timeout",
                    "Screen turns off after ${timeoutMs / 1000}s",
                    timeoutMs <= 60_000
                ),
                SystemCheck(
                    CheckId.Storage,
                    "Storage",
                    String.format(java.util.Locale.US, "%.1f GB free of %.1f GB", storage.freeGb, storage.totalGb),
                    !lowStorage
                ),
                SystemCheck(
                    CheckId.Memory,
                    "Memory",
                    "${memory.availableMb} MB free of ${memory.totalMb} MB",
                    !lowMemory
                ),
                SystemCheck(
                    CheckId.Temperature,
                    "Temperature",
                    if (hot) "Device is hot (${power.thermal.label}, ${battery.temperatureC} °C)" else "Temperature is normal",
                    !hot
                ),
                SystemCheck(
                    CheckId.Unrestricted,
                    "Background apps",
                    "$unrestricted apps can run without battery restrictions",
                    unrestricted <= 5
                )
            )
        }

    private fun unrestrictedCount(context: Context): Int {
        val power = context.getSystemService(PowerManager::class.java)
        val packageManager = context.packageManager
        return installedApps(context).count { app ->
            app.packageName != context.packageName &&
                packageManager.getLaunchIntentForPackage(app.packageName) != null &&
                power.isIgnoringBatteryOptimizations(app.packageName)
        }
    }

    @Suppress("DEPRECATION")
    private fun installedApps(context: Context): List<ApplicationInfo> =
        context.packageManager.getInstalledApplications(0)

    @Suppress("DEPRECATION")
    private fun protectedPackages(context: Context): Set<String> {
        val packageManager = context.packageManager
        val result = mutableSetOf(context.packageName)
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName?.let(result::add)
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
            ?.let(result::add)
        runCatching { WallpaperManager.getInstance(context).wallpaperInfo?.packageName }
            .getOrNull()
            ?.let(result::add)
        return result
    }

    private fun clearOwnCache(context: Context): Long {
        val dirs = listOfNotNull(context.cacheDir, context.externalCacheDir, context.codeCacheDir)
        return dirs.sumOf { dir -> dir.listFiles().orEmpty().sumOf(::deleteTree) }
    }

    private fun deleteTree(file: File): Long {
        val size = if (file.isDirectory) file.listFiles().orEmpty().sumOf(::deleteTree) else file.length()
        file.delete()
        return size
    }
}
