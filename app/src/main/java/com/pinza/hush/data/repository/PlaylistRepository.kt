// PlaylistRepository.kt
package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSong
import com.pinza.hush.domain.repository.IPlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PlaylistRepository @Inject constructor(
    private val dao: PlaylistDao
) : IPlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> =
        dao.getPlaylists()

    override suspend fun getPlaylistById(id: Int): Playlist? =
        dao.getPlaylistById(id)

    override suspend fun insert(playlist: Playlist): Long =
        dao.insertPlaylist(playlist)

    override suspend fun update(playlist: Playlist) =
        dao.updatePlaylist(playlist)

    override suspend fun delete(playlist: Playlist) =
        dao.deletePlaylist(playlist)

    override suspend fun addSong(item: PlaylistSong) =
        dao.addSongToPlaylist(item)

    override suspend fun removeSong(item: PlaylistSong) =
        dao.removeSongFromPlaylist(item)

    override suspend fun getPlaylistSongs(playlistId: Int): List<PlaylistSong> =
        dao.getPlaylistSongs(playlistId)  // ← Este método existe en PlaylistDao

    override suspend fun getSongsInPlaylist(playlistId: Int): List<PlaylistSong> =
        dao.getPlaylistSongs(playlistId)

    override suspend fun updateSongPosition(playlistId: Int, songId: Int, newPosition: Int) {
        // Implementar si es necesario
    }

    override suspend fun getPlaylistSongCount(playlistId: Int): Int {
        return dao.getPlaylistSongs(playlistId).size
    }
}