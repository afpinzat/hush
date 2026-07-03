package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IPlayerStateRepository
import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.domain.usecase.song.GetSongsUseCase
import com.pinza.hush.domain.usecase.song.ScanSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getSongsUseCase: GetSongsUseCase,
    private val scanSongsUseCase: ScanSongsUseCase,
    private val songRepository: ISongRepository,
    private val playerManager: IPlayerManager,
    private val playerStateRepository: IPlayerStateRepository
) : ViewModel() {

    private val TAG = "LibraryViewModel"

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _filteredSongs = MutableStateFlow<List<Song>>(emptyList())
    val filteredSongs: StateFlow<List<Song>> = _filteredSongs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentSongId = MutableStateFlow<Int?>(null)
    val currentSongId: StateFlow<Int?> = _currentSongId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongTitle = MutableStateFlow("")
    val currentSongTitle: StateFlow<String> = _currentSongTitle.asStateFlow()

    private val _currentArtist = MutableStateFlow("")
    val currentArtist: StateFlow<String> = _currentArtist.asStateFlow()

    private var isRestored = false

    init {
        loadSongs()
        restoreStateOnce()
        observePlayerState()
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getSongsUseCase().collect { songList ->
                    _songs.value = songList
                    _filteredSongs.value = songList
                    _isLoading.value = false
                    android.util.Log.d(TAG, "📋 Canciones cargadas: ${songList.size}")
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadSongsOrScanIfEmpty() {
        viewModelScope.launch {
            val existing = songRepository.getSongs().first()
            if (existing.isEmpty()) {
                scanSongs()
            } else {
                loadSongs()
            }
        }
    }

    private fun restoreStateOnce() {
        viewModelScope.launch {
            try {
                val state = playerStateRepository.getPlayerState().first()
                state?.let { playerState ->
                    playerState.currentSongId?.let { songId ->
                        val song = getSongById(songId)
                        if (song != null) {
                            android.util.Log.d(TAG, "♻️ Restaurando: ${song.title}")
                            // ✅ Restaurar con cola completa
                            val allSongs = _songs.value
                            val index = allSongs.indexOf(song)
                            if (index != -1 && allSongs.size > 1) {
                                playerManager.setQueue(allSongs, index)
                            } else {
                                playerManager.play(song)
                            }
                            playerManager.seekTo(playerState.currentPosition.toLong())
                            if (playerState.isPlaying) {
                                playerManager.resume()
                            }
                            _currentSongId.value = songId
                            _isPlaying.value = playerState.isPlaying
                            _currentSongTitle.value = song.title
                            _currentArtist.value = song.artist
                            isRestored = true
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error restaurando: ${e.message}")
            }
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            combine(
                playerManager.currentSongState,
                playerManager.isPlayingState
            ) { song, isPlaying ->
                Pair(song, isPlaying)
            }.collect { (song, isPlaying) ->
                _currentSongId.value = song?.id
                _isPlaying.value = isPlaying
                _currentSongTitle.value = song?.title ?: ""
                _currentArtist.value = song?.artist ?: ""
            }
        }
    }

    fun refreshCurrentSong() {
        val song = playerManager.currentSongState.value
        _currentSongId.value = song?.id
        _isPlaying.value = playerManager.isPlaying()
        _currentSongTitle.value = song?.title ?: ""
        _currentArtist.value = song?.artist ?: ""
    }

    fun scanSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                scanSongsUseCase()
                loadSongs()
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun searchSongs(query: String) {
        _searchQuery.value = query
        filterSongs(query)
    }

    private fun filterSongs(query: String) {
        if (query.isEmpty()) {
            _filteredSongs.value = _songs.value
        } else {
            _filteredSongs.value = _songs.value.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                        song.artist.contains(query, ignoreCase = true)
            }
        }
    }

    // ✅ PLAY SONG - ESTABLECER COLA COMPLETA
    fun playSong(song: Song) {
        val songs = _filteredSongs.value
        val index = songs.indexOfFirst { it.id == song.id }
        playerManager.setQueue(songs, if (index >= 0) index else 0)
        _currentSongTitle.value = song.title
        _currentArtist.value = song.artist
        viewModelScope.launch { saveState() }
    }

    fun nextSong() {
        android.util.Log.d(TAG, "⏭️ nextSong()")
        playerManager.next()
        viewModelScope.launch { saveState() }
    }

    fun previousSong() {
        android.util.Log.d(TAG, "⏮️ previousSong()")
        playerManager.previous()
        viewModelScope.launch { saveState() }
    }

    fun togglePlayPause() {
        android.util.Log.d(TAG, "⏯️ togglePlayPause()")
        if (playerManager.isPlaying()) {
            playerManager.pause()
        } else {
            playerManager.resume()
        }
        viewModelScope.launch { saveState() }
    }

    private suspend fun saveState() {
        try {
            val currentSong = playerManager.currentSongState.value
            currentSong?.let { song ->
                playerStateRepository.save(
                    PlayerState(
                        currentSongId = song.id,
                        isPlaying = playerManager.isPlaying(),
                        currentPosition = playerManager.currentPosition().toInt(),
                        playbackSpeed = 1f,
                        queueIds = "",
                        queueIndex = -1
                    )
                )
                android.util.Log.d(TAG, "💾 Estado guardado: ${song.title}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error guardando: ${e.message}")
        }
    }

    fun getSongById(songId: Int): Song? {
        return _songs.value.find { it.id == songId }
    }

    fun clearError() {
        _error.value = null
    }
}