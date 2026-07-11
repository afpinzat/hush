package com.pinza.hush.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IPlaylistRepository
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.usecase.library.GetAlbumsUseCase
import com.pinza.hush.domain.usecase.library.GetArtistsUseCase
import com.pinza.hush.domain.usecase.library.GetLibrarySongsUseCase
import com.pinza.hush.domain.usecase.library.GetSongsByAlbumUseCase
import com.pinza.hush.domain.usecase.library.GetSongsByArtistUseCase
import com.pinza.hush.domain.usecase.library.ScanMusicUseCase
import com.pinza.hush.domain.usecase.player.PlayQueueUseCase
import com.pinza.hush.domain.usecase.player.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val albums: List<SongDao.AlbumSummary> = emptyList(),
    val artists: List<SongDao.ArtistSummary> = emptyList(),
    val detailSongs: List<Song> = emptyList(),
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibrarySongsUseCase: GetLibrarySongsUseCase,
    private val getFavoriteSongsUseCase: com.pinza.hush.domain.usecase.library.GetFavoriteSongsUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getArtistsUseCase: GetArtistsUseCase,
    private val getSongsByAlbumUseCase: GetSongsByAlbumUseCase,
    private val getSongsByArtistUseCase: GetSongsByArtistUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playQueueUseCase: PlayQueueUseCase,
    private val playerManager: IPlayerManager,
    private val playlistRepository: IPlaylistRepository,
    private val songRepository: ISongRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = false))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val currentSongState = playerManager.currentSongState

    private var isScanning = false

    init {
        loadLibraryData()
    }

    private fun loadLibraryData() {
        viewModelScope.launch {
            // Combinamos las fuentes de datos con la query de búsqueda
            combine(
                getLibrarySongsUseCase(),
                getFavoriteSongsUseCase(),
                getAlbumsUseCase(),
                getArtistsUseCase(),
                _searchQuery.debounce(200).distinctUntilChanged()
            ) { songs, favorites, albums, artists, query ->
                val filteredSongs = if (query.isBlank()) songs else {
                    songs.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
                }
                val filteredFavorites = if (query.isBlank()) favorites else {
                    favorites.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
                }
                val filteredAlbums = if (query.isBlank()) albums else {
                    albums.filter { it.album.contains(query, ignoreCase = true) }
                }
                val filteredArtists = if (query.isBlank()) artists else {
                    artists.filter { it.artist.contains(query, ignoreCase = true) }
                }

                // Devolvemos el estado parcial para actualizarlo en el collect
                FilteredResults(filteredSongs, filteredFavorites, filteredAlbums, filteredArtists)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { results ->
                _uiState.update { it.copy(
                    songs = results.songs,
                    favoriteSongs = results.favorites,
                    albums = results.albums,
                    artists = results.artists,
                    isLoading = false
                ) }
            }
        }
    }

    private data class FilteredResults(
        val songs: List<Song>,
        val favorites: List<Song>,
        val albums: List<SongDao.AlbumSummary>,
        val artists: List<SongDao.ArtistSummary>
    )

    fun play(song: Song) {
        viewModelScope.launch {
            playSongUseCase(song)
        }
    }

    fun playWithQueue(songs: List<Song>, startIndex: Int) {
        viewModelScope.launch {
            playQueueUseCase(songs, startIndex)
        }
    }

    fun addToQueueNext(song: Song) {
        playerManager.addNext(song)
    }

    fun scanMusic() {
        if (isScanning) return
        
        viewModelScope.launch {
            isScanning = true
            // Solo mostramos carga si la lista está vacía. 
            // Si ya hay canciones, el escaneo es en "segundo plano" (sin bloquear UI).
            if (_uiState.value.songs.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            }
            
            try {
                scanMusicUseCase()
            } finally {
                isScanning = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    fun loadDetail(name: String, type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val flow = if (type == "album") {
                getSongsByAlbumUseCase(name)
            } else {
                getSongsByArtistUseCase(name)
            }
            
            flow.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { songs ->
                _uiState.update { it.copy(isLoading = false, detailSongs = songs) }
            }
        }
    }

    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            playlistRepository.getPlaylistWithSongs(playlistId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { result ->
                    _uiState.update { it.copy(isLoading = false, detailSongs = result.songs) }
                }
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch {
            songRepository.updateSong(song)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            songRepository.delete(song)
        }
    }

    suspend fun getLyrics(songId: Long): String? {
        return songRepository.getLyrics(songId)
    }

    fun saveLyrics(songId: Long, lyrics: String) {
        viewModelScope.launch {
            songRepository.saveLyrics(songId, lyrics)
        }
    }
}
