package com.oneui.monitor

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var tabDashboard: TextView
    private lateinit var tabTaskManager: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutTaskManager: View

    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnCleanMemory: MaterialButton

    // Dashboard views
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

    // Task Manager views
    private lateinit var tvProcessCount: TextView
    private lateinit var etSearchProcess: EditText
    private lateinit var rvProcesses: RecyclerView
    private lateinit var processAdapter: ProcessAdapter
    private var allProcesses = listOf<ProcessModel>()

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()
        refreshAllStats()
    }

    override fun onResume() {
        super.onResume()
        refreshAllStats()
    }

    private fun initViews() {
        tabDashboard = findViewById(R.id.tabDashboard)
        tabTaskManager = findViewById(R.id.tabTaskManager)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutTaskManager = findViewById(R.id.layoutTaskManager)

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

        tvProcessCount = findViewById(R.id.tvProcessCount)
        etSearchProcess = findViewById(R.id.etSearchProcess)
        rvProcesses = findViewById(R.id.rvProcesses)

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_blue))
    }

    private fun setupListeners() {
        tabDashboard.setOnClickListener {
            switchTab(isDashboard = true)
        }

        tabTaskManager.setOnClickListener {
            switchTab(isDashboard = false)
        }

        swipeRefresh.setOnRefreshListener {
            refreshAllStats()
            mainHandler.postDelayed({
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
            mainHandler.postDelayed({
                refreshAllStats()
                Toast.makeText(this, "Memori RAM berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
            }, 400)
        }

        etSearchProcess.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProcesses(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun switchTab(isDashboard: Boolean) {
        val activeBg = ContextCompat.getDrawable(this, R.drawable.pill_bg)
        val blueColor = ContextCompat.getColor(this, R.color.accent_blue)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val mutedColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (isDashboard) {
            tabDashboard.background = activeBg
            tabDashboard.backgroundTintList = ColorStateList.valueOf(blueColor)
            tabDashboard.setTextColor(whiteColor)

            tabTaskManager.background = null
            tabTaskManager.setTextColor(mutedColor)

            swipeRefresh.visibility = View.VISIBLE
            layoutTaskManager.visibility = View.GONE
        } else {
            tabTaskManager.background = activeBg
            tabTaskManager.backgroundTintList = ColorStateList.valueOf(blueColor)
            tabTaskManager.setTextColor(whiteColor)

            tabDashboard.background = null
            tabDashboard.setTextColor(mutedColor)

            swipeRefresh.visibility = View.GONE
            layoutTaskManager.visibility = View.VISIBLE
            loadProcessesAsync()
        }
    }

    private fun setupRecyclerView() {
        rvProcesses.layoutManager = LinearLayoutManager(this)
        processAdapter = ProcessAdapter(emptyList()) { item ->
            openAppDetails(item.packageName)
        }
        rvProcesses.adapter = processAdapter
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

    private fun filterProcesses(query: String) {
        if (query.isEmpty()) {
            processAdapter.updateList(allProcesses)
            tvProcessCount.text = "Proses Aktif (${allProcesses.size})"
        } else {
            val filtered = allProcesses.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
            processAdapter.updateList(filtered)
            tvProcessCount.text = "Ditemukan (${filtered.size})"
        }
    }

    private fun loadProcessesAsync() {
        executor.execute {
            val list = ProcessHelper.getRunningProcesses(this@MainActivity)
            mainHandler.post {
                allProcesses = list
                val currentFilter = etSearchProcess.text?.toString() ?: ""
                filterProcesses(currentFilter)
            }
        }
    }

    private fun refreshAllStats() {
        // 1. Battery & Temperature
        val battery = SystemStatsHelper.getBatteryStats(this)
        val tempFormatted = String.format(Locale.US, "%.1f°C", battery.temperatureC)
        tvBatteryTemp.text = tempFormatted
        tvBatteryLevel.text = "•  ${battery.level}% (${battery.statusText})"

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

        // Also refresh processes
        loadProcessesAsync()
    }
}
