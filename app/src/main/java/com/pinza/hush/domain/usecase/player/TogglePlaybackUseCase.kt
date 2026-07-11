package com.pinza.hush.domain.usecase.player

import com.pinza.hush.domain.player.IPlayerManager
import javax.inject.Inject

class TogglePlaybackUseCase @Inject constructor(
    private val playerManager: IPlayerManager
) {
    operator fun invoke() {
        if (playerManager.isPlaying()) {
            playerManager.pause()
        } else {
            playerManager.resume()
        }
    }
}