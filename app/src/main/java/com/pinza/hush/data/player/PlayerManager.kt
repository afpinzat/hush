package com.pinza.hush.data.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : IPlayerManager {

    private val TAG = "PlayerManager"

    // ─── ESTADOS OBSERVABLES ────────────────────────────────────────

    private val _isPlaying = MutableStateFlow(false)
    override val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPositionState: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val durationState: StateFlow<Long> = _duration.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSongState: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPrepared = MutableStateFlow(false)
    override val isPreparedState: StateFlow<Boolean> = _isPrepared.asStateFlow()

    // ─── COLA DE REPRODUCCIÓN ───────────────────────────────────────

    private var queue: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var serviceStarted = false

    // ─── REPRODUCTOR ─────────────────────────────────────────────────

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _isPrepared.value = true
                        _duration.value = duration
                        android.util.Log.d(TAG, "📊 Duración: ${formatDuration(duration)}")
                    }
                    Player.STATE_ENDED -> {
                        android.util.Log.d(TAG, "⏭️ Canción terminada")
                        _isPlaying.value = false
                        playNext()
                    }
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                android.util.Log.d(TAG, "▶️ isPlaying: $isPlaying")
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e(TAG, "❌ Error: ${error.message}")
            }
        })
    }

    // ─── ACTUALIZACIÓN DE PROGRESO ──────────────────────────────────

    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(200)
                _currentPosition.value = player.currentPosition
                _duration.value = player.duration
            }
        }
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    // ─── INICIAR SERVICIO UNA SOLA VEZ ─────────────────────────────

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun startServiceIfNeeded(song: Song) {
        if (serviceStarted) return
        try {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                putExtra("song_id", song.id)
                putExtra("song_title", song.title)
                putExtra("song_artist", song.artist)
                putExtra("song_path", song.filePath)
                putExtra("song_duration", song.duration)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            serviceStarted = true
            android.util.Log.d(TAG, "✅ Servicio iniciado")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error iniciando servicio: ${e.message}")
        }
    }

    // ─── MÉTODOS DE REPRODUCCIÓN ────────────────────────────────────

    override fun play(song: Song) {
        android.util.Log.d(TAG, "🎵 play: ${song.title}")
        queue = listOf(song)
        currentIndex = 0
        playSongAtIndex(currentIndex)
        startServiceIfNeeded(song)
        startForegroundService(song)
    }

    override fun playSongAt(index: Int) {
        android.util.Log.d(TAG, "📌 playSongAt: $index")
        if (index >= 0 && index < queue.size) {
            currentIndex = index
            playSongAtIndex(currentIndex)
        }
    }

    private fun playSongAtIndex(index: Int) {
        if (index < 0 || index >= queue.size) {
            android.util.Log.e(TAG, "❌ Índice inválido: $index")
            return

        }

        currentIndex = index
        val song = queue[index]
        android.util.Log.d(TAG, "▶️ Reproduciendo: ${song.title} (${index + 1}/${queue.size})")

        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.filePath)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        _isPlaying.value = true
        _isPrepared.value = false
        startProgressUpdater()
        startForegroundService(song)
    }

    override fun pause() {
        android.util.Log.d(TAG, "⏸️ pause")
        player.pause()
        _isPlaying.value = false
        stopProgressUpdater()
    }

    override fun resume() {
        android.util.Log.d(TAG, "▶️ resume")
        if (_currentSong.value != null) {
            player.play()
            _isPlaying.value = true
            startProgressUpdater()
        }
    }

    override fun stop() {
        android.util.Log.d(TAG, "⏹️ stop")
        player.stop()
        _isPlaying.value = false
        _currentPosition.value = 0
        _currentSong.value = null
        stopProgressUpdater()
        serviceStarted = false
        try {
            context.stopService(Intent(context, MusicPlayerService::class.java))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error deteniendo servicio: ${e.message}")
        }
    }

    // ─── NAVEGACIÓN ──────────────────────────────────────────────────

    override fun next() {
        android.util.Log.d(TAG, "⏭️ next() llamado")
        playNext()
    }

    override fun previous() {
        android.util.Log.d(TAG, "⏮️ previous() llamado")
        if (player.currentPosition > 3000) {
            android.util.Log.d(TAG, "🔄 Reiniciando canción")
            player.seekTo(0)
        } else {
            playPrevious()
        }
    }

    private fun playNext() {
        if (queue.isEmpty()) {
            android.util.Log.d(TAG, "❌ Cola vacía")
            return
        }

        if (queue.size <= 1) {
            android.util.Log.d(TAG, "⚠️ Solo una canción, reiniciando")
            player.seekTo(0)
            player.play()
            _isPlaying.value = true
            startProgressUpdater()
            return
        }

        val nextIndex = if (currentIndex + 1 < queue.size) currentIndex + 1 else 0
        android.util.Log.d(TAG, "📌 Siguiente índice: $nextIndex")
        currentIndex = nextIndex
        playSongAtIndex(currentIndex)
    }

    private fun playPrevious() {
        if (queue.isEmpty()) {
            android.util.Log.d(TAG, "❌ Cola vacía")
            return
        }

        if (queue.size <= 1) {
            android.util.Log.d(TAG, "⚠️ Solo una canción, reiniciando")
            player.seekTo(0)
            player.play()
            _isPlaying.value = true
            startProgressUpdater()
            return
        }

        val prevIndex = if (currentIndex - 1 >= 0) currentIndex - 1 else queue.size - 1
        android.util.Log.d(TAG, "📌 Índice anterior: $prevIndex")
        currentIndex = prevIndex
        playSongAtIndex(currentIndex)
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = position
    }

    override fun currentPosition(): Long = player.currentPosition
    override fun duration(): Long = player.duration
    override fun isPlaying(): Boolean = player.isPlaying
    override fun hasNext(): Boolean = queue.isNotEmpty() && currentIndex + 1 < queue.size
    override fun hasPrevious(): Boolean = queue.isNotEmpty() && currentIndex - 1 >= 0

    // ─── GESTIÓN DE COLA ─────────────────────────────────────────────

    override fun setQueue(songs: List<Song>, startIndex: Int) {
        android.util.Log.d(TAG, "📋 setQueue - songs: ${songs.size}, startIndex: $startIndex")
        if (songs.isEmpty()) {
            android.util.Log.d(TAG, "❌ Lista vacía")
            return
        }
        queue = songs
        currentIndex = startIndex.coerceIn(0, queue.size - 1)
        android.util.Log.d(TAG, "✅ Cola: ${queue.size} canciones, índice: $currentIndex")
        playSongAtIndex(currentIndex)
    }

    override fun getQueue(): List<Song> = queue
    override fun getCurrentIndex(): Int = currentIndex
    override fun getQueueSize(): Int = queue.size
    override fun addToQueue(song: Song) {
        queue = queue + song
    }
    override fun clearQueue() {
        queue = emptyList()
        currentIndex = -1
    }

    override fun getStateForPersistence(): Triple<Long, List<Song>, Int> {
        return Triple(currentPosition(), queue, currentIndex)
    }

    override fun restoreState(song: Song, position: Long, queueSongs: List<Song>, queueIndex: Int) {
        android.util.Log.d(TAG, "♻️ Restaurando: ${song.title}")
        this.queue = queueSongs
        this.currentIndex = queueIndex
        _currentSong.value = song
        val mediaItem = MediaItem.fromUri(song.filePath)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(position)
        player.play()
        _isPlaying.value = true
        startProgressUpdater()
        startServiceIfNeeded(song)
    }

    private fun startForegroundService(song: Song) {
        try {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                putExtra("song_id", song.id)
                putExtra("song_title", song.title)
                putExtra("song_artist", song.artist)
                putExtra("song_path", song.filePath)
                putExtra("song_duration", song.duration)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.util.Log.d("PlayerManager", "✅ Servicio iniciado")
        } catch (e: Exception) {
            android.util.Log.e("PlayerManager", "❌ Error iniciando servicio: ${e.message}")
        }
    }
}