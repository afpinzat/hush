// PlaylistViewModel.kt
package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.domain.usecase.playlist.CreatePlaylistUseCase
import com.pinza.hush.domain.usecase.playlist.DeletePlaylistUseCase
import com.pinza.hush.domain.usecase.playlist.GetPlaylistsUseCase
import com.pinza.hush.domain.usecase.playlist.UpdatePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getPlaylistsUseCase().collect { playlistList ->
                    _playlists.value = playlistList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _error.value = "El nombre no puede estar vacío"
                return@launch
            }

            _isLoading.value = true
            try {
                val playlist = Playlist(
                    name = name,
                    createdAt = System.currentTimeMillis(),
                    songCount = 0
                )
                createPlaylistUseCase(playlist)
                _showCreateDialog.value = false
                loadPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                deletePlaylistUseCase(playlist)
                loadPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                updatePlaylistUseCase(playlist)
                loadPlaylists()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun showCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }

    fun clearError() {
        _error.value = null
    }

    fun getPlaylistById(id: Int): Playlist? {
        return _playlists.value.find { it.id == id }
    }
}