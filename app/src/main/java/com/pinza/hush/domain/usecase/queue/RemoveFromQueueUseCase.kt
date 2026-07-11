package com.pinza.hush.domain.usecase.queue

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IQueueRepository
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class RemoveFromQueueUseCase @Inject constructor(
    private val repository: IQueueRepository
) {
    suspend operator fun invoke(song: Song) {
        // Aquí llamas al repositorio para ejecutar la eliminación
        repository.deleteFromQueue(song)
    }
}