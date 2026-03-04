package com.footprint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1") fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1") suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: UserStatsEntity)

    @Query(
            """
        UPDATE user_stats SET 
        totalMileage = totalMileage + :addedMileage,
        totalFootprints = totalFootprints + 1
        WHERE id = 1
    """
    )
    suspend fun incrementStats(addedMileage: Double)
}

@Dao
interface UserBadgesDao {
    @Query("SELECT * FROM user_badges ORDER BY unlockDate DESC")
    fun observeUserBadges(): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges WHERE badgeId = :badgeId LIMIT 1")
    suspend fun getBadgeById(badgeId: String): UserBadgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBadge(badge: UserBadgeEntity)

    @Query("SELECT badgeId FROM user_badges") suspend fun getAllUnlockedBadgeIds(): List<String>
}
