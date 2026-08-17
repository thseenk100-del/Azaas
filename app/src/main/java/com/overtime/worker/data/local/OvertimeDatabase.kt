package com.overtime.worker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkRecordEntity::class], version = 1, exportSchema = true)
abstract class OvertimeDatabase : RoomDatabase() {
    abstract fun workRecordDao(): WorkRecordDao

    companion object {
        @Volatile private var INSTANCE: OvertimeDatabase? = null

        fun getInstance(context: Context): OvertimeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OvertimeDatabase::class.java,
                    "overtime_worker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
