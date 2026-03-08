package com.footprint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeCapsuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeCapsule: TimeCapsuleEntity): Long

    @Query("SELECT * FROM time_capsules WHERE isUnlocked = 1 ORDER BY unlockTime DESC")
    fun getUnlockedCapsules(): Flow<List<TimeCapsuleEntity>>

    @Query("SELECT * FROM time_capsules WHERE isUnlocked = 0 ORDER BY unlockTime ASC")
    fun getLockedCapsules(): Flow<List<TimeCapsuleEntity>>

    @Query("SELECT * FROM time_capsules ORDER BY creationTime DESC")
    fun getAllCapsules(): Flow<List<TimeCapsuleEntity>>

    @Query("SELECT * FROM time_capsules ORDER BY creationTime DESC")
    suspend fun getAllCapsulesOnce(): List<TimeCapsuleEntity>

    @Query("UPDATE time_capsules SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlockCapsule(id: Long)

    @Query("SELECT * FROM time_capsules WHERE isUnlocked = 0 AND unlockTime <= :currentTime")
    suspend fun getReadyToUnlockCapsules(currentTime: Long): List<TimeCapsuleEntity>
}
