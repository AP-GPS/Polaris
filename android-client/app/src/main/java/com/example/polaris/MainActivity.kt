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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.polaris.data.repo.SnapshotRepository
import com.example.polaris.ui.theme.PolarisTheme
import com.example.polaris.utils.*
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
            PolarisTheme { Surface(modifier = Modifier.fillMaxSize()) { PolarisHomeScreen() } }
        }
    }
}

@Composable
fun PolarisHomeScreen() {
    val context = LocalContext.current
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

    // Notification state
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationType by remember { mutableStateOf("success") } // success, error, info

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
    suspend fun saveSnapshot(): Boolean {
        return if (autoSaveSnapshots && phoneStatePermissionGranted) {
            try {
                val coordinates = extractCoordinates(locationText)
                val snapshot = MonitoringSnapshot(
                    timestamp = System.currentTimeMillis(),
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                    signalInfo = signalInfoState.value,
                    cellInfo = infoText
                )
                repository.insert(snapshot)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
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

    // Periodic updates coroutine
    LaunchedEffect(isPeriodicUpdatesEnabled, updateInterval) {
        if (isPeriodicUpdatesEnabled) {
            showNotificationMessage("Periodic updates started", "success")
            while (isPeriodicUpdatesEnabled) {
                delay(updateInterval * 1000L) // Convert seconds to milliseconds

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

                // Signal strength is updated automatically via flow
                updateResults.add("Signal")
                updateCount++

                // Save snapshot if auto-save is enabled
                val snapshotSaved = saveSnapshot()
                if (snapshotSaved) {
                    updateResults.add("Snapshot")
                    updateCount++
                }

                // Update last update time
                lastUpdateTime = System.currentTimeMillis()

                // Show notification
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

            // Periodic Updates Control Panel
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
                        text = "Periodic Updates",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Auto Updates")
                        Switch(
                            checked = isPeriodicUpdatesEnabled,
                            onCheckedChange = { isPeriodicUpdatesEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Save Snapshots")
                        Switch(
                            checked = autoSaveSnapshots,
                            onCheckedChange = { autoSaveSnapshots = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Update Interval (seconds)")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (updateInterval > 10) updateInterval -= 10 },
                                enabled = !isPeriodicUpdatesEnabled
                            ) { Text("-") }

                            Text(
                                text = updateInterval.toString(),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Button(
                                onClick = { if (updateInterval < 300) updateInterval += 10 },
                                enabled = !isPeriodicUpdatesEnabled
                            ) { Text("+") }
                        }
                    }

                    if (lastUpdateTime > 0) {
                        Text(
                            text = "Last update: ${formatTimestamp(lastUpdateTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    enabled = !isLoading && !isPeriodicUpdatesEnabled
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
                    enabled = !isPeriodicUpdatesEnabled
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

            // Manual Save Snapshot Button
            Button(
                onClick = {
                    scope.launch {
                        val success = saveSnapshot()
                        showNotificationMessage(
                            if (success) "Snapshot saved manually" else "Failed to save snapshot",
                            if (success) "success" else "error"
                        )
                    }
                }
            ) {
                Text("Save Snapshot Manually")
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

            // Add bottom padding for better scrolling
            Spacer(modifier = Modifier.height(16.dp))
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