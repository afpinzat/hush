package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.pinza.hush.data.local.model.Song

@Entity(
    tableName = "queue",
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val queueId: Int = 0,
    val songId: Long,
    val position: Int
)