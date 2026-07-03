package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.ISongRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SongRepository @Inject constructor(
    private val dao: SongDao
) : ISongRepository {

    override fun getSongs(): Flow<List<Song>> =
        dao.getAllSongs()

    override suspend fun getSong(id: Int): Song? =
        dao.getSongById(id)

    override suspend fun insert(song: Song) =
        dao.insert(song)

    override suspend fun insertAll(songs: List<Song>) =
        dao.insertAll(songs)

    override suspend fun update(song: Song) =
        dao.update(song)

    override suspend fun delete(song: Song) =
        dao.delete(song)

    override suspend fun deleteAll() =
        dao.deleteAll()
}