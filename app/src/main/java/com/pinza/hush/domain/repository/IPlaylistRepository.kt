// domain/repository/IPlaylistRepository.kt
package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

interface IPlaylistRepository {
    fun getAllPlaylists(): Flow<List<PlaylistWithSongs>>
    suspend fun createPlaylist(name: String): Long
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun updatePlaylistName(playlistId: Long, newName: String)
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs>

}