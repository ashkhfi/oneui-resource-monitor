package com.oneui.monitor

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar

object ProcessHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun getRunningProcesses(context: Context): List<ProcessModel> {
        val pm = context.packageManager
        val list = mutableListOf<ProcessModel>()
        val seenPackages = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        if (hasUsageStatsPermission(context)) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    cal.timeInMillis,
                    now
                )

                if (!stats.isNullOrEmpty()) {
                    val sortedStats = stats
                        .filter { it.lastTimeUsed > 0 }
                        .sortedByDescending { it.lastTimeUsed }

                    for (usage in sortedStats) {
                        val pkg = usage.packageName
                        if (seenPackages.contains(pkg)) continue
                        seenPackages.add(pkg)

                        try {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val appName = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(appInfo)
                            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                            val diff = now - usage.lastTimeUsed
                            val status = when {
                                diff < 2 * 60 * 1000 -> "🟢 Sedang Aktif"
                                diff < 60 * 60 * 1000 -> "🟡 Aktif ${diff / (60 * 1000)}m lalu"
                                else -> "⚪ Aktif ${diff / (60 * 60 * 1000)}j lalu"
                            }

                            list.add(
                                ProcessModel(
                                    appName = appName,
                                    packageName = pkg,
                                    icon = icon,
                                    lastTimeUsed = usage.lastTimeUsed,
                                    formattedStatus = status,
                                    isSystemApp = isSystem
                                )
                            )
                        } catch (_: PackageManager.NameNotFoundException) {
                            // Uninstalled package, skip
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // Fallback or supplementary: RunningAppProcesses
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcesses = am.runningAppProcesses
            if (!runningProcesses.isNullOrEmpty()) {
                for (proc in runningProcesses) {
                    val pkg = proc.processName.split(":").first()
                    if (seenPackages.contains(pkg)) continue
                    seenPackages.add(pkg)

                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        list.add(
                            ProcessModel(
                                appName = appName,
                                packageName = pkg,
                                icon = icon,
                                lastTimeUsed = now,
                                formattedStatus = "🟢 Sedang Berjalan",
                                isSystemApp = isSystem
                            )
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }

        return list
    }
}
