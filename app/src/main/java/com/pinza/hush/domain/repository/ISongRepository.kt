package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.Flow

interface ISongRepository {

    fun getSongs(): Flow<List<Song>>

    suspend fun getSong(id: Int): Song?

    suspend fun insert(song: Song)

    suspend fun insertAll(songs: List<Song>)

    suspend fun update(song: Song)

    suspend fun delete(song: Song)

    suspend fun deleteAll()

}