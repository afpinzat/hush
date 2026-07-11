package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.PlayerState
import kotlinx.coroutines.flow.Flow

interface IPlayerStateRepository {
    suspend fun savePlayerState(state: PlayerState)
    suspend fun getPlayerState(): PlayerState?
}