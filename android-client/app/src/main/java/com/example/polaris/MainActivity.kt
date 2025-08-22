package com.example.polaris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.polaris.data.repo.SnapshotRepository
import com.example.polaris.ui.theme.PolarisTheme
import com.example.polaris.auth.LoginDialog
import com.example.polaris.auth.AuthManager
import com.example.polaris.auth.LoginRequest
import com.example.polaris.utils.*
import com.example.polaris.service.BackgroundMonitoringService
import com.example.polaris.service.ApiService
import com.example.polaris.service.PolarisApiRequest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.polaris.data.entity.MonitoringSnapshot
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PolarisApp()
                }
            }
        }
    }
}

@Composable
fun PolarisApp() {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val authState by authManager.authState.collectAsState()
    val scope = rememberCoroutineScope()

    // Handle login
    fun handleLogin(username: String, password: String) {
        scope.launch {
            try {
                authManager.setLoading(true)

                val loginApiService = ApiService.createForLogin()
                val response = loginApiService.login(LoginRequest(username, password))

                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    if (loginResponse.token != null) {
                        authManager.login(loginResponse.token)
                    } else {
                        authManager.setError("Invalid response from server")
                    }
                } else {
                    val errorMessage = "Login failed"
                    authManager.setError(errorMessage)
                }
            } catch (e: Exception) {
                authManager.setError("Network error: ${e.message}")
            }
        }
    }

    if (!authState.isAuthenticated) {
        LoginDialog(
            authState = authState,
            onLogin = { username, password ->
                handleLogin(username, password)
            }
        )
    } else {
        PolarisHomeScreen(
            onLogout = {
                authManager.logout()
            }
        )
    }
}

