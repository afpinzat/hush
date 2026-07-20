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

import kotlinx.coroutines.flow.combine
import coil.imageLoader
import coil.request.ImageRequest
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class NowPlayingUiState(
    val currentSong: Song? = null,
    val nextSong: Song? = null,
    val previousSong: Song? = null,
    val isPlaying: Boolean = false,
    val progress: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val lyrics: String = "",
    val lyrics2: String = "",
    val isPrimarySecond: Boolean = false,
    val parsedLyrics: List<LrcLine> = emptyList(),
    val currentLineIndex: Int = -1,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val showTranslation: Boolean = false
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerManager: IPlayerManager,
    private val songRepository: ISongRepository,
    @ApplicationContext private val context: Context
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
            // Observar cambios de canción y precargar adyacentes
            launch {
                combine(
                    playerManager.currentSongState,
                    playerManager.currentQueueState
                ) { currentSong, queue ->
                    Triple(currentSong, queue, findAdjacents(currentSong, queue))
                }.collect { (currentSong, queue, adjacents) ->
                    val (prev, next) = adjacents
                    
                    _uiState.update { 
                        if (it.currentSong?.id != currentSong?.id || 
                            it.currentSong?.isFavorite != currentSong?.isFavorite ||
                            it.nextSong?.id != next?.id ||
                            it.previousSong?.id != prev?.id) {
                            it.copy(
                                currentSong = currentSong,
                                nextSong = next,
                                previousSong = prev
                            )
                        } else it
                    }

                    // Precarga agresiva de carátulas adyacentes
                    next?.albumArt?.let { preloadImage(it) }
                    prev?.albumArt?.let { preloadImage(it) }

                    currentSong?.let { 
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

            // Actualización de progreso optimizada: Solo emite cuando la música está sonando
            launch {
                playerManager.isPlayingState.collect { isPlaying ->
                    if (isPlaying) {
                        while (playerManager.isPlaying()) {
                            val currentPos = playerManager.currentPosition()
                            val duration = playerManager.duration().coerceAtLeast(0L)
                            val lineIndex = findCurrentLineIndex(currentPos, _uiState.value.parsedLyrics)
                            
                            _uiState.update {
                                if (it.progress != currentPos || it.duration != duration || it.currentLineIndex != lineIndex) {
                                    it.copy(
                                        progress = currentPos,
                                        duration = duration,
                                        currentLineIndex = lineIndex
                                    )
                                } else it
                            }
                            delay(250) // Intervalo balanceado para letras y UI
                        }
                    }
                }
            }
        }
    }

    private fun findAdjacents(current: Song?, queue: List<Song>): Pair<Song?, Song?> {
        if (current == null || queue.isEmpty()) return null to null
        val index = queue.indexOfFirst { it.id == current.id }
        if (index == -1) return null to null
        
        val prev = if (index > 0) queue[index - 1] else null
        val next = if (index < queue.size - 1) queue[index + 1] else null
        return prev to next
    }

    private fun preloadImage(url: String) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(800, 800) // Match NowPlaying size
            .build()
        context.imageLoader.enqueue(request)
    }

    private fun observeLyrics(songId: Long) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            songRepository.getSongLyricsFlow(songId).collect { songLyrics ->
                val l1 = songLyrics?.lyrics ?: ""
                val l2 = songLyrics?.lyrics2 ?: ""
                val useSecond = songLyrics?.isPrimarySecond ?: false
                
                val primaryLyrics = if (useSecond && l2.isNotBlank()) l2 else l1
                val parsed = LrcParser.parse(primaryLyrics)
                
                _uiState.update { it.copy(
                    lyrics = l1,
                    lyrics2 = l2,
                    isPrimarySecond = useSecond,
                    parsedLyrics = parsed
                ) }
            }
        }
    }

    private fun observeCurrentSongStatus(songId: Long) {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            songRepository.getSongById(songId).collect { song ->
                song?.let { updatedSong ->
                    _uiState.update { 
                        if (it.currentSong?.id != updatedSong.id || it.currentSong?.isFavorite != updatedSong.isFavorite) {
                            it.copy(currentSong = updatedSong)
                        } else it
                    }
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

    fun toggleTranslation() {
        _uiState.update { it.copy(showTranslation = !it.showTranslation) }
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