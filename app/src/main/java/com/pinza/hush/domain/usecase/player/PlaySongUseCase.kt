package com.pinza.hush.domain.usecase.player

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val playerManager: IPlayerManager
) {
    operator fun invoke(song: Song) {
        playerManager.play(song)
    }
}