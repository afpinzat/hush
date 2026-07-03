package com.pinza.hush.domain.usecase.song

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class InsertAllSongsUseCase @Inject constructor(
    private val repository: ISongRepository
) {

    suspend operator fun invoke(songs: List<Song>) {
        repository.insertAll(songs)
    }

}