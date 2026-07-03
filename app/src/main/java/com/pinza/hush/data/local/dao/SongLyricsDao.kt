package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.SongLyrics
import kotlinx.coroutines.flow.Flow

@Dao
interface SongLyricsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lyrics: SongLyrics)

    @Update
    suspend fun update(lyrics: SongLyrics)

    @Delete
    suspend fun delete(lyrics: SongLyrics)

    @Query("SELECT * FROM song_lyrics WHERE songId = :songId")
    fun getLyrics(songId: Int): Flow<SongLyrics?>

    @Query("DELETE FROM song_lyrics WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Int)
}