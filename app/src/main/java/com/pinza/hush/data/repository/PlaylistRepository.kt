package com.pinza.hush.data.local.repository

import com.pinza.hush.data.model.Playlist
import com.pinza.hush.data.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Int): Playlist?
    suspend fun createPlaylist(playlist: Playlist): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlist: Playlist)
    fun getSongsForPlaylist(playlistId: Int): Flow<List<Song>>
    suspend fun addSongToPlaylist(playlistId: Int, songId: Int, position: Int)
    suspend fun removeSongFromPlaylist(playlistId: Int, songId: Int)
    suspend fun reorderSong(playlistId: Int, songId: Int, newPosition: Int)
    suspend fun clearPlaylist(playlistId: Int)
    suspend fun getSongCount(playlistId: Int): Int
}

