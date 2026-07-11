package com.pinza.hush.ui.nowplaying

import com.pinza.hush.domain.repository.ISongRepository
import com.pinza.hush.utils.LrcLine
import com.pinza.hush.utils.LrcParser
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.common.Player
import androidx.media3.session.MediaController

data class NowPlayingUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val lyrics: String = "",
    val parsedLyrics: List<LrcLine> = emptyList(),
    val currentLineIndex: Int = -1,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerManager: IPlayerManager,
    private val songRepository: ISongRepository
) : ViewModel() {

    private var mediaController: MediaController? = null

    // 2. Crea una función para que la Activity le pase el controlador cuando esté listo
    fun setMediaController(controller: MediaController) {
        this.mediaController = controller
    }
    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState = _uiState.asStateFlow()

    private var lyricsJob: Job? = null
    private var statusJob: Job? = null

    init {
        observePlaybackChanges()
    }

    private fun observePlaybackChanges() {
        viewModelScope.launch {
            // Observar cambios de canción y su estado de favorito
            launch {
                playerManager.currentSongState.collect { song ->
                    _uiState.update { it.copy(currentSong = song) }
                    song?.let { 
                        observeLyrics(it.id)
                        observeCurrentSongStatus(it.id)
                        viewModelScope.launch {
                            songRepository.checkAndExtractLyrics(it)
                        }
                    }
                }
            }

            // Observar estado de reproducción
            launch {
                playerManager.isPlayingState.collect { isPlaying ->
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }
            }

            // Observar si está cargando (buffering)
            launch {
                playerManager.isBufferingState.collect { isBuffering ->
                    _uiState.update { it.copy(isBuffering = isBuffering) }
                }
            }

            // Observar modo de repetición
            launch {
                playerManager.repeatModeState.collect { mode ->
                    _uiState.update { it.copy(repeatMode = mode) }
                }
            }

            // Actualizar progreso periódicamente
            while (true) {
                if (playerManager.isPlaying()) {
                    val currentPos = playerManager.currentPosition()
                    _uiState.update {
                        it.copy(
                            progress = currentPos,
                            duration = playerManager.duration().coerceAtLeast(0L),
                            currentLineIndex = findCurrentLineIndex(currentPos, it.parsedLyrics)
                        )
                    }
                }
                delay(200) // Más rápido para las letras
            }
        }
    }

    private fun observeLyrics(songId: Long) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            songRepository.getLyricsFlow(songId).collect { lyrics ->
                val content = lyrics ?: ""
                val parsed = LrcParser.parse(content)
                _uiState.update { it.copy(lyrics = content, parsedLyrics = parsed) }
            }
        }
    }

    private fun observeCurrentSongStatus(songId: Long) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            songRepository.getSongById(songId).collect { song ->
                song?.let { updatedSong ->
                    _uiState.update { it.copy(currentSong = updatedSong) }
                }
            }
        }
    }

    private fun findCurrentLineIndex(progress: Long, lines: List<LrcLine>): Int {
        if (lines.isEmpty()) return -1
        var index = -1
        for (i in lines.indices) {
            if (progress >= lines[i].time) {
                index = i
            } else {
                break
            }
        }
        return index
    }

    fun togglePlayPause() {
        if (playerManager.isPlaying()) playerManager.pause() else playerManager.resume()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun skipToNext() {
        playerManager.skipToNext()
    }

    fun skipToPrevious() {
        playerManager.skipToPrevious()
    }

    fun toggleRepeatMode() {
        playerManager.toggleRepeatMode()
    }

    fun playSpecificSong(song: Song) {
        // 1. Crear el MediaItem igual que arriba
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.filePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .build())
            .build()

        // 2. Enviar el MediaItem al MediaController (que está conectado al servicio)
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
    }
}