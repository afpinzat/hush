package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.domain.repository.ISongLyricsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SongLyricsRepository @Inject constructor(
    private val dao: SongLyricsDao
) : ISongLyricsRepository {

    override fun getLyrics(songId: Int): Flow<SongLyrics?> =
        dao.getLyrics(songId)

    override suspend fun insert(lyrics: SongLyrics) =
        dao.insert(lyrics)

    override suspend fun update(lyrics: SongLyrics) =
        dao.update(lyrics)

    override suspend fun delete(lyrics: SongLyrics) =
        dao.delete(lyrics)
}