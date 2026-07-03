package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.PlayerState
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.IPlayerStateRepository
import com.pinza.hush.domain.repository.ISongLyricsRepository
import com.pinza.hush.domain.repository.ISongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val songRepository: ISongRepository,
    private val lyricsRepository: ISongLyricsRepository,
    private val playerStateRepository: IPlayerStateRepository,
    private val playerManager: IPlayerManager
) : ViewModel() {

    private val TAG = "NowPlayingVM"

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private var currentSongId: Int? = null

    init {
        observePlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            combine(
                playerManager.currentSongState,
                playerManager.isPlayingState,
                playerManager.currentPositionState,
                playerManager.durationState
            ) { song, isPlaying, position, duration ->
                NowPlayingState(song, isPlaying, position, duration)
            }.collect { state ->
                state.song?.let {
                    if (currentSongId != it.id) {
                        currentSongId = it.id
                        loadLyrics(it.id)
                        checkFavorite(it.id)
                    }
                }

                val positionSeconds = (state.currentPosition / 1000).toInt()
                val durationSeconds = (state.duration / 1000).toInt()

                _uiState.value = _uiState.value.copy(
                    currentSong = state.song,
                    isPlaying = state.isPlaying,
                    currentPosition = positionSeconds,
                    duration = durationSeconds
                )
            }
        }
    }

    private data class NowPlayingState(
        val song: Song?,
        val isPlaying: Boolean,
        val currentPosition: Long,
        val duration: Long
    )

    suspend fun loadSong(songId: Int) {
        try {
            val song = songRepository.getSong(songId)
            if (song != null) {
                android.util.Log.d(TAG, "✅ Canción cargada: ${song.title}")
                // ✅ Solo reproducir si NO es ya la canción actual (evita pisar la cola)
                if (playerManager.currentSongState.value?.id != songId) {
                    playerManager.play(song)
                }
                currentSongId = song.id
                loadLyrics(song.id)
                checkFavorite(song.id)
            } else {
                android.util.Log.e(TAG, "❌ Canción no encontrada: $songId")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cargando: ${e.message}")
        }
    }

    private suspend fun loadLyrics(songId: Int) {
        try {
            val lyrics = lyricsRepository.getLyrics(songId).first()
            _uiState.value = _uiState.value.copy(
                lyrics = lyrics?.lyrics,
                lyricsSource = lyrics?.source,
                lyricsLanguage = lyrics?.language
            )
            android.util.Log.d(TAG, "✅ Letras cargadas")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cargando letras: ${e.message}")
        }
    }

    private suspend fun checkFavorite(songId: Int) {
        _isFavorite.value = false
    }

    // ─── ACTUALIZAR CANCIÓN ────────────────────────────────────────────

    fun updateSong(updatedSong: Song) {
        viewModelScope.launch {
            try {
                songRepository.update(updatedSong)
                _uiState.value = _uiState.value.copy(currentSong = updatedSong)
                playerManager.play(updatedSong)
                android.util.Log.d(TAG, "✅ Canción actualizada: ${updatedSong.title}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error actualizando: ${e.message}")
            }
        }
    }

    // ─── ELIMINAR CANCIÓN ─────────────────────────────────────────────

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                songRepository.delete(song)
                lyricsRepository.deleteBySongId(song.id)
                playerManager.stop()
                android.util.Log.d(TAG, "✅ Canción eliminada: ${song.title}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error eliminando: ${e.message}")
            }
        }
    }

    // ─── GUARDAR LETRAS ────────────────────────────────────────────────

    fun saveLyrics(lyrics: String) {
        viewModelScope.launch {
            try {
                val song = _uiState.value.currentSong
                if (song == null) {
                    android.util.Log.e(TAG, "❌ No hay canción actual")
                    return@launch
                }

                val existingSong = songRepository.getSong(song.id)
                if (existingSong == null) {
                    android.util.Log.e(TAG, "❌ La canción no existe en la base de datos")
                    return@launch
                }

                val songLyrics = SongLyrics(
                    songId = song.id,
                    lyrics = lyrics,
                    source = "manual",
                    language = "es"
                )
                lyricsRepository.insert(songLyrics)
                _uiState.value = _uiState.value.copy(lyrics = lyrics)
                android.util.Log.d(TAG, "✅ Letras guardadas para: ${song.title}")

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error guardando letras: ${e.message}")
            }
        }
    }

    // ─── ELIMINAR LETRAS ──────────────────────────────────────────────

    fun deleteLyrics() {
        viewModelScope.launch {
            try {
                val song = _uiState.value.currentSong
                if (song == null) {
                    android.util.Log.e(TAG, "❌ No hay canción actual")
                    return@launch
                }

                lyricsRepository.deleteBySongId(song.id)
                _uiState.value = _uiState.value.copy(lyrics = null)
                android.util.Log.d(TAG, "✅ Letras eliminadas para: ${song.title}")

            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error eliminando letras: ${e.message}")
            }
        }
    }

    // ─── CONTROLES DE REPRODUCCIÓN ─────────────────────────────────────

    fun togglePlayPause() {
        if (playerManager.isPlaying()) {
            playerManager.pause()
        } else {
            playerManager.resume()
        }
    }

    fun seekTo(position: Int) {
        playerManager.seekTo(position.toLong() * 1000)
    }

    fun next() {
        android.util.Log.d(TAG, "⏭️ next() llamado")
        playerManager.next()
    }

    fun previous() {
        android.util.Log.d(TAG, "⏮️ previous() llamado")
        playerManager.previous()
    }

    fun toggleFavorite() {
        _isFavorite.value = !_isFavorite.value
        _uiState.value = _uiState.value.copy(isFavorite = _isFavorite.value)
    }

    fun toggleShuffle() {
        _uiState.value = _uiState.value.copy(
            isShuffleEnabled = !_uiState.value.isShuffleEnabled
        )
    }

    fun toggleRepeat() {
        val currentMode = _uiState.value.repeatMode
        _uiState.value = _uiState.value.copy(
            repeatMode = when (currentMode) {
                0 -> 1
                1 -> 2
                else -> 0
            }
        )
    }

    fun updateProgress() {
        // Ya se actualiza automáticamente
    }

    fun addToPlaylist(song: Song, playlistName: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Agregando a playlist: $playlistName")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    data class NowPlayingUiState(
        val currentSong: Song? = null,
        val isPlaying: Boolean = false,
        val currentPosition: Int = 0,
        val duration: Int = 0,
        val playbackSpeed: Float = 1f,
        val lyrics: String? = null,
        val lyricsSource: String? = null,
        val lyricsLanguage: String? = null,
        val isFavorite: Boolean = false,
        val isShuffleEnabled: Boolean = false,
        val repeatMode: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}