@Composable
fun PolarisHomeScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    val currentUser = authManager.getCurrentUser()

    var locationText by remember { mutableStateOf("Requesting location...") }
    var isLoading by remember { mutableStateOf(true) }
    var isGpsEnabled by remember { mutableStateOf(isGpsEnabled(context)) }
    var infoText by remember { mutableStateOf("Requesting phone state permission...") }
    var phoneStatePermissionGranted by remember { mutableStateOf(false) }
    val signalInfoState = remember { mutableStateOf("Listening...") }
    val repository: SnapshotRepository = remember { SnapshotRepository(context) }
    val scope = rememberCoroutineScope()
    val snapshots by repository.getAll().collectAsState(initial = emptyList())

    // Periodic update settings
    var isPeriodicUpdatesEnabled by remember { mutableStateOf(false) }
    var updateInterval by remember { mutableIntStateOf(30) } // seconds
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    var autoSaveSnapshots by remember { mutableStateOf(false) }

    // Background service settings
    var isBackgroundServiceEnabled by remember { mutableStateOf(false) }
    var autoUploadSnapshots by remember { mutableStateOf(false) }
    var apiUrl by remember { mutableStateOf("https://tehran.nazareto.ir") }

    // Notification state
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationType by remember { mutableStateOf("success") } // success, error, info

    // Create API service with authentication
    val apiService = remember(authManager.getToken()) {
        authManager.getToken()?.let { ApiService.create(it) }
    }

    // Helper function to extract coordinates safely
    fun extractCoordinates(locationText: String): Pair<Double?, Double?> {
        return try {
            if (locationText.contains("Lat:") && locationText.contains("Lon:")) {
                val lat = locationText.substringAfter("Lat: ").substringBefore(",").trim().toDoubleOrNull()
                val lon = locationText.substringAfter("Lon: ").trim().toDoubleOrNull()
                Pair(lat, lon)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    // Helper function to format timestamp
    fun formatTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    // Function to show notification
    fun showNotificationMessage(message: String, type: String = "info") {
        notificationMessage = message
        notificationType = type
        showNotification = true
    }

    // Function to update location
    suspend fun updateLocation(): Boolean {
        return try {
            isGpsEnabled = isGpsEnabled(context)
            if (!isGpsEnabled) {
                locationText = "GPS is disabled"
                return false
            }

            val loc = getLastKnownLocation(context)
            locationText = if (loc != null) {
                "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
            } else {
                "Location not available"
            }
            true
        } catch (e: Exception) {
            locationText = "Error getting location: ${e.message}"
            false
        }
    }

    // Function to update cellular info
    suspend fun updateCellularInfo(): Boolean {
        return if (phoneStatePermissionGranted) {
            try {
                val cellData = getCellularInfo(context)
                infoText = cellData
                true
            } catch (e: Exception) {
                infoText = "Error getting cellular info: ${e.message}"
                false
            }
        } else {
            false
        }
    }

    // Function to save snapshot automatically
    suspend fun saveSnapshot(): MonitoringSnapshot? {
        return if (phoneStatePermissionGranted) {
            try {
                val coordinates = extractCoordinates(locationText)
                val snapshot = MonitoringSnapshot(
                    timestamp = System.currentTimeMillis(),
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                    signalInfo = signalInfoState.value,
                    cellInfo = infoText
                )
                if (autoSaveSnapshots) {
                    repository.insert(snapshot)
                }
                snapshot
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Function to upload snapshot to API
    suspend fun uploadSnapshot(snapshot: MonitoringSnapshot): Boolean {
        return try {
            val token = authManager.getToken() ?: return false
            if (apiService == null) return false

            val apiRequest = mapSnapshotToApiRequest(snapshot)
            val response = apiService.uploadSnapshot(token, apiRequest)

            if (response.code() == 401) {
                // Token expired, logout user
                authManager.logout()
                return false
            }

            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // Function to map snapshot to API request format
    fun mapSnapshotToApiRequest(snapshot: MonitoringSnapshot): PolarisApiRequest {
        // Extract signal strength from signal info
        val signalStrength = extractSignalStrength(snapshot.signalInfo)
        val networkType = extractNetworkType(snapshot.cellInfo)

        return PolarisApiRequest(
            timestamp = snapshot.timestamp,
            n = 1, // Default values - you may want to extract these from cellInfo
            s = 1,
            t = 1,
            band = 3,
            ulArfcn = 1800,
            dlArfcn = 1800,
            code = 100,
            ulBw = 10.5,
            dlBw = 20.2,
            plmnId = 12345,
            tacOrLac = 54321,
            rac = 1,
            longCellId = 123,
            siteId = 456,
            cellId = 789,
            latitude = snapshot.latitude ?: 0.0,
            longitude = snapshot.longitude ?: 0.0,
            signalStrength = signalStrength,
            networkType = networkType,
            downloadSpeed = 0.0, // These would need to be measured separately
            uploadSpeed = 0.0,
            pingTime = 0
        )
    }

    fun extractSignalStrength(signalInfo: String): Int {
        return try {
            val patterns = listOf(
                """Signal:\s*(-?\d+)""".toRegex(),
                """RSRP:\s*(-?\d+)""".toRegex(),
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

    fun extractNetworkType(cellInfo: String): String {
        return when {
            cellInfo.contains("LTE", ignoreCase = true) -> "LTE"
            cellInfo.contains("5G", ignoreCase = true) -> "5G"
            cellInfo.contains("GSM", ignoreCase = true) -> "GSM"
            cellInfo.contains("WCDMA", ignoreCase = true) -> "WCDMA"
            else -> "UNKNOWN"
        }
    }

    // Request permissions first
    RequestPhoneStatePermission(
        onGranted = {
            phoneStatePermissionGranted = true
            LaunchedEffect(Unit) {
                updateCellularInfo()
            }
        },
        onDenied = {
            phoneStatePermissionGranted = false
            infoText = "Permission denied. Please grant phone state permission in settings."
        }
    )

    RequestLocationPermission {
        LaunchedEffect(Unit) {
            updateLocation()
            isLoading = false
        }
    }

    // Signal strength listener
    LaunchedEffect(Unit) {
        SignalStrengthCollector.startListening(context)
        SignalStrengthCollector.signalInfo.collectLatest { signal ->
            signalInfoState.value = signal
        }
    }

    // Periodic updates coroutine (for foreground operation)
    LaunchedEffect(isPeriodicUpdatesEnabled, updateInterval) {
        if (isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled) {
            showNotificationMessage("Periodic updates started", "success")
            while (isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled) {
                delay(updateInterval * 1000L)

                var updateCount = 0
                val updateResults = mutableListOf<String>()

                // Update location
                if (updateLocation()) {
                    updateCount++
                    updateResults.add("Location")
                }

                // Update cellular info
                if (updateCellularInfo()) {
                    updateCount++
                    updateResults.add("Cellular")
                }

                updateResults.add("Signal")
                updateCount++

                // Save and upload snapshot
                val snapshot = saveSnapshot()
                if (snapshot != null) {
                    updateResults.add("Snapshot")
                    updateCount++

                    if (autoUploadSnapshots) {
                        val uploadSuccess = uploadSnapshot(snapshot)
                        if (uploadSuccess) {
                            updateResults.add("Upload")
                            updateCount++
                        }
                    }
                }

                lastUpdateTime = System.currentTimeMillis()

                val message = if (updateCount > 0) {
                    "Updated: ${updateResults.joinToString(", ")}"
                } else {
                    "Update completed with some issues"
                }

                showNotificationMessage(
                    message,
                    if (updateCount == updateResults.size) "success" else "error"
                )
            }
            showNotificationMessage("Periodic updates stopped", "info")
        }
    }

    // Auto-hide notification after 3 seconds
    LaunchedEffect(showNotification) {
        if (showNotification) {
            delay(3000)
            showNotification = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add top padding
            Spacer(modifier = Modifier.height(32.dp))

            // User info and logout section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentUser?.email != null) {
                            Text(
                                text = currentUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    TextButton(
                        onClick = onLogout
                    ) {
                        Text("Logout")
                    }
                }
            }

            // Background Service Control Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Background Service",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Background Monitoring")
                        Switch(
                            checked = isBackgroundServiceEnabled,
                            onCheckedChange = { enabled ->
                                isBackgroundServiceEnabled = enabled
                                if (enabled) {
                                    // Stop foreground periodic updates
                                    isPeriodicUpdatesEnabled = false
                                    // Start background service
                                    BackgroundMonitoringService.startMonitoring(
                                        context = context,
                                        updateInterval = updateInterval.toLong(),
                                        autoSave = autoSaveSnapshots,
                                        autoUpload = autoUploadSnapshots
                                    )
                                    showNotificationMessage("Background service started", "success")
                                } else {
                                    // Stop background service
                                    BackgroundMonitoringService.stopMonitoring(context)
                                    showNotificationMessage("Background service stopped", "info")
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Upload to API")
                        Switch(
                            checked = autoUploadSnapshots,
                            onCheckedChange = {
                                autoUploadSnapshots = it
                                if (isBackgroundServiceEnabled) {
                                    BackgroundMonitoringService.updateSettings(
                                        context = context,
                                        updateInterval = updateInterval.toLong(),
                                        autoSave = autoSaveSnapshots,
                                        autoUpload = autoUploadSnapshots
                                    )
                                }
                            }
                        )
                    }

//                    OutlinedTextField(
//                        value = apiUrl,
//                        onValueChange = { apiUrl = it },
//                        label = { Text("API Base URL") },
//                        modifier = Modifier.fillMaxWidth(),
//                        enabled = !isBackgroundServiceEnabled
//                    )
                }
            }

            // Periodic Updates Control Panel (for foreground operation)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBackgroundServiceEnabled)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Foreground Updates",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isBackgroundServiceEnabled)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (isBackgroundServiceEnabled) {
                        Text(
                            text = "Background service is active. Foreground updates are disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Enable Auto Updates",
                            color = if (isBackgroundServiceEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Switch(
                            checked = isPeriodicUpdatesEnabled,
                            onCheckedChange = { isPeriodicUpdatesEnabled = it },
                            enabled = !isBackgroundServiceEnabled
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Auto Save Snapshots",
                            color = if (isBackgroundServiceEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Switch(
                            checked = autoSaveSnapshots,
                            onCheckedChange = {
                                autoSaveSnapshots = it
                                if (isBackgroundServiceEnabled) {
                                    BackgroundMonitoringService.updateSettings(
                                        context = context,
                                        updateInterval = updateInterval.toLong(),
                                        autoSave = autoSaveSnapshots,
                                        autoUpload = autoUploadSnapshots
                                    )
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Update Interval (seconds)",
                            color = if (isBackgroundServiceEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (updateInterval > 10) {
                                        updateInterval -= 10
                                        if (isBackgroundServiceEnabled) {
                                            BackgroundMonitoringService.updateSettings(
                                                context = context,
                                                updateInterval = updateInterval.toLong(),
                                                autoSave = autoSaveSnapshots,
                                                autoUpload = autoUploadSnapshots
                                            )
                                        }
                                    }
                                },
                                enabled = !isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled
                            ) { Text("-") }

                            Text(
                                text = updateInterval.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = if (isBackgroundServiceEnabled)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Button(
                                onClick = {
                                    if (updateInterval < 300) {
                                        updateInterval += 10
                                        if (isBackgroundServiceEnabled) {
                                            BackgroundMonitoringService.updateSettings(
                                                context = context,
                                                updateInterval = updateInterval.toLong(),
                                                autoSave = autoSaveSnapshots,
                                                autoUpload = autoUploadSnapshots
                                            )
                                        }
                                    }
                                },
                                enabled = !isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled
                            ) { Text("+") }
                        }
                    }

                    if (lastUpdateTime > 0) {
                        Text(
                            text = "Last update: ${formatTimestamp(lastUpdateTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBackgroundServiceEnabled)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            // Location section
            Text(
                text = "Location Information",
                style = MaterialTheme.typography.titleMedium
            )

            if (isLoading) {
                CircularProgressIndicator()
            }

            Text(locationText)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isGpsEnabled) {
                    Button(
                        onClick = {
                            openGpsSettings(context)
                            isGpsEnabled = isGpsEnabled(context)
                        }
                    ) { Text("Enable GPS") }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val success = updateLocation()
                            showNotificationMessage(
                                if (success) "Location updated manually" else "Failed to update location",
                                if (success) "success" else "error"
                            )
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && !isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled
                ) {
                    Text("Manual Refresh")
                }
            }

            HorizontalDivider()

            // Cellular info section
            Text(text = "Cellular Information", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(text = infoText)
                }
            }

            if (!phoneStatePermissionGranted) {
                Button(
                    onClick = {
                        phoneStatePermissionGranted = false
                    }
                ) { Text("Request Phone State Permission") }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            val success = updateCellularInfo()
                            showNotificationMessage(
                                if (success) "Cellular info updated manually" else "Failed to update cellular info",
                                if (success) "success" else "error"
                            )
                        }
                    },
                    enabled = !isPeriodicUpdatesEnabled && !isBackgroundServiceEnabled
                ) {
                    Text("Manual Refresh Cellular")
                }
            }

            Text(text = "Signal Strength", style = MaterialTheme.typography.titleMedium)

            Box(
                modifier = Modifier
                    .height(150.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(text = signalInfoState.value)
                }
            }

            // Manual Save and Upload Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val snapshot = saveSnapshot()
                            showNotificationMessage(
                                if (snapshot != null) "Snapshot saved manually" else "Failed to save snapshot",
                                if (snapshot != null) "success" else "error"
                            )
                        }
                    }
                ) {
                    Text("Save Snapshot")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val snapshot = saveSnapshot()
                            if (snapshot != null) {
                                val uploadSuccess = uploadSnapshot(snapshot)
                                showNotificationMessage(
                                    if (uploadSuccess) "Snapshot uploaded successfully" else "Failed to upload snapshot",
                                    if (uploadSuccess) "success" else "error"
                                )
                            } else {
                                showNotificationMessage("Failed to create snapshot", "error")
                            }
                        }
                    }
                ) {
                    Text("Upload Snapshot")
                }
            }

            HorizontalDivider()

            // Saved Snapshots Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Snapshots",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "(${snapshots.size} total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(8.dp)
            ) {
                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No snapshots saved yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snapshots.take(5).forEach { snap ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "⏱ ${formatTimestamp(snap.timestamp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "📍 ${snap.latitude?.let { "%.6f".format(it) } ?: "N/A"}, ${snap.longitude?.let { "%.6f".format(it) } ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "📶 ${snap.signalInfo.take(50)}${if (snap.signalInfo.length > 50) "..." else ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "📡 ${snap.cellInfo.take(50)}${if (snap.cellInfo.length > 50) "..." else ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }

        // Notification overlay
        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable { showNotification = false },
                colors = CardDefaults.cardColors(
                    containerColor = when (notificationType) {
                        "success" -> Color(0xFF4CAF50)
                        "error" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.primary
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (notificationType) {
                            "success" -> "✓"
                            "error" -> "✗"
                            else -> "ℹ"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = notificationMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "×",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { showNotification = false }
                    )
                }
            }
        }
    }
}