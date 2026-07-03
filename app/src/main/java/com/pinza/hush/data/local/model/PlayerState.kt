package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerState(
    @PrimaryKey
    val id: Int = 1,
    val currentSongId: Int? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val playbackSpeed: Float = 1f,
    val queueIds: String = "",
    val queueIndex: Int = -1
)