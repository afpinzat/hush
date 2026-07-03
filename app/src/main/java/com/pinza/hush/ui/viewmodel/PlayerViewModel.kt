// PlayerViewModel.kt
package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IPlayerStateRepository
import com.pinza.hush.domain.repository.ISongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: IPlayerManager,
    private val playerStateRepository: IPlayerStateRepository,
    private val songRepository: ISongRepository
) : ViewModel() {

    // ─── ESTADO DEL REPRODUCTOR ────────────────────────────────

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // ─── COLA DE REPRODUCCIÓN (es una lista de canciones) ──────

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // ─── MODOS ──────────────────────────────────────────────────

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: none, 1: one, 2: all
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // ─── ESTADOS DE UI ──────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var originalQueue: List<Song> = emptyList()
    private var isShuffled = false

    init {
        loadPlayerState()
    }

    // ─── CARGAR ESTADO GUARDADO ────────────────────────────────

    private fun loadPlayerState() {
        viewModelScope.launch {
            playerStateRepository.getPlayerState().collect { state ->
                state?.let {
                    _isPlaying.value = it.isPlaying
                    _currentPosition.value = it.currentPosition
                    _playbackSpeed.value = it.playbackSpeed

                    it.currentSongId?.let { songId ->
                        loadSong(songId)
                    }
                }
            }
        }
    }

    private suspend fun loadSong(songId: Int) {
        val song = songRepository.getSong(songId)
        _currentSong.value = song
        _duration.value = song?.duration ?: 0
    }

    private fun savePlayerState() {
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                playerStateRepository.save(
                    PlayerState(
                        currentSongId = song.id,
                        isPlaying = _isPlaying.value,
                        currentPosition = _currentPosition.value,
                        playbackSpeed = _playbackSpeed.value
                    )
                )
            }
        }
    }

    // ─── CONTROLES BÁSICOS ─────────────────────────────────────

    fun playSong(song: Song) {
        viewModelScope.launch {
            playerManager.play(song)
            _currentSong.value = song
            _isPlaying.value = true
            _duration.value = song.duration

            // Si la canción no está en la cola, agregarla
            if (!_queue.value.contains(song)) {
                addToQueue(song)
                _currentQueueIndex.value = _queue.value.indexOf(song)
            } else {
                _currentQueueIndex.value = _queue.value.indexOf(song)
            }

            savePlayerState()
        }
    }

    fun playSongFromQueue(song: Song) {
        val index = _queue.value.indexOf(song)
        if (index != -1) {
            _currentQueueIndex.value = index
            playSong(song)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        playerManager.pause()
        _isPlaying.value = false
        savePlayerState()
    }

    fun resume() {
        if (_currentSong.value != null) {
            playerManager.resume()
            _isPlaying.value = true
            savePlayerState()
        }
    }

    fun stop() {
        playerManager.stop()
        _isPlaying.value = false
        _currentPosition.value = 0
        savePlayerState()
    }

    fun seekTo(position: Int) {
        playerManager.seekTo(position.toLong())
        _currentPosition.value = position
        savePlayerState()
    }

    fun updateProgress() {
        _currentPosition.value = playerManager.currentPosition().toInt()
        if (_currentSong.value != null) {
            _duration.value = playerManager.duration().toInt()
        }
    }

    // ─── NAVEGACIÓN EN COLA ────────────────────────────────────

    fun next() {
        if (_queue.value.isEmpty()) return

        when (_repeatMode.value) {
            1 -> {
                // Repeat one: misma canción
                _currentSong.value?.let { playSong(it) }
            }
            2 -> {
                // Repeat all: siguiente o volver al principio
                val nextIndex = if (_currentQueueIndex.value + 1 >= _queue.value.size) {
                    0
                } else {
                    _currentQueueIndex.value + 1
                }
                _currentQueueIndex.value = nextIndex
                _queue.value[nextIndex].let { playSong(it) }
            }
            else -> {
                // Sin repetición: siguiente o detener
                if (_currentQueueIndex.value + 1 < _queue.value.size) {
                    _currentQueueIndex.value = _currentQueueIndex.value + 1
                    _queue.value[_currentQueueIndex.value].let { playSong(it) }
                } else {
                    pause()
                }
            }
        }
    }

    fun previous() {
        if (_queue.value.isEmpty()) return

        when (_repeatMode.value) {
            2 -> {
                // Repeat all: ir al final si estamos al principio
                if (_currentQueueIndex.value <= 0) {
                    _currentQueueIndex.value = _queue.value.size - 1
                    _queue.value.last().let { playSong(it) }
                } else {
                    _currentQueueIndex.value = _currentQueueIndex.value - 1
                    _queue.value[_currentQueueIndex.value].let { playSong(it) }
                }
            }
            else -> {
                if (_currentQueueIndex.value > 0) {
                    _currentQueueIndex.value = _currentQueueIndex.value - 1
                    _queue.value[_currentQueueIndex.value].let { playSong(it) }
                } else {
                    // Volver al principio
                    _currentQueueIndex.value = 0
                    _queue.value.first().let { playSong(it) }
                }
            }
        }
    }

    // ─── GESTIÓN DE COLA ───────────────────────────────────────

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        originalQueue = songs
        _queue.value = songs
        _currentQueueIndex.value = startIndex
        isShuffled = false

        if (songs.isNotEmpty() && startIndex < songs.size) {
            playSong(songs[startIndex])
        }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        if (!currentQueue.contains(song)) {
            currentQueue.add(song)
            _queue.value = currentQueue
            originalQueue = currentQueue
        }
    }

    fun addAllToQueue(songs: List<Song>) {
        val currentQueue = _queue.value.toMutableList()
        val newSongs = songs.filter { !currentQueue.contains(it) }
        if (newSongs.isNotEmpty()) {
            currentQueue.addAll(newSongs)
            _queue.value = currentQueue
            originalQueue = currentQueue
        }
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index < currentQueue.size) {
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            originalQueue = currentQueue

            if (index == _currentQueueIndex.value) {
                // Si eliminamos la canción actual, detener o pasar a la siguiente
                if (index < _queue.value.size) {
                    _queue.value[index].let { playSong(it) }
                } else if (_queue.value.isNotEmpty()) {
                    _queue.value.last().let { playSong(it) }
                } else {
                    stop()
                }
            } else if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value = _currentQueueIndex.value - 1
            }
        }
    }

    fun removeFromQueue(song: Song) {
        val index = _queue.value.indexOf(song)
        if (index != -1) {
            removeFromQueue(index)
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentQueueIndex.value = -1
        stop()
    }

    fun moveQueueItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return

        val currentQueue = _queue.value.toMutableList()
        val item = currentQueue.removeAt(fromPosition)
        currentQueue.add(toPosition, item)
        _queue.value = currentQueue
        originalQueue = currentQueue

        // Actualizar índice actual
        when {
            _currentQueueIndex.value == fromPosition -> {
                _currentQueueIndex.value = toPosition
            }
            _currentQueueIndex.value > fromPosition && _currentQueueIndex.value <= toPosition -> {
                _currentQueueIndex.value = _currentQueueIndex.value - 1
            }
            _currentQueueIndex.value < fromPosition && _currentQueueIndex.value >= toPosition -> {
                _currentQueueIndex.value = _currentQueueIndex.value + 1
            }
        }
    }

    fun getQueueSize(): Int = _queue.value.size

    fun getCurrentQueuePosition(): Int = _currentQueueIndex.value

    fun getQueueSongAt(index: Int): Song? {
        return if (index < _queue.value.size) _queue.value[index] else null
    }

    fun isQueueEmpty(): Boolean = _queue.value.isEmpty()

    fun hasNext(): Boolean {
        return when (_repeatMode.value) {
            1 -> true // Repeat one siempre tiene siguiente
            2 -> true // Repeat all siempre tiene siguiente
            else -> _currentQueueIndex.value + 1 < _queue.value.size
        }
    }

    fun hasPrevious(): Boolean {
        return _currentQueueIndex.value > 0 || _repeatMode.value == 2
    }

    // ─── MODOS DE REPRODUCCIÓN ─────────────────────────────────

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        if (_shuffleEnabled.value) {
            shuffleQueue()
        } else {
            unshuffleQueue()
        }
    }

    private fun shuffleQueue() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        val currentSong = _currentSong.value
        val shuffled = currentQueue.toMutableList().apply {
            shuffle()
            // Mover la canción actual al principio
            currentSong?.let { song ->
                val index = indexOf(song)
                if (index != -1) {
                    removeAt(index)
                    add(0, song)
                }
            }
        }
        _queue.value = shuffled
        _currentQueueIndex.value = 0
        isShuffled = true
    }

    private fun unshuffleQueue() {
        if (isShuffled && originalQueue.isNotEmpty()) {
            val currentSong = _currentSong.value
            val newQueue = originalQueue.toMutableList()
            // Mover la canción actual al principio
            currentSong?.let { song ->
                val index = newQueue.indexOf(song)
                if (index != -1) {
                    newQueue.removeAt(index)
                    newQueue.add(0, song)
                }
            }
            _queue.value = newQueue
            _currentQueueIndex.value = 0
            isShuffled = false
        }
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            0 -> 1 // repeat one
            1 -> 2 // repeat all
            else -> 0 // no repeat
        }
    }

    fun getRepeatModeText(): String {
        return when (_repeatMode.value) {
            0 -> "Sin repetición"
            1 -> "Repetir una"
            2 -> "Repetir todas"
            else -> ""
        }
    }

    // ─── VELOCIDAD ─────────────────────────────────────────────

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        // TODO: Implementar cambio de velocidad en ExoPlayer
        savePlayerState()
    }

    fun getPlaybackSpeedOptions(): List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    // ─── UTILIDADES ────────────────────────────────────────────

    fun clearError() {
        _error.value = null
    }
}