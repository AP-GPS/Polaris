package com.example.polaris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.polaris.ui.theme.PolarisTheme
import com.example.polaris.utils.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PolarisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PolarisHomeScreen()
                }
            }
        }
    }
}

@Composable
fun PolarisHomeScreen() {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Requesting location...") }
    var isLoading by remember { mutableStateOf(true) }
    var retryCount by remember { mutableStateOf(0) }
    var isGpsEnabled by remember { mutableStateOf(isGpsEnabled(context)) }

    RequestLocationPermission {
        LaunchedEffect(retryCount) {
            isLoading = true
            try {
                if (!isGpsEnabled) {
                    locationText = "GPS is disabled"
                    return@LaunchedEffect
                }
                
                val loc = getLastKnownLocation(context)
                locationText = if (loc != null) {
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(locationText)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isGpsEnabled) {
                Button(
                    onClick = {
                        openGpsSettings(context)
                        isGpsEnabled = isGpsEnabled(context)
                    }
                ) {
                    Text("Enable GPS")
                }
            } else {
                Button(
                    onClick = { retryCount++ },
                    enabled = !isLoading
                ) {
                    Text("Refresh Location")
                }
            }
        }
    }
}
