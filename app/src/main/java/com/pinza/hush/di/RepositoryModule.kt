package com.pinza.hush.di

import com.pinza.hush.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongRepository(
        songRepositoryImpl: SongRepositoryImpl
    ): SongRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindQueueRepository(
        queueRepositoryImpl: QueueRepositoryImpl
    ): QueueRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        lyricsRepositoryImpl: LyricsRepositoryImpl
    ): LyricsRepository
}
