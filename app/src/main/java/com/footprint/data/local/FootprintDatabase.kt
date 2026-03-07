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
                        TrackPointEntity::class,
                        TimeCapsuleEntity::class,
                        UserBadgeEntity::class,
                        UserStatsEntity::class],
        version = 10,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FootprintDatabase : RoomDatabase() {
        abstract fun footprintDao(): FootprintDao
        abstract fun travelGoalDao(): TravelGoalDao
        abstract fun premiumDao(): PremiumDao
        abstract fun trackPointDao(): TrackPointDao
        abstract fun timeCapsuleDao(): TimeCapsuleDao
        abstract fun userStatsDao(): UserStatsDao
        abstract fun userBadgesDao(): UserBadgesDao

        companion object {
                @Volatile private var instance: FootprintDatabase? = null

                val MIGRATION_5_6 =
                        object : androidx.room.migration.Migration(5, 6) {
                                override fun migrate(
                                        db: androidx.sqlite.db.SupportSQLiteDatabase
                                ) {
                                        // Create Badges table
                                        db.execSQL(
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
                                        db.execSQL(
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
                                        db.execSQL(
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

                val MIGRATION_6_7 =
                        object : androidx.room.migration.Migration(6, 7) {
                                override fun migrate(
                                        db: androidx.sqlite.db.SupportSQLiteDatabase
                                ) {
                                        // Safely add columns to footprints table
                                        try {
                                                db.execSQL(
                                                        "ALTER TABLE footprints ADD COLUMN transport_type TEXT NOT NULL DEFAULT 'UNKNOWN'"
                                                )
                                        } catch (e: Exception) {
                                                // Column might already exist
                                        }
                                        try {
                                                db.execSQL(
                                                        "ALTER TABLE footprints ADD COLUMN carbon_saved REAL NOT NULL DEFAULT 0.0"
                                                )
                                        } catch (e: Exception) {
                                                // Column might already exist
                                        }
                                        try {
                                                db.execSQL(
                                                        "ALTER TABLE footprints ADD COLUMN icon TEXT NOT NULL DEFAULT 'LocationOn'"
                                                )
                                        } catch (e: Exception) {
                                                // Column might already exist
                                        }

                                        // Safely add columns to travel_goals table
                                        try {
                                                db.execSQL(
                                                        "ALTER TABLE travel_goals ADD COLUMN icon TEXT NOT NULL DEFAULT 'Flag'"
                                                )
                                        } catch (e: Exception) {
                                                // Column might already exist
                                        }
                                }
                        }

                val MIGRATION_7_8 =
                        object : androidx.room.migration.Migration(7, 8) {
                                override fun migrate(
                                        db: androidx.sqlite.db.SupportSQLiteDatabase
                                ) {
                                        // Create Time Capsules table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS `time_capsules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `message` TEXT NOT NULL,
                        `contentUri` TEXT,
                        `creationTime` INTEGER NOT NULL,
                        `unlockTime` INTEGER NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `radius` REAL NOT NULL
                    )
                """
                                        )
                                }
                        }

                val MIGRATION_9_10 =
                        object : androidx.room.migration.Migration(9, 10) {
                                override fun migrate(
                                        db: androidx.sqlite.db.SupportSQLiteDatabase
                                ) {
                                        // Add adcode to track_points
                                        try {
                                                db.execSQL(
                                                        "ALTER TABLE track_points ADD COLUMN adcode TEXT"
                                                )
                                        } catch (e: Exception) {
                                                // Column might already exist
                                        }

                                        // Create User Badges table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS `user_badges` (
                        `badgeId` TEXT NOT NULL,
                        `unlockDate` INTEGER NOT NULL,
                        `unlockLat` REAL,
                        `unlockLng` REAL,
                        `unlockWeather` TEXT,
                        `unlockMileage` REAL,
                        PRIMARY KEY(`badgeId`)
                    )
                """
                                        )

                                        // Create User Stats table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS `user_stats` (
                        `id` INTEGER NOT NULL,
                        `totalMileage` REAL NOT NULL,
                        `totalDays` INTEGER NOT NULL,
                        `citiesVisitedCount` INTEGER NOT NULL,
                        `totalFootprints` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """
                                        )

                                        // Ensure a single row exists
                                        db.execSQL(
                                                """
                    INSERT OR IGNORE INTO user_stats (id, totalMileage, totalDays, citiesVisitedCount, totalFootprints)
                    VALUES (1, 0.0, 0, 0, 0)
                """
                                        )
                                }
                        }

                fun getInstance(context: Context): FootprintDatabase =
                        instance
                                ?: synchronized(this) {
                                        instance ?: build(context).also { instance = it }
                                }

                private fun build(context: Context): FootprintDatabase =
                        Room.databaseBuilder(
                                        context.applicationContext,
                                        FootprintDatabase::class.java,
                                        "footprint-db"
                                )
                                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                                .addMigrations(MIGRATION_9_10)
                                .fallbackToDestructiveMigration()
                                .build()
        }
}
