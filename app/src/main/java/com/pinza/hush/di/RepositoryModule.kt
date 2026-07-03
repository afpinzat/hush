// di/RepositoryModule.kt
package com.pinza.hush.di

import com.pinza.hush.data.repository.PlayerStateRepository
import com.pinza.hush.data.repository.PlaylistRepository
import com.pinza.hush.data.repository.ScanResultRepository
import com.pinza.hush.data.repository.SongLyricsRepository
import com.pinza.hush.data.repository.SongRepository
import com.pinza.hush.domain.repository.IPlayerStateRepository
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.repository.IScanResultRepository
import com.pinza.hush.domain.repository.ISongLyricsRepository
import com.pinza.hush.domain.repository.ISongRepository
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
        repository: SongRepository
    ): ISongRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        repository: PlaylistRepository
    ): IPlaylistRepository

    @Binds
    @Singleton
    abstract fun bindPlayerStateRepository(
        repository: PlayerStateRepository
    ): IPlayerStateRepository

    @Binds
    @Singleton
    abstract fun bindLyricsRepository(
        repository: SongLyricsRepository
    ): ISongLyricsRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        repository: ScanResultRepository
    ): IScanResultRepository
}