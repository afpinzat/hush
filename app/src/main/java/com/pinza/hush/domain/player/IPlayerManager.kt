package com.pinza.hush.domain.player

import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.StateFlow

interface IPlayerManager {

    // Estados observables
    val currentSongState: StateFlow<Song?>
    val isPlayingState: StateFlow<Boolean>
    val isBufferingState: StateFlow<Boolean>
    val repeatModeState: StateFlow<Int>
    val currentQueueState: StateFlow<List<Song>>

    // Control de reproducción
    fun play(song: Song)
    fun playQueue(songs: List<Song>, startIndex: Int = 0)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(position: Long)
    fun skipToNext()
    fun skipToPrevious()
    fun skipToQueueItem(index: Int)

    // Gestión de cola
    fun addNext(song: Song)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    // Configuración
    fun toggleRepeatMode()

    // Información del reproductor
    fun isPlaying(): Boolean
    fun currentPosition(): Long
    fun duration(): Long
}
