package com.pinza.hush.domain.usecase.library

import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: ISongRepository
) {
    suspend operator fun invoke(songId: Long, isFavorite: Boolean) {
        repository.toggleFavorite(songId, isFavorite)
    }
}