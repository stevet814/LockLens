package com.richfieldlabs.locklens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.richfieldlabs.locklens.data.model.IntruderEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface IntruderDao {
    @Query("SELECT * FROM intruder_events ORDER BY attemptedAt DESC")
    fun observeAll(): Flow<List<IntruderEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: IntruderEvent): Long
}

