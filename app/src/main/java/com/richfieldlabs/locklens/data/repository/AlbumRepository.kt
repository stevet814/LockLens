package com.richfieldlabs.locklens.data.repository

import com.richfieldlabs.locklens.data.db.AlbumDao
import com.richfieldlabs.locklens.data.model.Album
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AlbumRepository @Inject constructor(
    private val albumDao: AlbumDao,
) {
    fun observeAll(): Flow<List<Album>> = albumDao.observeAll()

    fun observeById(albumId: Long): Flow<Album?> = albumDao.observeById(albumId)

    suspend fun insert(album: Album): Long = albumDao.insert(album)

    suspend fun update(album: Album) = albumDao.update(album)
}
