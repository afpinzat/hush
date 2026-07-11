package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_lyrics",
    foreignKeys = [/* ... tu ForeignKey ... */]
)
data class SongLyrics(
    @PrimaryKey val songId: Long, // Usamos songId como PrimaryKey
    val lyrics: String,
    val source: String? = null,
    val language: String? = null
)