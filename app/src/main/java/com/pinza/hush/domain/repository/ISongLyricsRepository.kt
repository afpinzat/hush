package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.SongLyrics
import kotlinx.coroutines.flow.Flow

interface ISongLyricsRepository {

    fun getLyrics(songId: Int): Flow<SongLyrics?>

    suspend fun insert(lyrics: SongLyrics)

    suspend fun update(lyrics: SongLyrics)

    suspend fun delete(lyrics: SongLyrics)
    suspend fun deleteBySongId(songId: Int)
}