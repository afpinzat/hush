package com.pinza.hush.data.local.repository

import com.pinza.hush.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * SongRepository — INTERFAZ
 *
 * Define el "contrato" de las operaciones disponibles.
 * El ViewModel depende de esta interfaz, no de la implementación concreta.
 *
 * Ventaja: en tests se puede inyectar FakeSongRepository sin Room real.
 */
interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun insertSong(song: Song)
    suspend fun insertAll(songs: List<Song>)
    suspend fun updateSong(song: Song)
    suspend fun deleteSong(song: Song)
    suspend fun deleteAll()
}
