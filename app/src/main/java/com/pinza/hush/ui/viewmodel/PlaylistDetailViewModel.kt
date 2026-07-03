package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistSong
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.usecase.playlist.AddSongToPlaylistUseCase
import com.pinza.hush.domain.usecase.playlist.RemoveSongFromPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: IPlaylistRepository,
    private val songRepository: ISongRepository,
    private val addSongUseCase: AddSongToPlaylistUseCase,
    private val removeSongUseCase: RemoveSongFromPlaylistUseCase,
    private val playerManager: IPlayerManager
) : ViewModel() {

    private val _playlist = MutableStateFlow<Playlist?>(null)
    val playlist: StateFlow<Playlist?> = _playlist.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showAddSongDialog = MutableStateFlow(false)
    val showAddSongDialog: StateFlow<Boolean> = _showAddSongDialog.asStateFlow()

    private var playlistId: Int = 0
    private var cachedPlaylistSongs: List<PlaylistSong> = emptyList()

    fun loadPlaylistDetails(playlistId: Int) {
        this.playlistId = playlistId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val playlist = playlistRepository.getPlaylistById(playlistId)
                _playlist.value = playlist

                songRepository.getSongs().collect { allSongsList ->
                    _allSongs.value = allSongsList
                    loadPlaylistSongs()
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadPlaylistSongs() {
        try {
            cachedPlaylistSongs = playlistRepository.getPlaylistSongs(playlistId)
            val songIds = cachedPlaylistSongs.map { it.songId }

            val songsInPlaylist = _allSongs.value.filter { song ->
                songIds.contains(song.id)
            }.sortedBy { song ->
                cachedPlaylistSongs.find { it.songId == song.id }?.position ?: 0
            }
            _songs.value = songsInPlaylist

            _playlist.value = _playlist.value?.copy(songCount = songsInPlaylist.size)
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun addSongToPlaylist(song: Song) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Verificar si la canción ya está en la playlist
                val existing = cachedPlaylistSongs.find { it.songId == song.id }
                if (existing != null) {
                    _error.value = "La canción ya está en la playlist"
                    _isLoading.value = false
                    return@launch
                }

                val playlistSong = PlaylistSong(
                    playlistId = playlistId,
                    songId = song.id,
                    position = _songs.value.size
                )
                addSongUseCase(playlistSong)
                _showAddSongDialog.value = false
                loadPlaylistSongs()
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun removeSongFromPlaylist(song: Song) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // ✅ Buscar el PlaylistSong correspondiente
                val playlistSong = cachedPlaylistSongs.find { it.songId == song.id }

                if (playlistSong != null) {
                    // ✅ Usamos removeSongUseCase con PlaylistSong (CORRECTO)
                    removeSongUseCase(playlistSong)
                    loadPlaylistSongs()
                } else {
                    _error.value = "Canción no encontrada en la playlist"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun playPlaylist() {
        if (_songs.value.isNotEmpty()) {
            val song = _songs.value.first()
            playerManager.play(song)
        }
    }

    fun playSongFromPlaylist(song: Song) {
        playerManager.play(song)
    }

    fun showAddSongDialog(show: Boolean) {
        _showAddSongDialog.value = show
    }

    fun clearError() {
        _error.value = null
    }

    fun getSongsNotInPlaylist(): List<Song> {
        val songIdsInPlaylist = _songs.value.map { it.id }
        return _allSongs.value.filter { !songIdsInPlaylist.contains(it.id) }
    }

    fun getPlaylistSongCount(): Int = _songs.value.size
}