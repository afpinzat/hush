package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.domain.repository.IPlayerStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerStateRepository @Inject constructor(
    private val playerStateDao: PlayerStateDao
) : IPlayerStateRepository {

    override suspend fun savePlayerState(state: PlayerState) {
        playerStateDao.saveState(state)
    }

    override suspend fun getPlayerState(): PlayerState? {
        return playerStateDao.getPlayerState()
    }
}