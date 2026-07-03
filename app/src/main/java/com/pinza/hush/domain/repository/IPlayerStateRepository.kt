package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.PlayerState
import kotlinx.coroutines.flow.Flow

interface IPlayerStateRepository {

    fun getPlayerState(): Flow<PlayerState?>

    suspend fun save(state: PlayerState)

    suspend fun clear()
}