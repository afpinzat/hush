package com.pinza.hush.ui.player
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import com.pinza.hush.di.MediaManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSongTitle: String = "",
    val progress: Float = 0f

)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaManager: MediaManager // Inyectamos el gestor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    fun togglePlayPause() {
        val controller = mediaManager.controller ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(position: Float) {
        val controller = mediaManager.controller ?: return
        controller.seekTo((position * controller.duration).toLong())
    }
}