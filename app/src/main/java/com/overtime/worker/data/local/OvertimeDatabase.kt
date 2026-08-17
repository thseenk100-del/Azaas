package com.overtime.worker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WorkRecordEntity::class], version = 2, exportSchema = true)
abstract class OvertimeDatabase : RoomDatabase() {
    abstract fun workRecordDao(): WorkRecordDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS overtime_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        calculationMethod TEXT NOT NULL DEFAULT 'HOURLY_RATE',
                        date TEXT NOT NULL,
                        overtimeStart TEXT NOT NULL,
                        overtimeEnd TEXT NOT NULL,
                        overtimeHours TEXT NOT NULL,
                        hourlyRateAmount TEXT,
                        monthlySalaryAmount TEXT,
                        workingDaysPerMonth TEXT,
                        workingHoursPerDay TEXT,
                        overtimeMultiplier TEXT NOT NULL,
                        overtimePayAmount TEXT NOT NULL,
                        currencyCode TEXT NOT NULL DEFAULT 'YER',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO overtime_records
                    (id, calculationMethod, date, overtimeStart, overtimeEnd, overtimeHours,
                     hourlyRateAmount, monthlySalaryAmount, workingDaysPerMonth, workingHoursPerDay,
                     overtimeMultiplier, overtimePayAmount, currencyCode, notes, createdAt, updatedAt)
                    SELECT id, 'HOURLY_RATE', workDate,
                           workDate || 'T00:00:00',
                           datetime(workDate || 'T00:00:00', '+' || CAST(ROUND(overtimeHours * 60) AS INTEGER) || ' minutes'),
                           CAST(overtimeHours AS TEXT),
                           CAST(hourlyRateCents AS TEXT), NULL, NULL, NULL,
                           CAST(multiplier AS TEXT), CAST(overtimePayCents / 100.0 AS TEXT),
                           'YER', note, strftime('%s','now') * 1000, strftime('%s','now') * 1000
                    FROM work_records
                    WHERE overtimeHours > 0
                """.trimIndent())
                database.execSQL("DROP TABLE work_records")
            }
        }

        @Volatile private var INSTANCE: OvertimeDatabase? = null

        fun getInstance(context: Context): OvertimeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, OvertimeDatabase::class.java, "overtime_worker.db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
