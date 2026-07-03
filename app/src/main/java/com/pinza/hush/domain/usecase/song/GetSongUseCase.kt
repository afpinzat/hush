package com.pinza.hush.domain.usecase.song

import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class GetSongUseCase @Inject constructor(
    private val repository: ISongRepository
) {

    suspend operator fun invoke(id: Int) =
        repository.getSong(id)

}