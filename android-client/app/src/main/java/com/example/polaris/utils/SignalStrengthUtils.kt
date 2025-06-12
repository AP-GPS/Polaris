package com.example.polaris.utils

import android.content.Context
import android.os.Build

import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import kotlin.math.log

object SignalStrengthCollector {
    private val _signalInfo = MutableStateFlow("Awaiting signal strength...")
    val signalInfo: StateFlow<String> = _signalInfo

    private var listener: Any? = null
    private var telephonyCallback: TelephonyCallback? = null

    fun startListening(context: Context) {
        if (listener != null || telephonyCallback != null) return // already listening

        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use TelephonyCallback for Android 12+ (API 31+)
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    // Use applicationContext to avoid memory leaks
                    _signalInfo.value = parseSignalStrength(signalStrength, context.applicationContext)
                }
            }

            val executor: Executor = context.mainExecutor
            telephonyManager.registerTelephonyCallback(executor, telephonyCallback!!)
        } else {
            // Use PhoneStateListener for older versions
            @Suppress("DEPRECATION")
            listener = object : android.telephony.PhoneStateListener() {
                @Suppress("DEPRECATION")
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    super.onSignalStrengthsChanged(signalStrength)
                    // Use applicationContext to avoid memory leaks
                    _signalInfo.value =
                        signalStrength?.let { parseSignalStrength(it, context.applicationContext) }
                            ?: "Signal unavailable"
                }
            }

            @Suppress("DEPRECATION")
            telephonyManager.listen(listener as android.telephony.PhoneStateListener, android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
        }
    }

    fun stopListening(context: Context) {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
                telephonyCallback = null
            }
        } else {
            listener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it as android.telephony.PhoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)
                listener = null
            }
        }
    }

    // Public function to get all cell info on demand
    fun getAllCellInfo(context: Context): String {
        return getAllCellInfoInternal(context.applicationContext)
    }

    private fun parseSignalStrength(signalStrength: SignalStrength, context: Context): String {
        return try {
            val info = StringBuilder()
            info.appendLine("=== SERVING CELL INFO ===")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cellSignalList = signalStrength.cellSignalStrengths

                if (cellSignalList.isNotEmpty()) {
                    for (s in cellSignalList) {
                        info.appendLine("${s.javaClass.simpleName}:")
                        info.appendLine("  Level: ${s.level}")
                        info.appendLine("  dBm: ${s.dbm}")
                        info.appendLine("---")
                    }
                } else {
                    info.appendLine("No cell signal strengths available")
                }
            } else {
                // Fallback for older APIs (Android 9 and below)
                @Suppress("DEPRECATION")
                info.appendLine("GSM Signal Strength: ${signalStrength.gsmSignalStrength}")
                @Suppress("DEPRECATION")
                info.appendLine("GSM Bit Error Rate: ${signalStrength.gsmBitErrorRate}")
            }

            // Add information about all visible cell towers
            info.appendLine("\n=== ALL VISIBLE CELLS ===")
            info.append(getAllCellInfoInternal(context))

            info.toString()
        } catch (e: Exception) {
            "Error parsing signal: ${e.localizedMessage}"
        }
    }

    private fun getAllCellInfoInternal(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return try {
            val allCells = telephonyManager.allCellInfo
            val info = StringBuilder()

            if (allCells != null && allCells.isNotEmpty()) {
                info.appendLine("Found ${allCells.size} cell tower(s):")
                allCells.forEachIndexed { index, cellInfo ->
                    info.appendLine("Cell ${index + 1}:")
                    info.appendLine("  Type: ${cellInfo.javaClass.simpleName}")
                    info.appendLine("  Registered: ${cellInfo.isRegistered}")

                    // Get signal strength for each cell type
                    when (cellInfo) {
                        is android.telephony.CellInfoLte -> {
                            info.appendLine("  Signal: ${cellInfo.cellSignalStrength.dbm} dBm")
                            info.appendLine("  Level: ${cellInfo.cellSignalStrength.level}")
                            info.appendLine("  RSRP: ${cellInfo.cellSignalStrength.rsrp} dBm")
                            info.appendLine("  RSRQ: ${cellInfo.cellSignalStrength.rsrq} dB")
                        }
                        is android.telephony.CellInfoGsm -> {
                            info.appendLine("  Signal: ${cellInfo.cellSignalStrength.dbm} dBm")
                            info.appendLine("  Level: ${cellInfo.cellSignalStrength.level}")
                        }
                        is android.telephony.CellInfoWcdma -> {
                            info.appendLine("  Signal: ${cellInfo.cellSignalStrength.dbm} dBm")
                            info.appendLine("  Level: ${cellInfo.cellSignalStrength.level}")
                        }
                        is android.telephony.CellInfoCdma -> {
                            info.appendLine("  Signal: ${cellInfo.cellSignalStrength.dbm} dBm")
                            info.appendLine("  Level: ${cellInfo.cellSignalStrength.level}")
                        }
                        else -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                when (cellInfo) {
                                    is android.telephony.CellInfoNr -> {
                                        val nrSignal = cellInfo.cellSignalStrength as android.telephony.CellSignalStrengthNr
                                        info.appendLine("  Signal: ${nrSignal.dbm} dBm")
                                        info.appendLine("  Level: ${nrSignal.level}")
                                    }
                                    is android.telephony.CellInfoTdscdma -> {
                                        info.appendLine("  Signal: ${cellInfo.cellSignalStrength.dbm} dBm")
                                        info.appendLine("  Level: ${cellInfo.cellSignalStrength.level}")
                                    }
                                }
                            }
                        }
                    }
                    info.appendLine("---")
                }
            } else {
                info.appendLine("No cell towers found or permission denied")
                info.appendLine("Make sure ACCESS_FINE_LOCATION permission is granted")
            }

            info.toString()
        } catch (e: SecurityException) {
            "Location permission required: ${e.localizedMessage}\n" +
                    "Add ACCESS_FINE_LOCATION permission to see all cell towers"
        } catch (e: Exception) {
            "Error getting cell info: ${e.localizedMessage}"
        }
    }
}