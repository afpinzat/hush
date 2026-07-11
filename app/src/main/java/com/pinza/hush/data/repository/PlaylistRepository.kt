package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.dao.PlaylistDao
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSongCrossRef
import kotlinx.coroutines.flow.Flow
import com.pinza.hush.data.local.model.PlaylistWithSongs
import com.pinza.hush.domain.repository.IPlaylistRepository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) : IPlaylistRepository{

    // Obtener todas las playlists con sus canciones relacionadas
    override fun getAllPlaylists() = playlistDao.getPlaylistsWithSongs()

    // Crear una nueva playlist
    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    // Agregar una canción a una playlist
    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        // Obtenemos el conteo actual para asignar la siguiente posición
        val currentCount = playlistDao.getSongCountInPlaylist(playlistId)
        playlistDao.insertCrossRef(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = currentCount
            )
        )
    }

    // Eliminar una canción específica de una playlist
    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Eliminar una playlist completa (en cascada por ForeignKey)
    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    // Renombrar playlist
    override suspend fun updatePlaylistName(playlistId: Long, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }

    override fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs> {
        // Room nos devuelve un Flow automáticamente, lo pasamos tal cual
        return playlistDao.getPlaylistWithSongs(playlistId)
    }

}