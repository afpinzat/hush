package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId")]
)
data class Queue(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val songId: Int,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

// QueueItem: JOIN de Queue + Song para mostrar en la UI sin dos queries
data class QueueItem(
    val queueId: Int,
    val position: Int,
    val addedAt: Long,
    val song: Song,
    val isPlaying: Boolean = false
)
