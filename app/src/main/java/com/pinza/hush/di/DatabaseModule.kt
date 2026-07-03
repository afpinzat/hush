package com.pinza.hush.di

import android.content.Context
import androidx.room.Room
import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.dao.ScanResultDao
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.database.MusicDatabase
import com.pinza.hush.utils.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        )
            .addMigrations(MusicDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()  // ⚠️ Solo para desarrollo, borra datos
            .build()
    }

    @Provides
    fun provideSongDao(db: MusicDatabase): SongDao =
        db.songDao()

    @Provides
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao =
        db.playlistDao()

    @Provides
    fun providePlayerStateDao(db: MusicDatabase): PlayerStateDao =
        db.playerStateDao()

    @Provides
    fun provideLyricsDao(db: MusicDatabase): SongLyricsDao =
        db.songLyricsDao()

    @Provides
    fun provideScanDao(db: MusicDatabase): ScanResultDao =
        db.scanResultDao()
    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }


}