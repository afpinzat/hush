package com.pinza.hush.domain.usecase.song

import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class GetSongsUseCase @Inject constructor(
    private val repository: ISongRepository
) {

    operator fun invoke() =
        repository.getSongs()

}