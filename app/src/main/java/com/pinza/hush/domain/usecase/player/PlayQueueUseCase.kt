package com.pinza.hush.domain.usecase.player

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import javax.inject.Inject

class PlayQueueUseCase @Inject constructor(
    private val playerManager: IPlayerManager
) {
    operator fun invoke(songs: List<Song>, startIndex: Int = 0) {
        playerManager.playQueue(songs, startIndex)
    }
}
