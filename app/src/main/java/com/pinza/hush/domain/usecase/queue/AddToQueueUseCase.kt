package com.pinza.hush.domain.usecase.queue

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IQueueRepository
import javax.inject.Inject

class AddToQueueUseCase @Inject constructor(
    private val queueRepository: IQueueRepository
) {
    suspend operator fun invoke(song: Song) {
        // Lógica: añade la canción a la tabla 'queue'
        queueRepository.addToQueue(song)
    }
}