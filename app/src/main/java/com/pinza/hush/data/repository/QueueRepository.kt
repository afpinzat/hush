package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.QueueDao
import com.pinza.hush.data.local.dao.QueueItemWithSong
import com.pinza.hush.data.local.model.QueueItem
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.domain.repository.IQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepository @Inject constructor(
    private val queueDao: QueueDao
) : IQueueRepository {

    override fun getQueue(): Flow<List<Song>> {
        return queueDao.getQueue().map { list ->
            // Convertimos la lista de "objetos con canción" a solo una lista de "canciones"
            list.map { it.song }
        }
    }

    override suspend fun deleteFromQueue(song: Song) {
        // Si tu cola está en la tabla 'playlist_song_join' o una tabla 'queue',
        // necesitas un método en tu DAO que elimine el registro
        queueDao.removeFromQueue(song.id)
    }

    override suspend fun insertAll(songs: List<Song>) {
        // Obtenemos el tamaño actual para añadir al final (o podrías decidir insertarlo al principio)
        // Esto es un ejemplo simple:
        val currentSize = 0 // En una versión avanzada, consultarías el conteo actual en DB

        val queueItems = songs.mapIndexed { index, song ->
            QueueItem(songId = song.id, position = currentSize + index)
        }

        queueDao.insertAll(queueItems)
    }

    override suspend fun setQueue(songs: List<Song>) {
        // 1. Limpiamos la cola actual
        queueDao.clearQueue()

        // 2. Mapeamos la lista de canciones a QueueItems con posición
        val queueItems = songs.mapIndexed { index, song ->
            QueueItem(songId = song.id, position = index)
        }

        // 3. Insertamos todo en una sola transacción
        queueDao.insertAll(queueItems)
    }

    override suspend fun clearQueue() = queueDao.clearQueue()


    override suspend fun addToQueue(song: Song) {
        // 1. Obtenemos la última posición actual
        val lastPos = queueDao.getLastPosition()

        // 2. Creamos el nuevo ítem en la posición siguiente
        val newItem = QueueItem(
            songId = song.id,
            position = lastPos + 1
        )

        // 3. Insertamos el ítem individual
        queueDao.insertAll(listOf(newItem))
    }
}