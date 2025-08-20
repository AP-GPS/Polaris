package com.example.polaris.data.repo

import android.content.Context
import com.example.polaris.data.db.PolarisDatabase
import com.example.polaris.data.entity.MonitoringSnapshot

class SnapshotRepository(context: Context) {
    private val snapshotDao = PolarisDatabase.getInstance(context).snapshotDao()

    suspend fun insert(snapshot: MonitoringSnapshot) {
        snapshotDao.insert(snapshot)
    }

    fun getAll() = snapshotDao.getAll()
}
