package com.pinza.hush.domain.usecase.player

import com.pinza.hush.domain.repository.IPlayerStateRepository
import javax.inject.Inject

class ClearPlayerStateUseCase @Inject constructor(
    private val repository: IPlayerStateRepository
) {

    suspend operator fun invoke() {

        repository.clear()

    }

}