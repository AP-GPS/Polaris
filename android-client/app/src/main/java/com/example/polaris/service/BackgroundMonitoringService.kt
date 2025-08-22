package com.example.polaris.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.polaris.R
import com.example.polaris.data.repo.SnapshotRepository
import com.example.polaris.data.entity.MonitoringSnapshot
import com.example.polaris.auth.AuthManager
import com.example.polaris.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson

class BackgroundMonitoringService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: SnapshotRepository
    private lateinit var authManager: AuthManager
    private var apiService: ApiService? = null

    private var isMonitoring = false
    private var updateInterval = 30L // seconds
    private var autoSaveSnapshots = true
    private var autoUploadSnapshots = true

    companion object {
        const val CHANNEL_ID = "polaris_monitoring_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_MONITORING = "com.example.polaris.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.polaris.STOP_MONITORING"
        const val ACTION_UPDATE_SETTINGS = "com.example.polaris.UPDATE_SETTINGS"

        const val EXTRA_UPDATE_INTERVAL = "update_interval"
        const val EXTRA_AUTO_SAVE = "auto_save"
        const val EXTRA_AUTO_UPLOAD = "auto_upload"

        fun startMonitoring(context: Context, updateInterval: Long = 30, autoSave: Boolean = true, autoUpload: Boolean = true) {
            val intent = Intent(context, BackgroundMonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_UPDATE_INTERVAL, updateInterval)
                putExtra(EXTRA_AUTO_SAVE, autoSave)
                putExtra(EXTRA_AUTO_UPLOAD, autoUpload)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, BackgroundMonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun updateSettings(context: Context, updateInterval: Long, autoSave: Boolean, autoUpload: Boolean) {
            val intent = Intent(context, BackgroundMonitoringService::class.java).apply {
                action = ACTION_UPDATE_SETTINGS
                putExtra(EXTRA_UPDATE_INTERVAL, updateInterval)
                putExtra(EXTRA_AUTO_SAVE, autoSave)
                putExtra(EXTRA_AUTO_UPLOAD, autoUpload)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = SnapshotRepository(this)
        authManager = AuthManager.getInstance(this)
        createNotificationChannel()
        initializeApiService()
    }

    private fun initializeApiService() {
        val token = authManager.getToken()
        if (token != null) {
            apiService = ApiService.create(token)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                if (!authManager.isAuthenticated()) {
                    updateNotification("Authentication required - stopping service")
                    stopSelf()
                    return START_NOT_STICKY
                }

                updateInterval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, 30L)
                autoSaveSnapshots = intent.getBooleanExtra(EXTRA_AUTO_SAVE, true)
                autoUploadSnapshots = intent.getBooleanExtra(EXTRA_AUTO_UPLOAD, true)
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
            ACTION_UPDATE_SETTINGS -> {
                updateInterval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, updateInterval)
                autoSaveSnapshots = intent.getBooleanExtra(EXTRA_AUTO_SAVE, autoSaveSnapshots)
                autoUploadSnapshots = intent.getBooleanExtra(EXTRA_AUTO_UPLOAD, autoUploadSnapshots)
                updateNotification("Settings updated - Interval: ${updateInterval}s")
            }
        }
        return START_STICKY // Restart service if killed by system
    }

    private fun startMonitoring() {
        if (isMonitoring) return

        // Re-initialize API service in case token changed
        initializeApiService()

        if (apiService == null && autoUploadSnapshots) {
            updateNotification("Authentication error - uploads disabled")
            autoUploadSnapshots = false
        }

        isMonitoring = true
        startForeground(NOTIFICATION_ID, createNotification("Starting monitoring..."))

        // Start signal strength listening
        SignalStrengthCollector.startListening(this)

        serviceScope.launch {
            var consecutiveFailures = 0
            val maxFailures = 3
            var consecutiveAuthFailures = 0
            val maxAuthFailures = 2

            while (isMonitoring) {
                try {
                    // Check authentication status
                    if (!authManager.isAuthenticated()) {
                        updateNotification("Authentication lost - stopping service")
                        stopMonitoring()
                        break
                    }

                    val snapshot = collectSnapshot()
                    var success = true

                    if (autoSaveSnapshots && snapshot != null) {
                        repository.insert(snapshot)
                    }

                    if (autoUploadSnapshots && snapshot != null && apiService != null) {
                        val uploadSuccess = uploadSnapshot(snapshot)
                        if (!uploadSuccess) {
                            success = false
                            consecutiveFailures++

                            // Check if it's an auth failure
                            if (consecutiveFailures >= maxFailures) {
                                consecutiveAuthFailures++
                                if (consecutiveAuthFailures >= maxAuthFailures) {
                                    updateNotification("Authentication failed - stopping uploads")
                                    autoUploadSnapshots = false
                                }
                            }
                        } else {
                            consecutiveFailures = 0
                            consecutiveAuthFailures = 0
                        }
                    }

                    val status = when {
                        !autoUploadSnapshots && autoSaveSnapshots -> {
                            "Active (Save only) - ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                        }
                        success -> {
                            "Active - Last update: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                        }
                        else -> {
                            "Active - Upload failed ($consecutiveFailures/$maxFailures)"
                        }
                    }

                    updateNotification(status)

                    // If too many consecutive failures, reduce update frequency
                    val actualInterval = if (consecutiveFailures >= maxFailures) {
                        updateInterval * 2 // Double the interval on persistent failures
                    } else {
                        updateInterval
                    }

                    delay(actualInterval * 1000)

                } catch (e: Exception) {
                    consecutiveFailures++
                    updateNotification("Error - ${e.message?.take(30) ?: "Unknown error"}")
                    delay(updateInterval * 1000)
                }
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        SignalStrengthCollector.stopListening(applicationContext)
        serviceScope.cancel()
        stopForeground(true)
        stopSelf()
    }

    private suspend fun collectSnapshot(): MonitoringSnapshot? {
        return try {
            val location = getLastKnownLocation(this)
            val cellularInfo = if (hasStatePermission(this)) {
                getCellularInfo(this)
            } else {
                "Permission not granted"
            }

            var signalInfo = "No signal data"
            SignalStrengthCollector.signalInfo.collectLatest { signal ->
                signalInfo = signal
                return@collectLatest // Exit after first emission
            }

            MonitoringSnapshot(
                timestamp = System.currentTimeMillis(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                signalInfo = signalInfo,
                cellInfo = cellularInfo
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun uploadSnapshot(snapshot: MonitoringSnapshot): Boolean {
        return try {
            val token = authManager.getToken() ?: return false
            val apiRequest = mapSnapshotToApiRequest(snapshot)
            val response = apiService?.uploadSnapshot("Bearer $token", apiRequest)
            response != null && response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun mapSnapshotToApiRequest(snapshot: MonitoringSnapshot): PolarisApiRequest {
        // Parse signal and cell info to extract relevant data
        val signalStrength = extractSignalStrength(snapshot.signalInfo)
        val networkType = extractNetworkType(snapshot.cellInfo)
        val cellData = parseCellularInfo(snapshot.cellInfo)

        val apiRequest = PolarisApiRequest(
            timestamp = snapshot.timestamp,
            n = cellData.n ?: 1,
            s = cellData.s ?: 1,
            t = cellData.t ?: 1,
            band = cellData.band ?: 3,
            ulArfcn = cellData.ulArfcn ?: 1800,
            dlArfcn = cellData.dlArfcn ?: 1800,
            code = cellData.code ?: 100,
            ulBw = cellData.ulBw ?: 10.5,
            dlBw = cellData.dlBw ?: 20.2,
            plmnId = cellData.plmnId ?: 12345,
            tacOrLac = cellData.tacOrLac ?: 54321,
            rac = cellData.rac ?: 1,
            longCellId = cellData.longCellId ?: 123,
            siteId = cellData.siteId ?: 456,
            cellId = cellData.cellId ?: 789,
            latitude = snapshot.latitude ?: 0.0,
            longitude = snapshot.longitude ?: 0.0,
            signalStrength = signalStrength,
            networkType = networkType,
            downloadSpeed = 0.0,
            uploadSpeed = 0.0,
            pingTime = 0
        )

        val gson = Gson()
        Log.d("SnapshotMapping", "Original snapshot: ${gson.toJson(snapshot)}")
        Log.d("SnapshotMapping", "Mapped API request: ${gson.toJson(apiRequest)}")

        return apiRequest
    }

    private fun extractSignalStrength(signalInfo: String): Int {
        return try {
            // Look for patterns like "Signal: -70 dBm" or "RSRP: -85"
            val patterns = listOf(
                """Signal:\s*(-?\d+)""".toRegex(),
                """RSRP:\s*(-?\d+)""".toRegex(),
                """dBm:\s*(-?\d+)""".toRegex(),
                """(-?\d+)\s*dBm""".toRegex()
            )

            for (pattern in patterns) {
                val match = pattern.find(signalInfo)
                if (match != null) {
                    return match.groupValues[1].toInt()
                }
            }
            -70 // Default value
        } catch (e: Exception) {
            -70
        }
    }

    private fun extractNetworkType(cellInfo: String): String {
        return when {
            cellInfo.contains("LTE", ignoreCase = true) -> "LTE"
            cellInfo.contains("5G", ignoreCase = true) -> "5G"
            cellInfo.contains("GSM", ignoreCase = true) -> "GSM"
            cellInfo.contains("WCDMA", ignoreCase = true) -> "WCDMA"
            cellInfo.contains("CDMA", ignoreCase = true) -> "CDMA"
            else -> "UNKNOWN"
        }
    }

    private fun parseCellularInfo(cellInfo: String): CellData {
        // This is a simplified parser - you might need to adjust based on your actual cellInfo format
        return try {
            CellData(
                n = extractIntValue(cellInfo, "N"),
                s = extractIntValue(cellInfo, "S"),
                t = extractIntValue(cellInfo, "T"),
                band = extractIntValue(cellInfo, "Band"),
                ulArfcn = extractIntValue(cellInfo, "UARFCN"),
                dlArfcn = extractIntValue(cellInfo, "DARFCN"),
                code = extractIntValue(cellInfo, "Code"),
                ulBw = extractDoubleValue(cellInfo, "UL_BW"),
                dlBw = extractDoubleValue(cellInfo, "DL_BW"),
                plmnId = extractIntValue(cellInfo, "PLMN"),
                tacOrLac = extractIntValue(cellInfo, "TAC") ?: extractIntValue(cellInfo, "LAC"),
                rac = extractIntValue(cellInfo, "RAC"),
                longCellId = extractLongValue(cellInfo, "CellId"),
                siteId = extractIntValue(cellInfo, "SiteId"),
                cellId = extractIntValue(cellInfo, "Cell")
            )
        } catch (e: Exception) {
            CellData() // Return default values
        }
    }

    private fun extractIntValue(text: String, key: String): Int? {
        return try {
            val pattern = """$key[:\s]*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            pattern.find(text)?.groupValues?.get(1)?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractLongValue(text: String, key: String): Long? {
        return try {
            val pattern = """$key[:\s]*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            pattern.find(text)?.groupValues?.get(1)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDoubleValue(text: String, key: String): Double? {
        return try {
            val pattern = """$key[:\s]*([\d.]+)""".toRegex(RegexOption.IGNORE_CASE)
            pattern.find(text)?.groupValues?.get(1)?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Polaris Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring service for Polaris app"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val stopIntent = Intent(this, BackgroundMonitoringService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Polaris Monitoring Active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // You'll need to add this icon
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun hasStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
}
    data class CellData(
        val n: Int? = null,
        val s: Int? = null,
        val t: Int? = null,
        val band: Int? = null,
        val ulArfcn: Int? = null,
        val dlArfcn: Int? = null,
        val code: Int? = null,
        val ulBw: Double? = null,
        val dlBw: Double? = null,
        val plmnId: Int? = null,
        val tacOrLac: Int? = null,
        val rac: Int? = null,
        val longCellId: Long? = null,
        val siteId: Int? = null,
        val cellId: Int? = null
    )
}