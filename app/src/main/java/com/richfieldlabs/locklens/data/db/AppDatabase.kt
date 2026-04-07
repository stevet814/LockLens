package com.richfieldlabs.locklens.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.richfieldlabs.locklens.data.model.Album
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.model.Photo

@Database(
    entities = [Photo::class, Album::class, IntruderEvent::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao
    abstract fun intruderDao(): IntruderDao
}

