package com.pinza.hush.domain.player

import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.StateFlow

interface IPlayerManager {
    // ─── REPRODUCCIÓN ───────────────────────────────────────────────
    fun play(song: Song)
    fun playSongAt(index: Int)
    fun pause()
    fun resume()
    fun stop()
    fun next()
    fun previous()
    fun seekTo(position: Long)

    // ─── INFORMACIÓN ────────────────────────────────────────────────
    fun currentPosition(): Long
    fun duration(): Long
    fun isPlaying(): Boolean
    fun hasNext(): Boolean
    fun hasPrevious(): Boolean

    // ─── COLA ────────────────────────────────────────────────────────
    fun setQueue(songs: List<Song>, startIndex: Int = 0)
    fun getQueue(): List<Song>
    fun getCurrentIndex(): Int
    fun getQueueSize(): Int
    fun addToQueue(song: Song)
    fun clearQueue()

    // ─── PERSISTENCIA ────────────────────────────────────────────────
    fun getStateForPersistence(): Triple<Long, List<Song>, Int>
    fun restoreState(song: Song, position: Long, queue: List<Song>, queueIndex: Int)

    // ─── ESTADOS OBSERVABLES ────────────────────────────────────────
    val isPlayingState: StateFlow<Boolean>
    val currentPositionState: StateFlow<Long>
    val durationState: StateFlow<Long>
    val currentSongState: StateFlow<Song?>
    val isPreparedState: StateFlow<Boolean>
}