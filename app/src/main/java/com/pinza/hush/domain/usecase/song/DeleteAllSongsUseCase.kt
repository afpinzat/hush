package com.pinza.hush.domain.usecase.song

import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class DeleteAllSongsUseCase @Inject constructor(
    private val repository: ISongRepository
) {

    suspend operator fun invoke() {
        repository.deleteAll()
    }

}