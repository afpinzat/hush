package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_state")
data class PlayerState(
    @PrimaryKey
    val id: Int = 1, // Siempre será 1 para que solo exista una fila de estado
    val currentSongId: Long? = null,
    val currentPosition: Long = 0L, // Usamos Long para precisión en milisegundos
    val isPlaying: Boolean = false
)