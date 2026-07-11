package com.pinza.hush.data.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.utils.toSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    private val exoPlayer: ExoPlayer
) : IPlayerManager {

    private val _currentSongState = MutableStateFlow<Song?>(null)
    override val currentSongState = _currentSongState.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)
    override val isPlayingState = _isPlayingState.asStateFlow()

    private val _isBufferingState = MutableStateFlow(false)
    override val isBufferingState = _isBufferingState.asStateFlow()

    private val _repeatModeState = MutableStateFlow(Player.REPEAT_MODE_OFF)
    override val repeatModeState = _repeatModeState.asStateFlow()

    private val _currentQueueState = MutableStateFlow<List<Song>>(emptyList())
    override val currentQueueState = _currentQueueState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentSongState.value = mediaItem?.toSong()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingState.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBufferingState.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatModeState.value = repeatMode
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                updateQueueState()
            }
        })
    }

    private fun updateQueueState() {
        val songs = mutableListOf<Song>()
        for (i in 0 until exoPlayer.mediaItemCount) {
            songs.add(exoPlayer.getMediaItemAt(i).toSong())
        }
        _currentQueueState.value = songs
    }

    override fun play(song: Song) {
        val mediaItem = createMediaItem(song)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _currentSongState.value = song
    }

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        val mediaItems = songs.map { createMediaItem(it) }
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        if (songs.isNotEmpty()) {
            _currentSongState.value = songs[startIndex]
        }
        updateQueueState()
    }

    private fun createMediaItem(song: Song): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArt?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(Uri.fromFile(File(song.filePath)))
            .setMediaMetadata(mediaMetadata)
            .build()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun resume() {
        exoPlayer.play()
    }

    override fun stop() {
        exoPlayer.stop()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun skipToNext() {
        when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                exoPlayer.seekTo(0L)
            }
            Player.REPEAT_MODE_OFF -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNext()
                } else {
                    exoPlayer.seekTo(0, 0L)
                }
            }
            else -> {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNext()
                } else {
                    exoPlayer.seekTo(0, 0L)
                }
            }
        }
    }

    override fun skipToPrevious() {
        when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                exoPlayer.seekTo(0L)
            }
            else -> {
                if (exoPlayer.hasPreviousMediaItem()) {
                    exoPlayer.seekToPrevious()
                } else {
                    exoPlayer.seekTo(exoPlayer.mediaItemCount - 1, 0L)
                }
            }
        }
    }

    override fun skipToQueueItem(index: Int) {
        if (index >= 0 && index < exoPlayer.mediaItemCount) {
            exoPlayer.seekTo(index, 0L)
        }
    }

    override fun toggleRepeatMode() {
        val nextMode = when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = nextMode
    }

    override fun addNext(song: Song) {
        val nextIndex = if (exoPlayer.mediaItemCount == 0) 0 else exoPlayer.currentMediaItemIndex + 1
        exoPlayer.addMediaItem(nextIndex, createMediaItem(song))
        updateQueueState()
    }

    override fun removeFromQueue(index: Int) {
        if (index >= 0 && index < exoPlayer.mediaItemCount) {
            exoPlayer.removeMediaItem(index)
            updateQueueState()
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in 0 until exoPlayer.mediaItemCount && toIndex in 0 until exoPlayer.mediaItemCount) {
            exoPlayer.moveMediaItem(fromIndex, toIndex)
            updateQueueState()
        }
    }

    override fun isPlaying(): Boolean = exoPlayer.isPlaying
    override fun currentPosition(): Long = exoPlayer.currentPosition
    override fun duration(): Long = exoPlayer.duration
}
