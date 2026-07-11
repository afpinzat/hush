package com.pinza.hush.di

import android.content.Context
import androidx.room.Room
import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.dao.QueueDao
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.database.MusicDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "hush_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    // Asegúrate de tener TODAS estas funciones aquí:
    @Provides
    fun provideSongDao(db: MusicDatabase): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(db: MusicDatabase): PlaylistDao = db.playlistDao()


    @Provides
    fun provideLyricsDao(db: MusicDatabase): SongLyricsDao = db.songLyricsDao()

    // ¡ESTA ES LA QUE FALTA!
    @Provides
    fun provideQueueDao(db: MusicDatabase): QueueDao = db.queueDao()
}