package com.footprint.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PremiumDao {
    @Query("SELECT * FROM badges")
    fun observeBadges(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadge(badge: BadgeEntity)

    @Query("SELECT * FROM privacy_fences")
    fun observeFences(): Flow<List<PrivacyFenceEntity>>

    @Insert
    suspend fun insertFence(fence: PrivacyFenceEntity)

    @Delete
    suspend fun deleteFence(fence: PrivacyFenceEntity)
    
    @Update
    suspend fun updateBadge(badge: BadgeEntity)
}
