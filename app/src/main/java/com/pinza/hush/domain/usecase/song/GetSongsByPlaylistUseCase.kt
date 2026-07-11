package com.pinza.hush.domain.usecase.song

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class GetSongsByPlaylistUseCase @Inject constructor(
    private val songRepository: ISongRepository
) {
    // Definimos el operador invoke para llamarlo como una función
    suspend operator fun invoke(playlistId: Long): List<Song> {
        return songRepository.getSongsByPlaylist(playlistId)
    }
}