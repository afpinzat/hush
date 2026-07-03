// di/PlayerModule.kt
package com.pinza.hush.di

import android.content.Context
import com.pinza.hush.data.player.PlayerManager
import com.pinza.hush.domain.player.IPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerManager(
        @ApplicationContext context: Context
    ): IPlayerManager {
        return PlayerManager(context)
    }
}