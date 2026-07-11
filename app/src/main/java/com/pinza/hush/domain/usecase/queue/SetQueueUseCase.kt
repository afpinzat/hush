package com.pinza.hush.domain.usecase.queue

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IQueueRepository
import javax.inject.Inject

class SetQueueUseCase @Inject constructor(
    private val queueRepository: IQueueRepository
) {
    suspend operator fun invoke(songs: List<Song>) {
        queueRepository.clearQueue()
        queueRepository.insertAll(songs)
    }
}