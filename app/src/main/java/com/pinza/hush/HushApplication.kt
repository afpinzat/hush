package com.pinza.hush

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class HushApplication : Application() {

    @Inject
    lateinit var playerStateRepository: com.pinza.hush.domain.repository.IPlayerStateRepository

    @Inject
    lateinit var playerManager: com.pinza.hush.domain.player.IPlayerManager

    override fun onCreate() {
        super.onCreate()
        Log.d("HushApplication", "🚀 App iniciada")
    }

    // ✅ Método público para guardar estado en Room
    fun savePlayerState() {
        try {
            val currentSong = playerManager.currentSongState.value
            val position = playerManager.currentPosition()

            currentSong?.let { song ->
                runBlocking {
                    playerStateRepository.save(
                        com.pinza.hush.data.local.model.PlayerState(
                            currentSongId = song.id,
                            isPlaying = playerManager.isPlaying(),
                            currentPosition = position.toInt(),
                            playbackSpeed = 1f,
                            queueIds = "",
                            queueIndex = -1
                        )
                    )
                }
                Log.d("HushApplication", "💾 Estado guardado en Room: ${song.title}")
            }
        } catch (e: Exception) {
            Log.e("HushApplication", "❌ Error guardando: ${e.message}")
        }
    }
}