package com.overtime.worker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WorkRecordEntity::class], version = 3, exportSchema = true)
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
                        overtimeStart TEXT,
                        overtimeEnd TEXT,
                        overtimeHours REAL NOT NULL,
                        hourlyRateCents INTEGER,
                        monthlySalaryCents INTEGER,
                        workingDaysPerMonth REAL,
                        workingHoursPerDay REAL,
                        overtimeMultiplier REAL NOT NULL,
                        overtimePayCents INTEGER NOT NULL,
                        currencyCode TEXT NOT NULL DEFAULT 'SAR',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                val sourceCount = database.query("SELECT COUNT(*) FROM work_records").use { if (it.moveToFirst()) it.getLong(0) else 0L }
                database.execSQL("""
                    INSERT INTO overtime_records
                    (id, calculationMethod, date, overtimeStart, overtimeEnd, overtimeHours,
                     hourlyRateCents, monthlySalaryCents, workingDaysPerMonth, workingHoursPerDay,
                     overtimeMultiplier, overtimePayCents, currencyCode, notes, createdAt, updatedAt)
                    SELECT id, 'HOURLY_RATE', workDate, NULL, NULL,
                           overtimeHours, hourlyRateCents, NULL, NULL, NULL, multiplier, overtimePayCents,
                           'SAR', note, strftime('%s','now') * 1000, strftime('%s','now') * 1000
                    FROM work_records
                """.trimIndent())
                val destinationCount = database.query("SELECT COUNT(*) FROM overtime_records").use { if (it.moveToFirst()) it.getLong(0) else 0L }
                require(sourceCount == destinationCount) { "Migration 1→2 لم تنسخ جميع السجلات" }
                database.execSQL("DROP TABLE work_records")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE overtime_records_v3 (
                        id TEXT NOT NULL PRIMARY KEY,
                        calculationMethod TEXT NOT NULL,
                        date TEXT NOT NULL,
                        overtimeStart TEXT,
                        overtimeEnd TEXT,
                        overtimeHours TEXT NOT NULL,
                        hourlyRateAmount TEXT,
                        monthlySalaryAmount TEXT,
                        workingDaysPerMonth TEXT,
                        workingHoursPerDay TEXT,
                        overtimeMultiplier TEXT NOT NULL,
                        overtimePayAmount TEXT NOT NULL,
                        currencyCode TEXT NOT NULL,
                        moneyScale INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        legacyTiming INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                val sourceCount = database.query("SELECT COUNT(*) FROM overtime_records").use { if (it.moveToFirst()) it.getLong(0) else 0L }
                database.execSQL("""
                    INSERT INTO overtime_records_v3
                    (id, calculationMethod, date, overtimeStart, overtimeEnd, overtimeHours,
                     hourlyRateAmount, monthlySalaryAmount, workingDaysPerMonth, workingHoursPerDay,
                     overtimeMultiplier, overtimePayAmount, currencyCode, moneyScale, notes, legacyTiming, createdAt, updatedAt)
                    SELECT id, calculationMethod, date, NULL, NULL, CAST(overtimeHours AS TEXT),
                           CAST(hourlyRateCents AS TEXT), CAST(monthlySalaryCents AS TEXT),
                           CASE WHEN workingDaysPerMonth IS NULL THEN NULL ELSE CAST(workingDaysPerMonth AS TEXT) END,
                           CASE WHEN workingHoursPerDay IS NULL THEN NULL ELSE CAST(workingHoursPerDay AS TEXT) END,
                           CAST(overtimeMultiplier AS TEXT), CAST(overtimePayCents AS TEXT),
                           currencyCode,
                           2, notes, 1, createdAt, updatedAt
                    FROM overtime_records
                """.trimIndent())
                val destinationCount = database.query("SELECT COUNT(*) FROM overtime_records_v3").use { if (it.moveToFirst()) it.getLong(0) else 0L }
                require(sourceCount == destinationCount) { "Migration 2→3 لم تنسخ جميع السجلات" }
                database.execSQL("DROP TABLE overtime_records")
                database.execSQL("ALTER TABLE overtime_records_v3 RENAME TO overtime_records")
            }
        }

        @Volatile private var INSTANCE: OvertimeDatabase? = null
        fun getInstance(context: Context): OvertimeDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, OvertimeDatabase::class.java, "overtime_worker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}
