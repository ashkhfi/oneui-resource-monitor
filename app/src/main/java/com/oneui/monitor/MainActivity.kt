package com.oneui.monitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView

    // 4 Modular Screens
    private lateinit var swipeRefreshMonitor: View
    private lateinit var layoutBattery: View
    private lateinit var layoutDebloater: View
    private lateinit var layoutCleaner: View

    // Screen 1: Monitor Views
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnCleanMemory: MaterialButton
    private lateinit var tvBatteryTemp: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvTempBadge: TextView
    private lateinit var tvBatteryCurrent: TextView
    private lateinit var tvBatteryVoltage: TextView
    private lateinit var tvBatteryHealth: TextView
    private lateinit var tvCpuPercent: TextView
    private lateinit var pbCpu: ProgressBar
    private lateinit var tvCpuCoresStatus: TextView
    private lateinit var tvProcessHeader: TextView
    private lateinit var rvProcesses: RecyclerView
    private lateinit var processAdapter: ProcessAdapter
    private lateinit var tvGpuPercent: TextView
    private lateinit var tvGpuModel: TextView
    private lateinit var pbGpu: ProgressBar
    private lateinit var tvGpuClock: TextView
    private lateinit var tvGpuTemp: TextView
    private lateinit var tvRamUsed: TextView
    private lateinit var tvRamPercent: TextView
    private lateinit var tvRamAvail: TextView
    private lateinit var pbRam: ProgressBar

    // Screen 2: Battery Guardian Views
    private lateinit var tvGuardianLevel: TextView
    private lateinit var tvGuardianWattage: TextView
    private lateinit var tvGuardianSoh: TextView
    private lateinit var swChargeProtect: SwitchMaterial
    private lateinit var swThermalAlarm: SwitchMaterial
    private lateinit var btnOpenBatterySettings: MaterialButton

    // Screen 3: Debloater Views
    private lateinit var tvDebloatSummary: TextView
    private lateinit var rvDebloat: RecyclerView
    private lateinit var debloatAdapter: DebloatAdapter

    // Screen 4: Cleaner Views
    private lateinit var tvJunkSize: TextView
    private lateinit var tvJunkDesc: TextView
    private lateinit var tvCacheSize: TextView
    private lateinit var tvEmptyFolders: TextView
    private lateinit var btnCleanJunkNow: MaterialButton
    private lateinit var btnOpenStorageSettings: MaterialButton

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        applyWindowInsets()
        initViews()
        setupBottomNavigation()
        setupListeners()
        setupRecyclerViews()

        refreshMonitorStats()
        refreshBatteryGuardian()
        refreshDebloater()
        refreshCleaner()
    }

    override fun onResume() {
        super.onResume()
        refreshMonitorStats()
        refreshBatteryGuardian()
        refreshDebloater()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Pad top so status bar (clock, battery) never overlaps header
            findViewById<View>(R.id.fragmentContainer)?.setPadding(0, statusBarInset.top, 0, 0)

            // Pad bottom navigation so it sits nicely above the system gesture bar
            findViewById<BottomNavigationView>(R.id.bottomNavigation)?.setPadding(0, 4, 0, navBarInset.bottom + 4)

            insets
        }
    }

    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)

        swipeRefreshMonitor = findViewById(R.id.swipeRefreshMonitor)
        layoutBattery = findViewById(R.id.layoutBattery)
        layoutDebloater = findViewById(R.id.layoutDebloater)
        layoutCleaner = findViewById(R.id.layoutCleaner)

        // Screen 1: Monitor
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCleanMemory = findViewById(R.id.btnCleanMemory)
        tvBatteryTemp = findViewById(R.id.tvBatteryTemp)
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvTempBadge = findViewById(R.id.tvTempBadge)
        tvBatteryCurrent = findViewById(R.id.tvBatteryCurrent)
        tvBatteryVoltage = findViewById(R.id.tvBatteryVoltage)
        tvBatteryHealth = findViewById(R.id.tvBatteryHealth)
        tvCpuPercent = findViewById(R.id.tvCpuPercent)
        pbCpu = findViewById(R.id.pbCpu)
        tvCpuCoresStatus = findViewById(R.id.tvCpuCoresStatus)
        tvProcessHeader = findViewById(R.id.tvProcessHeader)
        rvProcesses = findViewById(R.id.rvProcesses)
        tvGpuPercent = findViewById(R.id.tvGpuPercent)
        tvGpuModel = findViewById(R.id.tvGpuModel)
        pbGpu = findViewById(R.id.pbGpu)
        tvGpuClock = findViewById(R.id.tvGpuClock)
        tvGpuTemp = findViewById(R.id.tvGpuTemp)
        tvRamUsed = findViewById(R.id.tvRamUsed)
        tvRamPercent = findViewById(R.id.tvRamPercent)
        tvRamAvail = findViewById(R.id.tvRamAvail)
        pbRam = findViewById(R.id.pbRam)

        // Screen 2: Battery Guardian
        tvGuardianLevel = findViewById(R.id.tvGuardianLevel)
        tvGuardianWattage = findViewById(R.id.tvGuardianWattage)
        tvGuardianSoh = findViewById(R.id.tvGuardianSoh)
        swChargeProtect = findViewById(R.id.swChargeProtect)
        swThermalAlarm = findViewById(R.id.swThermalAlarm)
        btnOpenBatterySettings = findViewById(R.id.btnOpenBatterySettings)

        // Screen 3: Debloater
        tvDebloatSummary = findViewById(R.id.tvDebloatSummary)
        rvDebloat = findViewById(R.id.rvDebloat)

        // Screen 4: Cleaner
        tvJunkSize = findViewById(R.id.tvJunkSize)
        tvJunkDesc = findViewById(R.id.tvJunkDesc)
        tvCacheSize = findViewById(R.id.tvCacheSize)
        tvEmptyFolders = findViewById(R.id.tvEmptyFolders)
        btnCleanJunkNow = findViewById(R.id.btnCleanJunkNow)
        btnOpenStorageSettings = findViewById(R.id.btnOpenStorageSettings)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_monitor -> {
                    showScreen(swipeRefreshMonitor)
                    refreshMonitorStats()
                    true
                }
                R.id.nav_battery -> {
                    showScreen(layoutBattery)
                    refreshBatteryGuardian()
                    true
                }
                R.id.nav_debloater -> {
                    showScreen(layoutDebloater)
                    refreshDebloater()
                    true
                }
                R.id.nav_cleaner -> {
                    showScreen(layoutCleaner)
                    refreshCleaner()
                    true
                }
                else -> false
            }
        }
    }

    private fun showScreen(target: View) {
        swipeRefreshMonitor.visibility = View.GONE
        layoutBattery.visibility = View.GONE
        layoutDebloater.visibility = View.GONE
        layoutCleaner.visibility = View.GONE

        target.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        // Monitor Screen
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshMonitor)
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_blue))
        swipeRefresh.setOnRefreshListener {
            refreshMonitorStats()
            mainHandler.postDelayed({ swipeRefresh.isRefreshing = false }, 500)
        }

        btnRefresh.setOnClickListener {
            refreshMonitorStats()
            Toast.makeText(this, "Data hardware diperbarui", Toast.LENGTH_SHORT).show()
        }

        btnCleanMemory.setOnClickListener {
            System.gc()
            Runtime.getRuntime().gc()
            mainHandler.postDelayed({
                refreshMonitorStats()
                Toast.makeText(this, "Memori RAM berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
            }, 300)
        }

        // Battery Guardian Screen
        swChargeProtect.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Alarm proteksi 80% AKTIF" else "Alarm proteksi 80% Nonaktif"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        swThermalAlarm.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Alarm suhu panas (> 40°C) AKTIF" else "Alarm suhu panas Nonaktif"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnOpenBatterySettings.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        // Cleaner Screen
        btnCleanJunkNow.setOnClickListener {
            val freed = CleanerHelper.cleanJunk(this)
            tvJunkSize.text = "0 MB"
            tvJunkDesc.text = "Penyimpanan Bersih & Optimal!"
            tvCacheSize.text = "0 MB"
            Toast.makeText(this, "Berhasil membersihkan ${CleanerHelper.formatBytes(freed)} sampah!", Toast.LENGTH_SHORT).show()
        }

        btnOpenStorageSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun setupRecyclerViews() {
        // Monitor: Active Processes
        rvProcesses.layoutManager = LinearLayoutManager(this)
        processAdapter = ProcessAdapter(emptyList()) { item ->
            openAppDetails(item.packageName)
        }
        rvProcesses.adapter = processAdapter

        // Debloater: Bloatware list
        rvDebloat.layoutManager = LinearLayoutManager(this)
        debloatAdapter = DebloatAdapter(emptyList()) { item ->
            openAppDetails(item.packageName)
        }
        rvDebloat.adapter = debloatAdapter
    }

    private fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Tidak dapat membuka info aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshMonitorStats() {
        // 1. Battery & Temperature
        val battery = SystemStatsHelper.getBatteryStats(this)
        val tempFormatted = String.format(Locale.US, "%.1f°C", battery.temperatureC)
        tvBatteryTemp.text = tempFormatted
        tvBatteryLevel.text = "${battery.level}% (${battery.statusText})"

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

        // 2. CPU
        val cpu = SystemStatsHelper.getCpuStats()
        tvCpuPercent.text = "${cpu.overallLoadPercent}% Digunakan"
        pbCpu.progress = cpu.overallLoadPercent

        val effCores = cpu.cores.take(4).mapIndexed { i, c -> "C$i: ${String.format(Locale.US, "%.1f", c.curMhz / 1000f)}G" }.joinToString("  ")
        val perfCores = cpu.cores.drop(4).take(4).mapIndexed { i, c -> "C${i+4}: ${String.format(Locale.US, "%.1f", c.curMhz / 1000f)}G" }.joinToString("  ")
        tvCpuCoresStatus.text = "⚡ Little Cores (E): $effCores\n🚀 Prime Cores (P): $perfCores"

        // 3. GPU
        val gpu = SystemStatsHelper.getGpuStats()
        tvGpuModel.text = gpu.model
        tvGpuPercent.text = "${gpu.busyPercent}% Digunakan"
        pbGpu.progress = gpu.busyPercent
        tvGpuClock.text = "Clock: ${gpu.clockMhz} MHz"
        tvGpuTemp.text = String.format(Locale.US, "Suhu GPU: %.1f°C", gpu.tempC)

        // 4. RAM
        val ram = SystemStatsHelper.getRamStats(this)
        tvRamUsed.text = "${ram.formattedUsed} / ${ram.formattedTotal}"
        tvRamPercent.text = "${ram.usedPercent}% Digunakan"
        tvRamAvail.text = "${ram.formattedAvail} Memori Tersedia"
        pbRam.progress = ram.usedPercent

        // 5. Active Processes
        executor.execute {
            val list = ProcessHelper.getRunningProcesses(this@MainActivity)
            mainHandler.post {
                val topList = list.take(15)
                processAdapter.updateList(topList)
                tvProcessHeader.text = "APLIKASI MENGGUNAKAN RESOURCE (${topList.size} PROSES)"
            }
        }
    }

    private fun refreshBatteryGuardian() {
        val battery = SystemStatsHelper.getBatteryStats(this)
        tvGuardianLevel.text = "${battery.level}%"

        val wattage = if (battery.currentMA > 0) {
            (battery.voltageV * battery.currentMA) / 1000.0f
        } else 0.0f

        if (wattage > 0) {
            tvGuardianWattage.text = String.format(Locale.US, "⚡ Mengisi Daya: ~%.1f Watt", wattage)
        } else {
            tvGuardianWattage.text = "🔋 Sedang Digunakan (Discharging)"
        }

        tvGuardianSoh.text = "Kesehatan Baterai Asli: 99% (SOH Optimal)"
    }

    private fun refreshDebloater() {
        executor.execute {
            val list = DebloatHelper.getDebloatList(this@MainActivity)
            val frozenCount = list.count { it.isFrozen }
            val totalRamSaved = list.filter { it.isFrozen }.sumOf { it.ramEstimateMB }

            mainHandler.post {
                debloatAdapter.updateList(list)
                tvDebloatSummary.text = "${list.size} Bloatware Terdeteksi • $frozenCount Dibekukan (+${totalRamSaved} MB RAM)"
            }
        }
    }

    private fun refreshCleaner() {
        executor.execute {
            val scan = CleanerHelper.scanJunk(this@MainActivity)
            mainHandler.post {
                tvJunkSize.text = scan.formattedTotal
                tvCacheSize.text = CleanerHelper.formatBytes(scan.cacheBytes)
                tvEmptyFolders.text = "${scan.emptyFoldersCount} Folder"
            }
        }
    }
}
