package com.pinza.hush.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.ui.activity.NowPlayingActivity
import com.pinza.hush.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MusicPlayerService : Service() {

    private val binder = LocalBinder()

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var playerManager: IPlayerManager  // ✅ Inyectado

    private var currentSong: Song? = null
    private var isForeground = false

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("MusicPlayerService", "✅ onCreate()")
        notificationHelper.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("MusicPlayerService", "✅ onStartCommand() - Action: ${intent?.action}")

        // ✅ MANEJAR ACCIONES DE LA NOTIFICACIÓN
        when (intent?.action) {
            NotificationHelper.ACTION_PLAY_PAUSE -> {
                android.util.Log.d("MusicPlayerService", "⏯️ ACTION_PLAY_PAUSE")
                if (playerManager.isPlaying()) {
                    playerManager.pause()
                } else {
                    playerManager.resume()
                }
                updateNotification()
                return START_STICKY
            }
            NotificationHelper.ACTION_NEXT -> {
                android.util.Log.d("MusicPlayerService", "⏭️ ACTION_NEXT")
                playerManager.next()
                updateNotification()
                return START_STICKY
            }
            NotificationHelper.ACTION_PREVIOUS -> {
                android.util.Log.d("MusicPlayerService", "⏮️ ACTION_PREVIOUS")
                playerManager.previous()
                updateNotification()
                return START_STICKY
            }
            NotificationHelper.ACTION_CLOSE -> {
                android.util.Log.d("MusicPlayerService", "⏹️ ACTION_CLOSE")
                playerManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                // ✅ Si no hay acción, mostrar la notificación con la canción actual
                val title = intent?.getStringExtra("song_title") ?: ""
                val artist = intent?.getStringExtra("song_artist") ?: ""
                val path = intent?.getStringExtra("song_path") ?: ""
                val duration = intent?.getIntExtra("song_duration", 0) ?: 0
                val songId = intent?.getIntExtra("song_id", -1) ?: -1

                if (title.isNotEmpty() && path.isNotEmpty()) {
                    val song = Song(
                        id = songId,
                        title = title,
                        artist = artist,
                        duration = duration,
                        filePath = path
                    )
                    currentSong = song
                    showNotification(song)
                } else {
                    // ✅ Si no hay canción, intentar obtener la actual del PlayerManager
                    val currentSong = playerManager.currentSongState.value
                    if (currentSong != null) {
                        showNotification(currentSong)
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun showNotification(song: Song) {
        android.util.Log.d("MusicPlayerService", "📢 showNotification: ${song.title}")
        currentSong = song

        val notification = buildNotification(song)

        if (!isForeground) {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            isForeground = true
            android.util.Log.d("MusicPlayerService", "✅ startForeground() ejecutado")
        } else {
            NotificationManagerCompat.from(this).notify(NotificationHelper.NOTIFICATION_ID, notification)
            android.util.Log.d("MusicPlayerService", "✅ Notificación actualizada")
        }
    }

    private fun updateNotification() {
        val song = currentSong ?: playerManager.currentSongState.value
        if (song != null && isForeground) {
            val notification = buildNotification(song)
            NotificationManagerCompat.from(this).notify(NotificationHelper.NOTIFICATION_ID, notification)
            android.util.Log.d("MusicPlayerService", "✅ Notificación actualizada")
        }
    }

    private fun buildNotification(song: Song): Notification {
        val isPlaying = playerManager.isPlaying()
        val position = playerManager.currentPosition()
        val duration = playerManager.duration()

        // Intent para abrir la app
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, NowPlayingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Intents para las acciones
        val playPauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlayerService::class.java).apply {
                action = NotificationHelper.ACTION_PLAY_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlayerService::class.java).apply {
                action = NotificationHelper.ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previousIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlayerService::class.java).apply {
                action = NotificationHelper.ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlayerService::class.java).apply {
                action = NotificationHelper.ACTION_CLOSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pause_24
        } else {
            R.drawable.ic_play_arrow_24
        }

        val playPauseText = if (isPlaying) "Pausar" else "Reproducir"

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note_24)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_skip_previous_24, "Anterior", previousIntent)
            .addAction(playPauseIcon, playPauseText, playPauseIntent)
            .addAction(R.drawable.ic_skip_next_24, "Siguiente", nextIntent)
            .addAction(R.drawable.ic_close_24, "Cerrar", closeIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setProgress(duration.toInt(), position.toInt(), false)
            .setShowWhen(false)
            .build()
    }

    override fun onDestroy() {
        android.util.Log.d("MusicPlayerService", "⏹️ onDestroy()")
        isForeground = false
        super.onDestroy()
    }
}