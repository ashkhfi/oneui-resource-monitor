package com.oneui.monitor

import android.content.Context
import android.content.pm.PackageManager

enum class BloatSafety {
    SAFE,       // 100% safe to freeze/disable, pure bloatware
    RECOMMENDED,// Telemetry / background profiling
    OPTIONAL    // Useful if user doesn't use the feature
}

data class DebloatItem(
    val appName: String,
    val packageName: String,
    val description: String,
    val safety: BloatSafety,
    val isInstalled: Boolean,
    var isFrozen: Boolean,
    val ramEstimateMB: Int
)

object DebloatHelper {

    private val KNOWN_BLOATWARE = listOf(
        DebloatItem(
            appName = "Facebook Services",
            packageName = "com.facebook.services",
            description = "Telemetri dan background sync Facebook, memakan RAM secara konstan.",
            safety = BloatSafety.SAFE,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 45
        ),
        DebloatItem(
            appName = "Facebook App Manager",
            packageName = "com.facebook.appmanager",
            description = "Pengelola pembaruan internal Facebook di latar belakang.",
            safety = BloatSafety.SAFE,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 30
        ),
        DebloatItem(
            appName = "Microsoft App Manager",
            packageName = "com.microsoft.appmanager",
            description = "Layanan pra-instal Microsoft untuk sinkronisasi Link to Windows.",
            safety = BloatSafety.SAFE,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 55
        ),
        DebloatItem(
            appName = "Samsung Rubin (Customization Service)",
            packageName = "com.samsung.android.rubin.app",
            description = "Pelacak kebiasaan pengguna untuk iklan dan profil data.",
            safety = BloatSafety.RECOMMENDED,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 75
        ),
        DebloatItem(
            appName = "Samsung Free / SPage",
            packageName = "com.samsung.android.app.spage",
            description = "Feed berita bawaan di layar kiri Home Screen yang menyedot data.",
            safety = BloatSafety.RECOMMENDED,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 40
        ),
        DebloatItem(
            appName = "Samsung Sticker Center",
            packageName = "com.samsung.android.stickercenter",
            description = "Background service stiker keyboard yang sering berjalan sendiri.",
            safety = BloatSafety.SAFE,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 25
        ),
        DebloatItem(
            appName = "AR Avatar Preload",
            packageName = "com.samsung.android.app.camera.sticker.facearavatar.preload",
            description = "Aset animasi AR Emoji kamera yang jarang digunakan.",
            safety = BloatSafety.SAFE,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 20
        ),
        DebloatItem(
            appName = "Bixby Voice Wakeup",
            packageName = "com.samsung.android.bixby.wakeup",
            description = "Mic listener latar belakang yang menyedot baterai secara pasif.",
            safety = BloatSafety.OPTIONAL,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 35
        ),
        DebloatItem(
            appName = "Game Optimizing Service (GOS)",
            packageName = "com.samsung.android.game.gos",
            description = "Pengontrol performa game yang membatasi clock CPU/GPU.",
            safety = BloatSafety.OPTIONAL,
            isInstalled = false,
            isFrozen = false,
            ramEstimateMB = 50
        )
    )

    fun getDebloatList(context: Context): List<DebloatItem> {
        val pm = context.packageManager
        val result = mutableListOf<DebloatItem>()

        for (item in KNOWN_BLOATWARE) {
            try {
                val appInfo = pm.getApplicationInfo(item.packageName, 0)
                val isFrozen = !appInfo.enabled
                result.add(
                    item.copy(
                        isInstalled = true,
                        isFrozen = isFrozen
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed or uninstalled
            }
        }
        return result
    }
}
