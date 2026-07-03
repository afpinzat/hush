package com.pinza.hush.data.scanner

import android.content.Context
import android.provider.MediaStore
import com.pinza.hush.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * MusicScanner
 * Usa MediaStore (la base de datos del sistema Android) para
 * encontrar todos los archivos de audio en el almacenamiento del dispositivo.
 * No necesita leer el sistema de archivos directamente — Android indexa
 * automáticamente los archivos de música y los expone a través de MediaStore.
 */
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Escanea el almacenamiento y retorna una lista de canciones.
     * Es una suspend fun porque hace I/O — se llama desde una corrutina.
     *
     * MediaStore.Audio.Media.EXTERNAL_CONTENT_URI apunta al almacenamiento
     * externo (tarjeta SD y almacenamiento interno del usuario).
     */
    fun scanMusic(): List<Song> {
        val songs = mutableListOf<Song>()

        // Columnas que queremos leer de MediaStore
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,          // ruta del archivo
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Filtro: solo archivos de música (no notificaciones, ringtones, etc.)
        // IS_MUSIC = 1 excluye efectos de sonido del sistema
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 " +
                        "AND ${MediaStore.Audio.Media.DURATION} > 30000"
        // AND DURATION > 30000 ms = descarta archivos de menos de 30 segundos

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            // Índices de columnas — más rápido que llamar a getColumnIndex() por fila
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id       = cursor.getLong(idCol)
                val title    = cursor.getString(titleCol) ?: "Sin título"
                val artist   = cursor.getString(artistCol)
                    .let { if (it == "<unknown>") "Artista desconocido" else it }
                val duration = (cursor.getLong(durationCol) / 1000).toInt() // ms → segundos
                val filePath = cursor.getString(dataCol) ?: continue
                val albumId  = cursor.getLong(albumIdCol)

                // URI de la carátula del álbum (puede ser null si no tiene)
                val albumArtUri = getAlbumArtUri(albumId)

                songs.add(
                    Song(
                        title    = title,
                        artist   = artist,
                        duration = duration,
                        filePath = filePath,
                        albumArt = albumArtUri
                    )
                )
            }
        }

        return songs
    }

    /**
     * Construye la URI de la carátula del álbum a partir del albumId.
     * Formato estándar de Android para acceder a artwork sin permisos extra.
     */
    private fun getAlbumArtUri(albumId: Long): String? {
        return if (albumId > 0) {
            "content://media/external/audio/albumart/$albumId"
        } else null
    }
}
