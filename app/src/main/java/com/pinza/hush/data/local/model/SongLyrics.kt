package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_lyrics")
data class SongLyrics(
    @PrimaryKey val songId: Long,
    val lyrics: String,
    val lyrics2: String? = null,
    val isPrimarySecond: Boolean = false,
    val source: String? = null,
    val language: String? = null
)
