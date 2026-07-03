package com.pinza.hush.domain.usecase.playlist

import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.domain.repository.IPlaylistRepository
import javax.inject.Inject

class DeletePlaylistUseCase @Inject constructor(
    private val repository: IPlaylistRepository
) {

    suspend operator fun invoke(playlist: Playlist) {

        repository.delete(playlist)

    }

}