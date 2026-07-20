package com.pinza.hush.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.usecase.library.*
import com.pinza.hush.domain.usecase.player.PlayQueueUseCase
import com.pinza.hush.domain.usecase.player.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLibrarySongsUseCase: GetLibrarySongsUseCase,
    private val getFavoriteSongsUseCase: GetFavoriteSongsUseCase,
    private val getAlbumsUseCase: GetAlbumsUseCase,
    private val getArtistsUseCase: GetArtistsUseCase,
    private val scanMusicUseCase: ScanMusicUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playQueueUseCase: PlayQueueUseCase,
    private val playerManager: IPlayerManager,
    private val songRepository: ISongRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Optimización: antes este flow era "frío", así que cada uno de los 4 combine()
    // de abajo ejecutaba su PROPIO debounce(300) + distinctUntilChanged() por separado
    // (4 timers de debounce corriendo en paralelo sobre la misma búsqueda).
    // Con stateIn() se calcula UNA sola vez y se comparte entre los 4 consumidores.
    private val debouncedSearch: StateFlow<String> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // 1. Lista de canciones (Solo activa si se observa)
    val songs: StateFlow<List<Song>> = getLibrarySongsUseCase()
        .combine(debouncedSearch) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Favoritos
    val favoriteSongs: StateFlow<List<Song>> = getFavoriteSongsUseCase()
        .combine(debouncedSearch) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Álbumes
    val albums: StateFlow<List<SongDao.AlbumSummary>> = getAlbumsUseCase()
        .combine(debouncedSearch) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.album.contains(query, ignoreCase = true) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Artistas
    val artists: StateFlow<List<SongDao.ArtistSummary>> = getArtistsUseCase()
        .combine(debouncedSearch) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.artist.contains(query, ignoreCase = true) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val currentSongState = playerManager.currentSongState

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun scanMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                scanMusicUseCase()
            } finally {
                _isLoading.value = false
            }
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

    suspend fun getLyrics(songId: Long): String? = songRepository.getLyrics(songId)

    suspend fun getSongLyrics(songId: Long): SongLyrics? = songRepository.getSongLyrics(songId)

    fun saveLyrics(songId: Long, lyrics: String) {
        viewModelScope.launch {
            songRepository.saveLyrics(songId, lyrics)
        }
    }

    fun saveSongLyrics(lyrics: SongLyrics) {
        viewModelScope.launch {
            songRepository.saveSongLyrics(lyrics)
        }
    }
}