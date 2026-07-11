package com.pinza.hush.di

import com.pinza.hush.data.player.PlayerManager
import com.pinza.hush.data.repository.AuthRepository
import com.pinza.hush.data.repository.QueueRepository
import com.pinza.hush.data.repository.SongRepository
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IAuthRepository
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.repository.IQueueRepository
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.repository.PlaylistRepository
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
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepository: PlaylistRepository
    ): IPlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSongRepository(
        songRepository: SongRepository
    ): ISongRepository

    @Binds
    @Singleton
    abstract fun bindQueueRepository(
        queueRepository: QueueRepository
    ): IQueueRepository

    @Binds
    @Singleton
    abstract fun bindPlayerManager(
        playerManager: PlayerManager
    ): IPlayerManager
}