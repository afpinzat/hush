package com.pinza.hush.domain.usecase.player

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val repository: ISongRepository
) {
    // Si 'onlyFavorites' es true, busca solo en los marcados como corazón
    operator fun invoke(query: String, onlyFavorites: Boolean = false): Flow<List<Song>> {
        return if (onlyFavorites) {
            repository.searchFavorites(query)
        } else {
            // Aquí puedes llamar a una función en el repositorio que
            // busque en todas las canciones (ej: searchAll(query))
            repository.searchAllSongs(query)
        }
    }
}