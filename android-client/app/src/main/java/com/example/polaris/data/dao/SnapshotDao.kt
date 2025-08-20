package com.example.polaris.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.polaris.data.entity.MonitoringSnapshot

@Dao
interface SnapshotDao {
    @Insert
    suspend fun insert(snapshot: MonitoringSnapshot)

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAll(): Flow<List<MonitoringSnapshot>>
}