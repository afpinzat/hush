package com.pinza.hush.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.dao.ScanResultDao
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSong
import com.pinza.hush.data.local.model.ScanResult
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics

@Database(
    entities = [
        Song::class,
        Playlist::class,
        PlaylistSong::class,
        PlayerState::class,
        SongLyrics::class,
        ScanResult::class
    ],
    version = 2,  // ✅ Cambiar a 2
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playerStateDao(): PlayerStateDao
    abstract fun songLyricsDao(): SongLyricsDao
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE player_state ADD COLUMN queueIds TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE player_state ADD COLUMN queueIndex INTEGER NOT NULL DEFAULT -1")
                } catch (e: Exception) {
                    // Si las columnas ya existen, ignorar
                }
            }
        }
    }
}