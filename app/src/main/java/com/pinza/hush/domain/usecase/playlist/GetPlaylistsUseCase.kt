package com.pinza.hush.domain.usecase.playlist

import com.pinza.hush.domain.repository.IPlaylistRepository
import javax.inject.Inject

class GetPlaylistsUseCase @Inject constructor(
    private val repository: IPlaylistRepository
) {

    operator fun invoke() =
        repository.getPlaylists()

}