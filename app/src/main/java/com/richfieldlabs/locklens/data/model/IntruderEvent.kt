package com.richfieldlabs.locklens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruder_events")
data class IntruderEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedSelfieFilePath: String? = null,
    val selfieIv: String? = null,
    val attemptedPin: String,
    val attemptedAt: Long = System.currentTimeMillis(),
)
