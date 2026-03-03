package com.footprint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
        @Insert suspend fun insert(point: TrackPointEntity)

        @Query(
                "SELECT * FROM track_points WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC"
        )
        fun getPointsInRange(start: Long, end: Long): Flow<List<TrackPointEntity>>

        @Query(
                "SELECT * FROM track_points WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC"
        )
        suspend fun getPointsInRangeOnce(start: Long, end: Long): List<TrackPointEntity>

        @Query("SELECT * FROM track_points WHERE timestamp >= :since ORDER BY timestamp ASC")
        fun getPointsSince(since: Long): Flow<List<TrackPointEntity>>

        @Query("DELETE FROM track_points WHERE timestamp < :timestamp")
        suspend fun deleteOlderThan(timestamp: Long)

        @Query("SELECT * FROM track_points") suspend fun getAll(): List<TrackPointEntity>

        @Query("SELECT COUNT(*) FROM track_points WHERE timestamp BETWEEN :start AND :end")
        suspend fun getCountInRange(start: Long, end: Long): Int

        @Query("SELECT COUNT(*) FROM track_points WHERE timestamp BETWEEN :start AND :end")
        fun observeCountInRange(start: Long, end: Long): Flow<Int>

        @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
        suspend fun insertAll(points: List<TrackPointEntity>)

        /**
         * Get all distinct location points for fog exploration mask.
         * Grid-snaps to ~100m resolution (3 decimal places ≈ 111m) to reduce point count.
         */
        @Query(
                """
        SELECT ROUND(latitude, 3) as latitude, ROUND(longitude, 3) as longitude, 
               MIN(id) as id, MIN(timestamp) as timestamp, 
               0.0 as speed, 0.0 as accuracy, 0.0 as altitude
        FROM track_points 
        GROUP BY ROUND(latitude, 3), ROUND(longitude, 3)
        """
        )
        suspend fun getAllDistinctLocations(): List<TrackPointEntity>
}
