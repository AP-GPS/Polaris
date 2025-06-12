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
import com.example.polaris.ui.theme.PolarisTheme
import com.example.polaris.utils.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

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
    var retryCount by remember { mutableIntStateOf(0) }
    var isGpsEnabled by remember { mutableStateOf(isGpsEnabled(context)) }
    var infoText by remember { mutableStateOf("Requesting phone state permission...") }
    var phoneStatePermissionGranted by remember { mutableStateOf(false) }
    val signalInfoState = remember { mutableStateOf("Listening...") }

    // Request permissions first
    RequestPhoneStatePermission(
            onGranted = {
                phoneStatePermissionGranted = true
                LaunchedEffect(Unit) {
                    val cellData = getCellularInfo(context)
                    infoText = cellData
                }
            },
            onDenied = {
                phoneStatePermissionGranted = false
                infoText = "Permission denied. Please grant phone state permission in settings."
            }
    )

    RequestLocationPermission {
        LaunchedEffect(retryCount) {
            isLoading = true
            try {
                if (!isGpsEnabled) {
                    locationText = "GPS is disabled"
                    return@LaunchedEffect
                }

                val loc = getLastKnownLocation(context)
                locationText =
                        if (loc != null) {
                            "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                        } else {
                            "Location not available"
                        }
            } catch (e: Exception) {
                locationText = "Error getting location"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        SignalStrengthCollector.startListening(context)
        SignalStrengthCollector.signalInfo.collectLatest { signal ->
            signalInfoState.value = signal
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add top padding
            Spacer(modifier = Modifier.height(32.dp))

            // Location section
            if (isLoading) {
                CircularProgressIndicator()
            }

            Text(locationText)

            if (!isGpsEnabled) {
                Button(
                        onClick = {
                            openGpsSettings(context)
                            isGpsEnabled = isGpsEnabled(context)
                        }
                ) { Text("Enable GPS") }
            } else {
                Button(onClick = { retryCount++ }, enabled = !isLoading) {
                    Text("Refresh Location")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Cellular info section
            Text(text = "Cellular Information", style = MaterialTheme.typography.titleMedium)

            // Scrollable cellular info box
            Box(
                    modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
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
                            // This will trigger the permission request again
                            phoneStatePermissionGranted = false
                        }
                ) { Text("Request Phone State Permission") }
            }

            Text(text = "Signal Strength", style = MaterialTheme.typography.titleMedium)

            // Scrollable signal info box (assuming it's below cellular info)
            Box(
                    modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
            ) {
                Column(
                        modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                ) {
                    Text(text = signalInfoState.value)
                }
            }
        }
    }
}
