package com.pinza.hush.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.service.MusicPlayerService
import com.pinza.hush.ui.activity.NowPlayingActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "hush_music_channel"
        const val CHANNEL_NAME = "Reproductor Hush"
        const val NOTIFICATION_ID = 1001

        // Acciones
        const val ACTION_PLAY_PAUSE = "com.pinza.hush.PLAY_PAUSE"
        const val ACTION_NEXT = "com.pinza.hush.NEXT"
        const val ACTION_PREVIOUS = "com.pinza.hush.PREVIOUS"
        const val ACTION_CLOSE = "com.pinza.hush.CLOSE"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Control de reproducción de música"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildMediaNotification(
        song: Song,
        isPlaying: Boolean,
        currentPosition: Long,
        duration: Long
    ): NotificationCompat.Builder {

        // Intent para abrir la app al hacer click
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, NowPlayingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para Play/Pause
        val playPauseIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_PLAY_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para Next
        val nextIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_NEXT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para Previous
        val previousIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_PREVIOUS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para Cerrar
        val closeIntent = PendingIntent.getService(
            context,
            4,
            Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_CLOSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pause_24
        } else {
            R.drawable.ic_play_arrow_24
        }

        val playPauseText = if (isPlaying) "Pausar" else "Reproducir"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note_24)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_skip_previous_24, "Anterior", previousIntent)
            .addAction(playPauseIcon, playPauseText, playPauseIntent)
            .addAction(R.drawable.ic_skip_next_24, "Siguiente", nextIntent)
            .addAction(R.drawable.ic_close_24, "Cerrar", closeIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setProgress(duration.toInt(), currentPosition.toInt(), false)
            .setShowWhen(false)
    }
}