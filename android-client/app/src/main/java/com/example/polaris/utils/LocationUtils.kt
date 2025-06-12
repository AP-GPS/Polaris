package com.example.polaris.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "LocationUtils"

fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

fun openGpsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

@SuppressLint("MissingPermission")
suspend fun getLastKnownLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) {
        Log.d(TAG, "Location permission not granted")
        return null
    }

    if (!isGpsEnabled(context)) {
        Log.d(TAG, "GPS is not enabled")
        return null
    }

    // Add a small delay to ensure location services are initialized
    delay(1000)

    val fusedClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)
    val cancellationToken = CancellationTokenSource()

    return try {
        suspendCancellableCoroutine { cont ->
            Log.d(TAG, "Requesting current location...")
            fusedClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            Log.d(
                                    TAG,
                                    "Location obtained: ${location.latitude}, ${location.longitude}"
                            )
                            cont.resume(location)
                        } else {
                            Log.d(TAG, "Location is null, trying last known location")
                            // Fallback to last known location
                            fusedClient.lastLocation
                                    .addOnSuccessListener { lastLocation ->
                                        if (lastLocation != null) {
                                            Log.d(
                                                    TAG,
                                                    "Last known location obtained: ${lastLocation.latitude}, ${lastLocation.longitude}"
                                            )
                                            cont.resume(lastLocation)
                                        } else {
                                            Log.d(TAG, "Last known location is null")
                                            cont.resume(null)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error getting last known location", e)
                                        cont.resume(null)
                                    }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting current location", e)
                        cont.resume(null)
                    }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Exception while getting location", e)
        null
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun RequestLocationPermission(
        onPermissionGranted: @Composable () -> Unit,
) {
    val context = rememberUpdatedState(LocalContext.current).value
    var permissionRequested by remember { mutableStateOf(false) }
    var isGranted by remember { mutableStateOf(false) }

    val launcher =
            rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val fineLocationGranted =
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocationGranted =
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                if (fineLocationGranted || coarseLocationGranted) {
                    Log.d(TAG, "Location permission granted")
                    isGranted = true
                } else {
                    Log.d(TAG, "Location permission denied")
                    Toast.makeText(
                                    context,
                                    "Location permission denied. Some features may not work.",
                                    Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }

    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            permissionRequested = true
            launcher.launch(
                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
            )
        }
    }

    if (isGranted) {
        onPermissionGranted()
    }
}
