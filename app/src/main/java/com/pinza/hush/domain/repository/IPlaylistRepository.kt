// IPlaylistRepository.kt
package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

interface IPlaylistRepository {
    // Playlist CRUD
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Int): Playlist?
    suspend fun insert(playlist: Playlist): Long
    suspend fun update(playlist: Playlist)
    suspend fun delete(playlist: Playlist)

    // Playlist-Song relations
    suspend fun addSong(item: PlaylistSong)
    suspend fun removeSong(item: PlaylistSong)
    suspend fun getPlaylistSongs(playlistId: Int): List<PlaylistSong>  // ← Este método falta
    suspend fun getSongsInPlaylist(playlistId: Int): List<PlaylistSong>
    suspend fun updateSongPosition(playlistId: Int, songId: Int, newPosition: Int)
    suspend fun getPlaylistSongCount(playlistId: Int): Int
}