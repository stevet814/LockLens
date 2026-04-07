package com.richfieldlabs.locklens.data.repository

import com.richfieldlabs.locklens.data.db.PhotoDao
import com.richfieldlabs.locklens.data.model.Photo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
) {
    fun observePhotos(): Flow<List<Photo>> = photoDao.observeAll()

    fun observePhoto(photoId: Long): Flow<Photo?> = photoDao.observeById(photoId)

    fun observeAlbum(albumId: Long): Flow<List<Photo>> = photoDao.observeByAlbum(albumId)

    suspend fun insert(photo: Photo): Long = photoDao.insert(photo)

    suspend fun delete(photo: Photo) = photoDao.delete(photo)

    suspend fun count(): Int = photoDao.count()
}

