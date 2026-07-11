package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.dao.QueueItemWithSong
import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.Flow

interface IQueueRepository {

    fun getQueue(): Flow<List<Song>>
    suspend fun setQueue(songs: List<Song>)
    suspend fun clearQueue()
    suspend fun insertAll(songs: List<Song>)
    suspend fun addToQueue(song: Song)
    suspend fun deleteFromQueue(song: Song)
}
