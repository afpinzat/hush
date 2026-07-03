package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_lyrics",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["songId"], unique = true)
    ]
)
data class SongLyrics(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val songId: Int,

    val lyrics: String,

    val source: String?,

    val language: String?
)