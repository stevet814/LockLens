package com.richfieldlabs.locklens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.richfieldlabs.locklens.data.model.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    fun observeById(albumId: Long): Flow<Album?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album): Long

    @Update
    suspend fun update(album: Album)
}

