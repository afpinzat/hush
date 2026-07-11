package com.pinza.hush.domain.usecase.player

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ISongRepository
) {
    // Recibe la canción completa para tener su ID y su estado actual
    suspend operator fun invoke(song: Song) {
        val newFavoriteStatus = !song.isFavorite
        repository.toggleFavorite(song.id, newFavoriteStatus)
    }
}