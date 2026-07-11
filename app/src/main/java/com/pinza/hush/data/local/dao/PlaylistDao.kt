package com.pinza.hush.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSongCrossRef
import com.pinza.hush.data.local.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists")
    fun getPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_join WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("SELECT COUNT(*) FROM playlist_song_join WHERE playlistId = :playlistId")
    suspend fun getSongCountInPlaylist(playlistId: Long): Int

        @Transaction // Necesario cuando usas @Relation
        @Query("SELECT * FROM playlists WHERE id = :playlistId")
        fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs>


}