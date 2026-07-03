package com.pinza.hush.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song")
data class Song(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val artist: String,

    val duration: Int,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "album_art")
    val albumArt: String? = null
)