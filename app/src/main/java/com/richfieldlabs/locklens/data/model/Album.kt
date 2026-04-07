package com.richfieldlabs.locklens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPhotoId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val photoCount: Int = 0,
)

