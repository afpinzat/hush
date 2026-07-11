package com.pinza.hush.domain.usecase.playlist

import com.pinza.hush.data.local.model.PlaylistWithSongs
import com.pinza.hush.domain.repository.IPlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllPlaylistsUseCase @Inject constructor(
    private val repository: IPlaylistRepository
) {
    operator fun invoke(): Flow<List<PlaylistWithSongs>> {
        return repository.getAllPlaylists()
    }
}
