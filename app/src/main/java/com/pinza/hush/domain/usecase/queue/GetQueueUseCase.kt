package com.pinza.hush.domain.usecase.queue

import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IQueueRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQueueUseCase @Inject constructor(
    private val queueRepository: IQueueRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return queueRepository.getQueue()
    }
}