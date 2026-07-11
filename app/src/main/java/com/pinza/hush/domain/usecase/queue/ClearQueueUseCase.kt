package com.pinza.hush.domain.usecase.queue

import com.pinza.hush.domain.repository.IQueueRepository
import javax.inject.Inject

class ClearQueueUseCase @Inject constructor(
    private val queueRepository: IQueueRepository
) {
    suspend operator fun invoke() {
        queueRepository.clearQueue()
    }
}