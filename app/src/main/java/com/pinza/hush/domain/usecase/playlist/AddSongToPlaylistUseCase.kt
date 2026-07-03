package com.pinza.hush.domain.usecase.playlist

import com.pinza.hush.data.local.model.PlaylistSong
import com.pinza.hush.domain.repository.IPlaylistRepository
import javax.inject.Inject

class AddSongToPlaylistUseCase @Inject constructor(
    private val repository: IPlaylistRepository
) {

    suspend operator fun invoke(item: PlaylistSong) {

        repository.addSong(item)

    }

}