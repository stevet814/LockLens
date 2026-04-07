package com.richfieldlabs.locklens.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("albumId"), Index("capturedAt")],
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long?,
    val encryptedFilePath: String,
    val encryptedThumbPath: String,
    val iv: String,
    val thumbIv: String,
    val mimeType: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val capturedAt: Long,
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val label: String = "",
)

