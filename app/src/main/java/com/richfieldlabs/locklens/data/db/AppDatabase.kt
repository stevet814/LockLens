package com.richfieldlabs.locklens.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.richfieldlabs.locklens.data.model.Album
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.model.Photo

@Database(
    entities = [Photo::class, Album::class, IntruderEvent::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun albumDao(): AlbumDao
    abstract fun intruderDao(): IntruderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE intruder_events ADD COLUMN selfieIv TEXT")
            }
        }
    }
}
