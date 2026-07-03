package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("SELECT * FROM song ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE id = :id")
    suspend fun getSongById(id: Int): Song?

    @Query("DELETE FROM song")
    suspend fun deleteAll()
}