package com.pinza.hush.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.dao.QueueDao
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.dao.UserDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSongCrossRef
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.data.local.model.QueueItem
import com.pinza.hush.data.local.model.User

@Database(
    entities = [
        Song::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        PlayerState::class,
        SongLyrics::class,
        QueueItem::class,
        User::class
    ],
    version = 10,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playerStateDao(): PlayerStateDao
    abstract fun songLyricsDao(): SongLyricsDao
    abstract fun queueDao(): QueueDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "hush_database"
                )
                    // En desarrollo, esto es lo más seguro para evitar crashes constantes:
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}