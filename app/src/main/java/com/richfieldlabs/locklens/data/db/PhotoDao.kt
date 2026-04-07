package com.richfieldlabs.locklens.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.richfieldlabs.locklens.data.model.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE id = :photoId LIMIT 1")
    fun observeById(photoId: Long): Flow<Photo?>

    @Query("SELECT * FROM photos WHERE albumId = :albumId ORDER BY capturedAt DESC")
    fun observeByAlbum(albumId: Long): Flow<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Update
    suspend fun update(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)

    @Query("SELECT COUNT(*) FROM photos")
    suspend fun count(): Int
}

