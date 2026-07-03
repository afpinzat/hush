package com.pinza.hush.ui.state

import com.pinza.hush.data.local.model.PlayerState

data class PlayerUiState(

    val state: PlayerState? = null,

    val isPlaying: Boolean = false,

    val currentPosition: Long = 0L,

    val duration: Long = 0L

)