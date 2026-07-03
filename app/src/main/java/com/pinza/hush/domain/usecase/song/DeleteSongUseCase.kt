package com.pinza.hush.domain.usecase.song

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class DeleteSongUseCase @Inject constructor(
    private val repository: ISongRepository
) {

    suspend operator fun invoke(song: Song) {
        repository.delete(song)
    }

}