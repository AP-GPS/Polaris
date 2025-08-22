package com.example.polaris.utils

import com.example.polaris.data.entity.MonitoringSnapshot
import com.example.polaris.service.ApiService
import com.example.polaris.service.PolarisApiRequest
import android.util.Log
import com.google.gson.Gson

/**
 * Maps a local MonitoringSnapshot entity to the API request body.
 */
fun mapSnapshotToApiRequest(snapshot: MonitoringSnapshot): PolarisApiRequest {
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

    // Log the snapshot and API request as JSON
    val gson = Gson()
    Log.d("SnapshotMapping", "Original snapshot: ${gson.toJson(snapshot)}")
    Log.d("SnapshotMapping", "Mapped API request: ${gson.toJson(apiRequest)}")

    return apiRequest
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

/**
 * Extracts signal strength (dBm) from the signal info string.
 */
fun extractSignalStrength(signalInfo: String): Int {
    // Example: "SignalStrength: -70 dBm, level 3"
    val regex = Regex("(-?\\d+)\\s*dBm")
    return regex.find(signalInfo)?.groupValues?.get(1)?.toIntOrNull() ?: -999
}

/**
 * Extracts network type (LTE/5G/NR/etc.) from the signal info string.
 */
fun extractNetworkType(signalInfo: String): String {
    return when {
        signalInfo.contains("LTE", ignoreCase = true) -> "LTE"
        signalInfo.contains("5G", ignoreCase = true) -> "5G"
        signalInfo.contains("NR", ignoreCase = true) -> "NR"
        signalInfo.contains("WCDMA", ignoreCase = true) -> "WCDMA"
        signalInfo.contains("GSM", ignoreCase = true) -> "GSM"
        else -> "UNKNOWN"
    }
}
