package com.footprint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
        entities =
                [
                        FootprintEntity::class,
                        TravelGoalEntity::class,
                        BadgeEntity::class,
                        PrivacyFenceEntity::class,
                        TrackPointEntity::class],
        version = 6,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FootprintDatabase : RoomDatabase() {
    abstract fun footprintDao(): FootprintDao
    abstract fun travelGoalDao(): TravelGoalDao
    abstract fun premiumDao(): PremiumDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        @Volatile private var instance: FootprintDatabase? = null

        val MIGRATION_5_6 =
                object : androidx.room.migration.Migration(5, 6) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Create Badges table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `badges` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `unlockDate` INTEGER,
                        `category` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `target` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """
                        )

                        // Create Privacy Fences table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `privacy_fences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `radiusMeters` REAL NOT NULL,
                        `isActive` INTEGER NOT NULL
                    )
                """
                        )

                        // Create Track Points table (if not exists)
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `track_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `speed` REAL NOT NULL,
                        `accuracy` REAL NOT NULL,
                        `altitude` REAL NOT NULL
                    )
                """
                        )
                    }
                }

        fun getInstance(context: Context): FootprintDatabase =
                instance ?: synchronized(this) { instance ?: build(context).also { instance = it } }

        private fun build(context: Context): FootprintDatabase =
                Room.databaseBuilder(
                                context.applicationContext,
                                FootprintDatabase::class.java,
                                "footprint-db"
                        )
                        .addMigrations(MIGRATION_5_6)
                        .build()
    }
}
