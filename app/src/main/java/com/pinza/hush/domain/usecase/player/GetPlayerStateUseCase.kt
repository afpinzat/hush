package com.pinza.hush.domain.usecase.player

import com.pinza.hush.domain.repository.IPlayerStateRepository
import javax.inject.Inject

class GetPlayerStateUseCase @Inject constructor(
    private val repository: IPlayerStateRepository
) {

    operator fun invoke() =
        repository.getPlayerState()

}