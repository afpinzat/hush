package com.pinza.hush.domain.usecase.player

import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.domain.repository.IPlayerStateRepository
import javax.inject.Inject

class SavePlayerStateUseCase @Inject constructor(
    private val repository: IPlayerStateRepository
) {

    suspend operator fun invoke(state: PlayerState) {

        repository.save(state)

    }

}