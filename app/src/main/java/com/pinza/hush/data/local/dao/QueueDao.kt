package com.pinza.hush.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.pinza.hush.data.local.model.QueueItem
import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
    interface QueueDao {
        @Query("SELECT * FROM queue ORDER BY position ASC")
        fun getQueue(): Flow<List<QueueItemWithSong>> // Usaremos una clase relacional

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(items: List<QueueItem>)

        @Query("DELETE FROM queue")
        suspend fun clearQueue()

    @Query("SELECT IFNULL(MAX(position), -1) FROM queue")
    suspend fun getLastPosition(): Int

    @Query("DELETE FROM playlist_song_join WHERE songId = :songId")
    suspend fun removeFromQueue(songId: Long) // No necesita cuerpo, Room lo genera por ti

}


    // Clase para traer la canción automáticamente al consultar la cola
    data class QueueItemWithSong(
        @Embedded val queueItem: QueueItem,
        @Relation(parentColumn = "songId", entityColumn = "id")
        val song: Song
    )