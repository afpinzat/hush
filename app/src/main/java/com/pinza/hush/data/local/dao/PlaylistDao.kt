// PlaylistDao.kt
package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // ─── PLAYLIST CRUD ─────────────────────────────────────────

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlist ORDER BY name")
    fun getPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getPlaylistById(id: Int): Playlist?

    @Query("DELETE FROM playlist")
    suspend fun deleteAllPlaylists()

    // ─── PLAYLIST-SONG RELATIONS ──────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(item: PlaylistSong)

    @Delete
    suspend fun removeSongFromPlaylist(item: PlaylistSong)

    @Query("SELECT * FROM playlist_song WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongs(playlistId: Int): List<PlaylistSong>

    @Query("SELECT * FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun getPlaylistSong(playlistId: Int, songId: Int): PlaylistSong?

    @Query("SELECT COUNT(*) FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Int): Int

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Int)

    @Query("UPDATE playlist_song SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Int, songId: Int, position: Int)

    @Query("SELECT * FROM playlist_song WHERE playlistId = :playlistId AND position >= :fromPosition")
    suspend fun getSongsFromPosition(playlistId: Int, fromPosition: Int): List<PlaylistSong>

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Int, songId: Int)

    @Query("SELECT MAX(position) FROM playlist_song WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: Int): Int?
}