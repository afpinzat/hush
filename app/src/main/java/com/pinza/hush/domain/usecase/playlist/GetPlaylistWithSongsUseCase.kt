package com.pinza.hush.domain.usecase.playlist

import com.pinza.hush.domain.repository.IPlaylistRepository
import javax.inject.Inject

class GetPlaylistWithSongsUseCase @Inject constructor(
    private val playlistRepository: IPlaylistRepository
) {
    // Retornamos el Flow directamente
    operator fun invoke(playlistId: Long) = playlistRepository.getPlaylistWithSongs(playlistId)
}