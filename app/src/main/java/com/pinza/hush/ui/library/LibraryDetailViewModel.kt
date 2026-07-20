package com.pinza.hush.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.usecase.library.GetSongsByAlbumUseCase
import com.pinza.hush.domain.usecase.library.GetSongsByArtistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryDetailUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryDetailViewModel @Inject constructor(
    private val getSongsByAlbumUseCase: GetSongsByAlbumUseCase,
    private val getSongsByArtistUseCase: GetSongsByArtistUseCase,
    private val playlistRepository: IPlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryDetailUiState())
    val uiState: StateFlow<LibraryDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(name: String, type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val flow = if (type == "album") {
                getSongsByAlbumUseCase(name)
            } else {
                getSongsByArtistUseCase(name)
            }
            
            flow.catch { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }.collect { songs ->
                _uiState.value = _uiState.value.copy(isLoading = false, songs = songs)
            }
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            playlistRepository.getPlaylistWithSongs(playlistId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .collect { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false, songs = result.songs)
                }
        }
    }
}
