package com.pinza.hush.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QueueUiState(
    val songs: List<Song> = emptyList(),
    val activeSongId: Long? = null
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerManager: IPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeQueue()
        observeActiveSong()
    }

    private fun observeQueue() {
        viewModelScope.launch {
            playerManager.currentQueueState.collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    private fun observeActiveSong() {
        viewModelScope.launch {
            playerManager.currentSongState.collect { song ->
                _uiState.update { it.copy(activeSongId = song?.id) }
            }
        }
    }

    fun removeFromQueue(song: Song) {
        val index = uiState.value.songs.indexOf(song)
        if (index != -1) {
            playerManager.removeFromQueue(index)
        }
    }

    fun playSpecificSong(song: Song) {
        val index = uiState.value.songs.indexOf(song)
        if (index != -1) {
            playerManager.skipToQueueItem(index)
        }
    }

    fun moveItem(from: Int, to: Int) {
        playerManager.moveQueueItem(from, to)
    }
}
