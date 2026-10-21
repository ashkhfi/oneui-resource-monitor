package com.oneui.monitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnCleanMemory: MaterialButton
    private lateinit var btnOpenRunningServices: MaterialButton

    // Battery / Thermal views
    private lateinit var tvBatteryTemp: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvTempBadge: TextView
    private lateinit var tvBatteryCurrent: TextView
    private lateinit var tvBatteryVoltage: TextView
    private lateinit var tvBatteryHealth: TextView

    // CPU views
    private lateinit var tvCpuPercent: TextView
    private lateinit var pbCpu: ProgressBar
    private lateinit var tvCpuCoresStatus: TextView

    // Process Breakdown
    private lateinit var tvProcessHeader: TextView
    private lateinit var rvProcesses: RecyclerView
    private lateinit var processAdapter: ProcessAdapter

    // GPU views
    private lateinit var tvGpuPercent: TextView
    private lateinit var tvGpuModel: TextView
    private lateinit var pbGpu: ProgressBar
    private lateinit var tvGpuClock: TextView
    private lateinit var tvGpuTemp: TextView

    // RAM views
    private lateinit var tvRamUsed: TextView
    private lateinit var tvRamPercent: TextView
    private lateinit var tvRamAvail: TextView
    private lateinit var pbRam: ProgressBar

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
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCleanMemory = findViewById(R.id.btnCleanMemory)
        btnOpenRunningServices = findViewById(R.id.btnOpenRunningServices)

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

        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.accent_blue))
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener {
            refreshAllStats()
            mainHandler.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 600)
        }

        btnRefresh.setOnClickListener {
            refreshAllStats()
            Toast.makeText(this, "Data hardware diperbarui", Toast.LENGTH_SHORT).show()
        }

        btnCleanMemory.setOnClickListener {
            System.gc()
            Runtime.getRuntime().gc()
            mainHandler.postDelayed({
                refreshAllStats()
                Toast.makeText(this, "Memori RAM berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
            }, 400)
        }

        btnOpenRunningServices.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Buka Opsi Pengembang di Pengaturan HP", Toast.LENGTH_SHORT).show()
            }
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

    private fun loadProcessesAsync() {
        executor.execute {
            val list = ProcessHelper.getRunningProcesses(this@MainActivity)
            mainHandler.post {
                val topList = list.take(15)
                processAdapter.updateList(topList)
                tvProcessHeader.text = "APLIKASI MENGGUNAKAN RESOURCE (${topList.size} PROSES)"
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

        // 2. CPU
        val cpu = SystemStatsHelper.getCpuStats()
        tvCpuPercent.text = "${cpu.overallLoadPercent}% Digunakan"
        pbCpu.progress = cpu.overallLoadPercent

        val coreSummary = StringBuilder()
        cpu.cores.forEachIndexed { idx, core ->
            val ghz = core.curMhz.toFloat() / 1000f
            coreSummary.append(String.format(Locale.US, "C%d: %.1fGHz ", idx, ghz))
            if (idx == 3) coreSummary.append("\n")
        }
        tvCpuCoresStatus.text = coreSummary.toString().trim()

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
        loadProcessesAsync()
    }
}
