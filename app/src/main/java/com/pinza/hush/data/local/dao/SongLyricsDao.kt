package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.SongLyrics
import kotlinx.coroutines.flow.Flow

@Dao
interface SongLyricsDao {

    // Inserción directa (si ya existe, reemplaza)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lyrics: SongLyrics)

    // Consulta para el UseCase (versión suspend, rápida)
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId")
    suspend fun getLyricsSingle(songId: Long): SongLyrics?

    // Consulta para la UI (versión Flow, reactiva)
    @Query("SELECT * FROM song_lyrics WHERE songId = :songId")
    fun getLyricsFlow(songId: Long): Flow<SongLyrics?>

    // Limpieza
    @Query("DELETE FROM song_lyrics WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Long)
}