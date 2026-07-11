package com.pinza.hush.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.PlaylistWithSongs
import com.pinza.hush.domain.repository.IPlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDialogViewModel @Inject constructor(
    private val repository: IPlaylistRepository
) : ViewModel() {

    // Cambiamos a StateFlow con valor inicial para que el diálogo lo lea inmediatamente
    private val _playlists = MutableStateFlow<List<PlaylistWithSongs>>(emptyList())
    val playlists = _playlists.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllPlaylists().collect {
                _playlists.value = it
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: Long) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addSongToPlaylist(id, songId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }
}
