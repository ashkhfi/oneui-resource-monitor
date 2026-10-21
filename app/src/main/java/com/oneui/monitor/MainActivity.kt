package com.oneui.monitor

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnCleanMemory: MaterialButton

    private lateinit var tvBatteryTemp: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvTempBadge: TextView
    private lateinit var tvBatteryCurrent: TextView
    private lateinit var tvBatteryVoltage: TextView
    private lateinit var tvBatteryHealth: TextView

    private lateinit var tvRamUsed: TextView
    private lateinit var tvRamPercent: TextView
    private lateinit var tvRamAvail: TextView
    private lateinit var pbRam: ProgressBar

    private lateinit var tvStorageUsed: TextView
    private lateinit var tvStoragePercent: TextView
    private lateinit var tvStorageFree: TextView
    private lateinit var pbStorage: ProgressBar

    private lateinit var tvDeviceModel: TextView
    private lateinit var tvCpuCores: TextView
    private lateinit var tvOsVersion: TextView
    private lateinit var tvUptime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        refreshAllStats()
    }

    override fun onResume() {
        super.onResume()
        refreshAllStats()
    }

    private fun initViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCleanMemory = findViewById(R.id.btnCleanMemory)

        tvBatteryTemp = findViewById(R.id.tvBatteryTemp)
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvTempBadge = findViewById(R.id.tvTempBadge)
        tvBatteryCurrent = findViewById(R.id.tvBatteryCurrent)
        tvBatteryVoltage = findViewById(R.id.tvBatteryVoltage)
        tvBatteryHealth = findViewById(R.id.tvBatteryHealth)

        tvRamUsed = findViewById(R.id.tvRamUsed)
        tvRamPercent = findViewById(R.id.tvRamPercent)
        tvRamAvail = findViewById(R.id.tvRamAvail)
        pbRam = findViewById(R.id.pbRam)

        tvStorageUsed = findViewById(R.id.tvStorageUsed)
        tvStoragePercent = findViewById(R.id.tvStoragePercent)
        tvStorageFree = findViewById(R.id.tvStorageFree)
        pbStorage = findViewById(R.id.pbStorage)

        tvDeviceModel = findViewById(R.id.tvDeviceModel)
        tvCpuCores = findViewById(R.id.tvCpuCores)
        tvOsVersion = findViewById(R.id.tvOsVersion)
        tvUptime = findViewById(R.id.tvUptime)

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_blue))
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            refreshAllStats()
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefresh.isRefreshing = false
            }, 600)
        }

        btnRefresh.setOnClickListener {
            refreshAllStats()
            Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
        }

        btnCleanMemory.setOnClickListener {
            System.gc()
            Runtime.getRuntime().gc()
            Handler(Looper.getMainLooper()).postDelayed({
                refreshAllStats()
                Toast.makeText(this, "Memori RAM berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
            }, 400)
        }
    }

    private fun refreshAllStats() {
        // 1. Battery & Temperature
        val battery = SystemStatsHelper.getBatteryStats(this)
        val tempFormatted = String.format(Locale.US, "%.1f°C", battery.temperatureC)
        tvBatteryTemp.text = tempFormatted
        tvBatteryLevel.text = "•  ${battery.level}% (${battery.statusText})"

        // Thermal badge color & label
        val coolColor = ContextCompat.getColor(this, R.color.temp_cool)
        val warmColor = ContextCompat.getColor(this, R.color.temp_warm)
        val hotColor = ContextCompat.getColor(this, R.color.temp_hot)

        when {
            battery.temperatureC < 38.0f -> {
                tvTempBadge.text = "Suhu Dingin"
                tvTempBadge.setTextColor(coolColor)
                tvBatteryTemp.setTextColor(coolColor)
            }
            battery.temperatureC <= 42.5f -> {
                tvTempBadge.text = "Suhu Hangat"
                tvTempBadge.setTextColor(warmColor)
                tvBatteryTemp.setTextColor(warmColor)
            }
            else -> {
                tvTempBadge.text = "Ponsel Panas!"
                tvTempBadge.setTextColor(hotColor)
                tvBatteryTemp.setTextColor(hotColor)
            }
        }

        val currentPrefix = if (battery.currentMA > 0) "+" else ""
        tvBatteryCurrent.text = "$currentPrefix${battery.currentMA} mA"
        tvBatteryVoltage.text = String.format(Locale.US, "%.2f V", battery.voltageV)
        tvBatteryHealth.text = battery.healthText

        // 2. RAM Memory
        val ram = SystemStatsHelper.getRamStats(this)
        tvRamUsed.text = "${ram.formattedUsed} / ${ram.formattedTotal}"
        tvRamPercent.text = "${ram.usedPercent}% Digunakan"
        tvRamAvail.text = "${ram.formattedAvail} Memori Tersedia"
        pbRam.progress = ram.usedPercent

        // 3. Storage
        val storage = SystemStatsHelper.getStorageStats()
        tvStorageUsed.text = "${storage.formattedUsed} / ${storage.formattedTotal}"
        tvStoragePercent.text = "${storage.usedPercent}% Digunakan"
        tvStorageFree.text = "${storage.formattedFree} Ruang Kosong"
        pbStorage.progress = storage.usedPercent

        // 4. System Specs
        val sys = SystemStatsHelper.getSystemStats()
        tvDeviceModel.text = sys.deviceModel
        tvCpuCores.text = "${sys.cpuCores} Core (${sys.architecture})"
        tvOsVersion.text = "${sys.oneUiVersion} • ${sys.androidVersion}"
        tvUptime.text = sys.uptimeFormatted
    }
}
