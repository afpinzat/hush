package com.pinza.hush.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pinza.hush.R
import com.pinza.hush.data.model.Song
import com.pinza.hush.ui.nowplaying.NowPlayingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MusicPlayerService — Foreground Service con ExoPlayer
 *
 * Por qué un Service y no solo el ViewModel:
 * - La música debe seguir sonando cuando el usuario sale de la app.
 * - Un ViewModel se destruye con la Activity; un Service persiste.
 * - Android requiere un Foreground Service para reproducción en background
 *   (con notificación visible obligatoria desde Android 8+).
 *
 * LifecycleService: versión de Service compatible con corrutinas y lifecycleScope.
 */
@AndroidEntryPoint
class MusicPlayerService : LifecycleService() {

    // ── BINDER (comunicación Activity ↔ Service) ──────────────────────────
    /**
     * LocalBinder permite a la Activity obtener la instancia del Service
     * y llamar sus funciones directamente (sin IPC, mismo proceso).
     */
    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    private val binder = LocalBinder()

    // ── EXOPLAYER ─────────────────────────────────────────────────────────
    private lateinit var exoPlayer: ExoPlayer

    // ── ESTADO EXPUESTO A LA UI ───────────────────────────────────────────
    private val _isPlaying    = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong  = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _progressMs   = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs   = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Cola de reproducción y posición actual
    private var queue: List<Song> = emptyList()
    private var currentIndex: Int = 0

    // Job que actualiza el progreso cada 500ms
    private var progressJob: Job? = null

    companion object {
        const val CHANNEL_ID      = "hush_player_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY     = "com.pinza.hush.PLAY"
        const val ACTION_PAUSE    = "com.pinza.hush.PAUSE"
        const val ACTION_NEXT     = "com.pinza.hush.NEXT"
        const val ACTION_PREV     = "com.pinza.hush.PREV"
        const val ACTION_STOP     = "com.pinza.hush.STOP"
    }

    // ── LIFECYCLE ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initExoPlayer()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    /**
     * onStartCommand: recibe acciones desde los botones de la notificación.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY  -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT  -> skipNext()
            ACTION_PREV  -> skipPrevious()
            ACTION_STOP  -> stopSelf()
        }
        return START_STICKY
        // START_STICKY: si el sistema mata el servicio, lo reinicia
    }

    override fun onDestroy() {
        progressJob?.cancel()
        exoPlayer.release()
        super.onDestroy()
    }

    // ── INICIALIZACIÓN ────────────────────────────────────────────────────

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {

            /**
             * Se llama cuando ExoPlayer cambia entre play/pause/buffering/ended.
             */
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressUpdater() else progressJob?.cancel()
                updateNotification()
            }

            /**
             * Se llama cuando la canción termina.
             * Player.STATE_ENDED = llegó al final del archivo.
             */
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    skipNext()
                }
            }
        })
    }

    // ── CONTROL DE REPRODUCCIÓN ───────────────────────────────────────────

    /**
     * Carga y reproduce una canción.
     * @param song    canción a reproducir
     * @param queue   lista completa (para siguiente/anterior)
     * @param index   posición en la cola
     */
    fun playSong(song: Song, queue: List<Song>, index: Int) {
        this.queue        = queue
        this.currentIndex = index
        _currentSong.value = song

        // Convierte la ruta del archivo en un MediaItem de ExoPlayer
        val mediaItem = MediaItem.fromUri(song.filePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()   // carga el archivo
        exoPlayer.play()      // inicia reproducción

        startForeground(NOTIFICATION_ID, buildNotification(song))
    }

    fun pausePlayback() {
        exoPlayer.pause()
    }

    fun resumePlayback() {
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) pausePlayback() else resumePlayback()
    }

    fun skipNext() {
        if (queue.isEmpty()) return
        currentIndex = if (currentIndex < queue.lastIndex) currentIndex + 1 else 0
        playSong(queue[currentIndex], queue, currentIndex)
    }

    fun skipPrevious() {
        // Si pasaron más de 3s, reinicia la canción actual
        if (exoPlayer.currentPosition > 3_000L) {
            exoPlayer.seekTo(0)
            return
        }
        currentIndex = if (currentIndex > 0) currentIndex - 1 else queue.lastIndex
        if (queue.isNotEmpty()) playSong(queue[currentIndex], queue, currentIndex)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    fun getCurrentPositionMs(): Long = exoPlayer.currentPosition

    fun getDurationMs(): Long = exoPlayer.duration.coerceAtLeast(0L)

    // ── PROGRESO ──────────────────────────────────────────────────────────

    /**
     * Actualiza _progressMs y _durationMs cada 500ms mientras reproduce.
     * La UI observa estos valores para mover el SeekBar en tiempo real.
     */
    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            while (exoPlayer.isPlaying) {
                _progressMs.value  = exoPlayer.currentPosition
                _durationMs.value  = exoPlayer.duration.coerceAtLeast(0L)
                delay(500L)
            }
        }
    }

    // ── NOTIFICACIÓN ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproductor Hush",
            NotificationManager.IMPORTANCE_LOW  // LOW = sin sonido, sin vibración
        ).apply {
            description = "Controles de reproducción de música"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(song: Song): Notification {
        // Intent para abrir NowPlayingActivity al tocar la notificación
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, NowPlayingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intents para los botones de la notificación
        fun actionIntent(action: String) = PendingIntent.getService(
            this,
            action.hashCode(),
            Intent(this, MusicPlayerService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note_24)
            .setContentIntent(contentIntent)
            .setOngoing(true)           // el usuario no puede deslizar para cerrar
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Botones de control
            .addAction(R.drawable.ic_skip_previous_24, "Anterior", actionIntent(ACTION_PREV))
            .addAction(
                if (exoPlayer.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24,
                if (exoPlayer.isPlaying) "Pausar" else "Reproducir",
                actionIntent(if (exoPlayer.isPlaying) ACTION_PAUSE else ACTION_PLAY)
            )
            .addAction(R.drawable.ic_skip_next_24, "Siguiente", actionIntent(ACTION_NEXT))
            // Estilo de media (muestra controles en la pantalla de bloqueo)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2) // anterior, play/pause, siguiente
            )
            .build()
    }

    private fun updateNotification() {
        _currentSong.value?.let { song ->
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(song))
        }
    }
}
