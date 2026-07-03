package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.PlayerStateDao
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.domain.repository.IPlayerStateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlayerStateRepository @Inject constructor(
    private val dao: PlayerStateDao
) : IPlayerStateRepository {

    override fun getPlayerState(): Flow<PlayerState?> =
        dao.getPlayerState()

    override suspend fun save(state: PlayerState) =
        dao.saveState(state)

    override suspend fun clear() =
        dao.clear()
}