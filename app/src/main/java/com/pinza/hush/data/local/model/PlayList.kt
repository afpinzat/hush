package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val createdAt: Long,

    val songCount: Int = 0
)