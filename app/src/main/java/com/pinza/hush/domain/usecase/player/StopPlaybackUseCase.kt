package com.pinza.hush.domain.usecase.player

import com.pinza.hush.domain.player.IPlayerManager
import javax.inject.Inject

class StopPlaybackUseCase @Inject constructor(
    private val playerManager: IPlayerManager
) {
    operator fun invoke() {
        playerManager.stop()
    }
}