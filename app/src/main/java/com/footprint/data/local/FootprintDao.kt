package com.footprint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FootprintDao {
    @Query("SELECT * FROM footprints ORDER BY happened_on DESC, id DESC")
    fun observeEntries(): Flow<List<FootprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FootprintEntity)

    @Update
    suspend fun update(entry: FootprintEntity)

    @Query("DELETE FROM footprints WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM footprints WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FootprintEntity?

    @Query("SELECT COUNT(*) FROM footprints")
    suspend fun count(): Int

    @Query("SELECT * FROM footprints ORDER BY happened_on DESC, id DESC")
    suspend fun getAll(): List<FootprintEntity>

    @Query("SELECT * FROM footprints ORDER BY happened_on ASC LIMIT :limit OFFSET :offset")
    suspend fun getFootprintsPaged(limit: Int, offset: Int): List<FootprintEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<FootprintEntity>)
}
