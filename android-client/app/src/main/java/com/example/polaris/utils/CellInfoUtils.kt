package com.example.polaris.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RequestPhoneStatePermission(onGranted: @Composable () -> Unit, onDenied: () -> Unit = {}) {
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    // Use from Composable context
    val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted
                ->
                if (granted) {
                    permissionGranted = true
                } else {
                    onDenied()
                }
            }

    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            permissionRequested = true
            launcher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    if (permissionGranted) {
        onGranted()
    }
}

private fun hasPhoneStatePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}

private fun getMccMnc(identity: CellIdentityLte): Pair<String, String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Pair(identity.mccString ?: "unknown", identity.mncString ?: "unknown")
    } else {
        @Suppress("DEPRECATION") Pair(identity.mcc.toString(), identity.mnc.toString())
    }
}

private fun getMccMnc(identity: CellIdentityGsm): Pair<String, String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Pair(identity.mccString ?: "unknown", identity.mncString ?: "unknown")
    } else {
        @Suppress("DEPRECATION") Pair(identity.mcc.toString(), identity.mnc.toString())
    }
}

private fun getMccMnc(identity: CellIdentityWcdma): Pair<String, String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Pair(identity.mccString ?: "unknown", identity.mncString ?: "unknown")
    } else {
        @Suppress("DEPRECATION") Pair(identity.mcc.toString(), identity.mnc.toString())
    }
}

suspend fun getCellularInfo(context: Context): String =
        withContext(Dispatchers.IO) {
            if (!hasPhoneStatePermission(context)) {
                return@withContext "Missing READ_PHONE_STATE permission"
            }

            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val builder = StringBuilder()
            try {
                val cellInfoList = manager.allCellInfo
                if (cellInfoList.isNullOrEmpty()) return@withContext "No cell info available"

                // First, find the primary (connected) cell
                val primaryCell = cellInfoList.find { it.isRegistered }

                // Add primary cell info first
                primaryCell?.let { cell ->
                    when (cell) {
                        is CellInfoLte -> {
                            val identity = cell.cellIdentity
                            val (mcc, mnc) = getMccMnc(identity)
                            builder.appendLine("Primary LTE Cell:")
                            builder.appendLine("  Cell ID: ${identity.ci}")
                            builder.appendLine("  TAC: ${identity.tac}")
                            builder.appendLine("  PCI: ${identity.pci}")
                            builder.appendLine("  EARFCN: ${identity.earfcn}")
                            builder.appendLine("  MCC: $mcc")
                            builder.appendLine("  MNC: $mnc")
                            builder.appendLine(
                                    "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                            )
                        }
                        is CellInfoGsm -> {
                            val identity = cell.cellIdentity
                            val (mcc, mnc) = getMccMnc(identity)
                            builder.appendLine("Primary GSM Cell:")
                            builder.appendLine("  Cell ID: ${identity.cid}")
                            builder.appendLine("  LAC: ${identity.lac}")
                            builder.appendLine("  ARFCN: ${identity.arfcn}")
                            builder.appendLine("  MCC: $mcc")
                            builder.appendLine("  MNC: $mnc")
                            builder.appendLine(
                                    "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                            )
                        }
                        is CellInfoWcdma -> {
                            val identity = cell.cellIdentity
                            val (mcc, mnc) = getMccMnc(identity)
                            builder.appendLine("Primary WCDMA Cell:")
                            builder.appendLine("  Cell ID: ${identity.cid}")
                            builder.appendLine("  LAC: ${identity.lac}")
                            builder.appendLine("  UARFCN: ${identity.uarfcn}")
                            builder.appendLine("  MCC: $mcc")
                            builder.appendLine("  MNC: $mnc")
                            builder.appendLine(
                                    "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                            )
                        }
                        else -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell is CellInfoNr
                            ) {
                                val identity = cell.cellIdentity as CellIdentityNr
                                builder.appendLine("Primary 5G NR Cell:")
                                builder.appendLine("  NCI: ${identity.nci}")
                                builder.appendLine("  TAC: ${identity.tac}")
                                builder.appendLine("  PCI: ${identity.pci}")
                                builder.appendLine("  NRARFCN: ${identity.nrarfcn}")
                                builder.appendLine("  MCC: ${identity.mccString}")
                                builder.appendLine("  MNC: ${identity.mncString}")
                                builder.appendLine(
                                        "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                                )
                            }
                        }
                    }
                    builder.appendLine("---")
                }

                // Then add neighboring cells
                val neighboringCells = cellInfoList.filter { !it.isRegistered }
                if (neighboringCells.isNotEmpty()) {
                    builder.appendLine("Neighboring Cells:")
                    for (cell in neighboringCells) {
                        when (cell) {
                            is CellInfoLte -> {
                                val identity = cell.cellIdentity
                                val (mcc, mnc) = getMccMnc(identity)
                                builder.appendLine("LTE:")
                                builder.appendLine("  Cell ID: N/A")
                                builder.appendLine("  TAC: N/A")
                                builder.appendLine("  PCI: ${identity.pci}")
                                builder.appendLine("  EARFCN: ${identity.earfcn}")
                                builder.appendLine("  MCC: N/A")
                                builder.appendLine("  MNC: N/A")
                                builder.appendLine(
                                        "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                                )
                            }
                            is CellInfoGsm -> {
                                val identity = cell.cellIdentity
                                val (mcc, mnc) = getMccMnc(identity)
                                builder.appendLine("GSM:")
                                builder.appendLine("  Cell ID: N/A")
                                builder.appendLine("  LAC: N/A")
                                builder.appendLine("  ARFCN: ${identity.arfcn}")
                                builder.appendLine("  MCC: N/A")
                                builder.appendLine("  MNC: N/A")
                                builder.appendLine(
                                        "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                                )
                            }
                            is CellInfoWcdma -> {
                                val identity = cell.cellIdentity
                                val (mcc, mnc) = getMccMnc(identity)
                                builder.appendLine("WCDMA:")
                                builder.appendLine("  Cell ID: N/A")
                                builder.appendLine("  LAC: N/A")
                                builder.appendLine("  UARFCN: ${identity.uarfcn}")
                                builder.appendLine("  MCC: N/A")
                                builder.appendLine("  MNC: N/A")
                                builder.appendLine(
                                        "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                                )
                            }
                            else -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                                cell is CellInfoNr
                                ) {
                                    val identity = cell.cellIdentity as CellIdentityNr
                                    builder.appendLine("5G NR:")
                                    builder.appendLine("  NCI: N/A")
                                    builder.appendLine("  TAC: N/A")
                                    builder.appendLine("  PCI: ${identity.pci}")
                                    builder.appendLine("  NRARFCN: ${identity.nrarfcn}")
                                    builder.appendLine("  MCC: N/A")
                                    builder.appendLine("  MNC: N/A")
                                    builder.appendLine(
                                            "  Signal Strength: ${cell.cellSignalStrength.level} dBm"
                                    )
                                }
                            }
                        }
                        builder.appendLine("---")
                    }
                }
            } catch (e: Exception) {
                return@withContext "Error: ${e.message}"
            }
            builder.toString()
        }
