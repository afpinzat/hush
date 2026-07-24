package com.pinza.hush.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistWithSongs
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.usecase.playlist.GetAllPlaylistsUseCase
import com.pinza.hush.domain.usecase.playlist.GetPlaylistWithSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val title: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PlaylistUiState(
    val playlists: List<PlaylistWithSongs> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistWithSongsUseCase: GetPlaylistWithSongsUseCase,
    private val getAllPlaylistsUseCase: GetAllPlaylistsUseCase,
    private val repository: IPlaylistRepository
) : ViewModel() {

    val playlistsState: StateFlow<PlaylistUiState> = getAllPlaylistsUseCase()
        .map { list ->
            PlaylistUiState(
                playlists = list,
                isLoading = false
            )
        }
        .catch { e ->
            emit(PlaylistUiState(error = e.message, isLoading = false))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaylistUiState(isLoading = true)
        )

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
        }
    }

    // Canal de entrada para el ID
    private val _playlistId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlaylistDetailUiState> = _playlistId
        .filterNotNull()
        .flatMapLatest { id ->
            getPlaylistWithSongsUseCase(id)
                .map { result ->
                    PlaylistDetailUiState(
                        title = result.playlist.name,
                        songs = result.songs,
                        isLoading = false
                    )
                }
                .catch { e ->
                    emit(PlaylistDetailUiState(error = e.message, isLoading = false))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaylistDetailUiState(isLoading = true)
        )

    // Llamas a esto desde tu UI al navegar a la pantalla
    fun setPlaylistId(id: Long) {
        _playlistId.value = id
    }
}
