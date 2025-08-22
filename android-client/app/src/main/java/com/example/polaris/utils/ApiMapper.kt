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
    // Parse primary LTE cell lines
    val primaryCellLines = snapshot.cellInfo.lines()
        .filter { it.contains(":") }
        .map { it.trim() }

    fun getIntValue(key: String): Int? =
        primaryCellLines.find { it.startsWith(key, ignoreCase = true) }
            ?.split(":")?.get(1)?.trim()?.toIntOrNull()

    fun getLongValue(key: String): Long? =
        primaryCellLines.find { it.startsWith(key, ignoreCase = true) }
            ?.split(":")?.get(1)?.trim()?.toLongOrNull()

    fun getDoubleValue(key: String): Double? =
        primaryCellLines.find { it.startsWith(key, ignoreCase = true) }
            ?.split(":")?.get(1)?.trim()?.toDoubleOrNull()

    val cellId = getIntValue("Cell ID") ?: 0
    val tacOrLac = getIntValue("TAC") ?: getIntValue("LAC") ?: 0
    val code = getIntValue("PCI") ?: 0
    val ulArfcn = getIntValue("EARFCN") ?: 0
    val dlArfcn = ulArfcn  // Assuming same as UL
    val band = getIntValue("Band") ?: mapEarFcnToBand(ulArfcn)
    val ulBw = getDoubleValue("UL_BW") ?: 0.0
    val dlBw = getDoubleValue("DL_BW") ?: 0.0
    val plmnId = getIntValue("PLMN") ?: 0
    val rac = getIntValue("RAC") ?: 0
    val siteId = getIntValue("SiteId") ?: 0
    val n = getIntValue("N") ?: 1
    val s = getIntValue("S") ?: 1
    val t = getIntValue("T") ?: 1
    val longCellId = getLongValue("CellId") ?: 0

    val signalStrength = extractSignalStrength(snapshot.signalInfo)
    val networkType = extractNetworkType(snapshot.cellInfo)

    val apiRequest = PolarisApiRequest(
        timestamp = snapshot.timestamp,
        n = n,
        s = s,
        t = t,
        band = band,
        ulArfcn = ulArfcn,
        dlArfcn = dlArfcn,
        code = code,
        ulBw = ulBw,
        dlBw = dlBw,
        plmnId = plmnId,
        tacOrLac = tacOrLac,
        rac = rac,
        longCellId = longCellId,
        siteId = siteId,
        cellId = cellId,
        latitude = snapshot.latitude ?: 0.0,
        longitude = snapshot.longitude ?: 0.0,
        signalStrength = signalStrength,
        networkType = networkType,
        downloadSpeed = 0.0,
        uploadSpeed = 0.0,
        pingTime = 0
    )

    Log.d("SnapshotMapping", "Original snapshot: ${Gson().toJson(snapshot)}")
    Log.d("SnapshotMapping", "Mapped API request: ${Gson().toJson(apiRequest)}")

    return apiRequest
}

// Optional helper to map EARFCN to LTE band dynamically if Band key is missing
fun mapEarFcnToBand(earfcn: Int): Int {
    return when (earfcn) {
        in 0..599 -> 1
        in 600..1199 -> 3
        in 1200..1949 -> 7
        in 1950..2399 -> 20
        in 2400..2649 -> 38
        else -> 0
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
