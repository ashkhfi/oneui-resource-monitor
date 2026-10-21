package com.oneui.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.util.Locale
import kotlin.math.abs

data class BatteryStats(
    val level: Int,
    val temperatureC: Float,
    val voltageV: Float,
    val currentMA: Int,
    val statusText: String,
    val healthText: String,
    val technology: String
)

data class RamStats(
    val usedBytes: Long,
    val totalBytes: Long,
    val availBytes: Long,
    val usedPercent: Int,
    val formattedUsed: String,
    val formattedTotal: String,
    val formattedAvail: String
)

data class StorageStats(
    val usedBytes: Long,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedPercent: Int,
    val formattedUsed: String,
    val formattedTotal: String,
    val formattedFree: String
)

data class SystemStats(
    val cpuCores: Int,
    val architecture: String,
    val deviceModel: String,
    val androidVersion: String,
    val oneUiVersion: String,
    val uptimeFormatted: String
)

object SystemStatsHelper {

    fun getBatteryStats(context: Context): BatteryStats {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = rawTemp / 10.0f

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 0

        val rawVoltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageV = if (rawVoltage > 1000) rawVoltage / 1000.0f else rawVoltage.toFloat()

        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val statusInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusText = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Mengisi Daya"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Sedang Digunakan"
            BatteryManager.BATTERY_STATUS_FULL -> "Penuh (100%)"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Tidak Mengisi"
            else -> "Normal"
        }

        val healthInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Baik (Optimal)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu Panas!"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Rusak"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan Berlebih"
            BatteryManager.BATTERY_HEALTH_COLD -> "Terlalu Dingin"
            else -> "Normal"
        }

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val rawCurrent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // Convert microamperes to milliamperes if > 10000
        val currentMA = if (abs(rawCurrent) > 10000) rawCurrent / 1000 else rawCurrent

        return BatteryStats(
            level = batteryPct,
            temperatureC = tempC,
            voltageV = voltageV,
            currentMA = currentMA,
            statusText = statusText,
            healthText = healthText,
            technology = tech
        )
    }

    fun getRamStats(context: Context): RamStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val total = memInfo.totalMem
        val avail = memInfo.availMem
        val used = total - avail
        val percent = if (total > 0) ((used.toDouble() / total.toDouble()) * 100).toInt() else 0

        return RamStats(
            usedBytes = used,
            totalBytes = total,
            availBytes = avail,
            usedPercent = percent,
            formattedUsed = formatBytes(used),
            formattedTotal = formatBytes(total),
            formattedAvail = formatBytes(avail)
        )
    }

    fun getStorageStats(): StorageStats {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availBlocks * blockSize
        val usedBytes = totalBytes - freeBytes
        val percent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0

        return StorageStats(
            usedBytes = usedBytes,
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedPercent = percent,
            formattedUsed = formatBytes(usedBytes),
            formattedTotal = formatBytes(totalBytes),
            formattedFree = formatBytes(freeBytes)
        )
    }

    fun getSystemStats(): SystemStats {
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val androidVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        // Read One UI property
        var oneUi = "One UI"
        try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            val ver = getMethod.invoke(null, "ro.build.version.oneui", "") as String
            if (ver.isNotEmpty()) {
                val num = ver.toIntOrNull()
                if (num != null) {
                    val major = num / 10000
                    val minor = (num % 10000) / 100
                    oneUi = "One UI $major.$minor"
                }
            }
        } catch (_: Exception) {
            oneUi = "One UI"
        }

        val uptimeSec = SystemClock.elapsedRealtime() / 1000
        val hours = uptimeSec / 3600
        val mins = (uptimeSec % 3600) / 60
        val uptimeFormatted = "${hours}j ${mins}m"

        return SystemStats(
            cpuCores = cores,
            architecture = arch,
            deviceModel = model,
            androidVersion = androidVer,
            oneUiVersion = oneUi,
            uptimeFormatted = uptimeFormatted
        )
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return String.format(Locale.US, "%.1f GB", gb)
    }
}
