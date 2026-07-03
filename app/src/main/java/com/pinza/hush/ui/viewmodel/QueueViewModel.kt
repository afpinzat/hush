package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.QueueItem
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.player.IPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerManager: IPlayerManager
) : ViewModel() {

    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems: StateFlow<List<QueueItem>> = _queueItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadQueue()
    }

    fun loadQueue() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val queue = playerManager.getQueue()
                val currentIndex = playerManager.getCurrentIndex()
                val currentSong = playerManager.currentSongState.value

                val items = queue.mapIndexed { index, song ->
                    QueueItem(
                        queueId = index,
                        position = index,
                        addedAt = System.currentTimeMillis(),
                        song = song,
                        isPlaying = index == currentIndex && currentSong?.id == song.id
                    )
                }
                _queueItems.value = items
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun playQueueItem(item: QueueItem) {
        val song = item.song
        val index = _queueItems.value.indexOf(item)
        if (index != -1) {
            playerManager.playSongAt(index)
            // Actualizar estados
            val updatedItems = _queueItems.value.mapIndexed { i, queueItem ->
                queueItem.copy(isPlaying = i == index)
            }
            _queueItems.value = updatedItems
        } else {
            playerManager.play(song)
        }
    }

    fun removeFromQueue(item: QueueItem) {
        val currentItems = _queueItems.value.toMutableList()
        val index = currentItems.indexOf(item)
        if (index != -1) {
            currentItems.removeAt(index)
            _queueItems.value = currentItems

            // Actualizar posiciones
            val updatedItems = currentItems.mapIndexed { i, queueItem ->
                queueItem.copy(position = i)
            }
            _queueItems.value = updatedItems

            // Si eliminamos la canción actual, actualizar PlayerManager
            if (item.isPlaying) {
                if (currentItems.isNotEmpty()) {
                    playerManager.playSongAt(0)
                } else {
                    playerManager.stop()
                }
            }
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return

        val currentItems = _queueItems.value.toMutableList()
        val item = currentItems.removeAt(fromPosition)
        currentItems.add(toPosition, item)

        // Actualizar posiciones
        val updatedItems = currentItems.mapIndexed { i, queueItem ->
            queueItem.copy(position = i)
        }
        _queueItems.value = updatedItems
    }

    fun saveQueueOrder() {
        // Guardar el orden en PlayerManager
        val songs = _queueItems.value.map { it.song }
        val currentIndex = _queueItems.value.indexOfFirst { it.isPlaying }
        if (currentIndex != -1) {
            playerManager.setQueue(songs, currentIndex)
        } else if (songs.isNotEmpty()) {
            playerManager.setQueue(songs, 0)
        }
    }

    fun clearQueue() {
        _queueItems.value = emptyList()
        playerManager.clearQueue()
        playerManager.stop()
    }

    fun clearError() {
        _error.value = null
    }
}