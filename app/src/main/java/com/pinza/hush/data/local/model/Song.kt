package com.pinza.hush.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
// Song.kt
@Entity(tableName = "song")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val artist: String,
    val duration: Int,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "album_art")  // ✅ album_art
    val albumArt: String? = null,

    @ColumnInfo(name = "album")
    val album: String? = null,

    @ColumnInfo(name = "year")
    val year: Int? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false
)

