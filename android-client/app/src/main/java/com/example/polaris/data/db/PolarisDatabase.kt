package com.example.polaris.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.polaris.data.entity.MonitoringSnapshot
import com.example.polaris.data.dao.SnapshotDao

@Database(entities = [MonitoringSnapshot::class], version = 1, exportSchema = false)
abstract class PolarisDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile private var instance: PolarisDatabase? = null

        fun getInstance(context: Context): PolarisDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PolarisDatabase::class.java,
                    "polaris_db"
                ).build().also { instance = it }
            }
        }
    }
}
