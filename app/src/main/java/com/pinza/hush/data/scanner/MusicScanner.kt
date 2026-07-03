package com.pinza.hush.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.pinza.hush.data.local.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MusicScanner @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    fun scan(): List<Song> {
        android.util.Log.d("MusicScanner", "🔍 Iniciando escaneo...")
        val list = mutableListOf<Song>()

        // ✅ Verificar que el contentResolver no sea null
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR
        )

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        android.util.Log.d("MusicScanner", "📁 URI: $uri")

        // ✅ Mejor filtro para encontrar música
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} IS NOT NULL"
        val selectionArgs: Array<String>? = null
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                android.util.Log.d("MusicScanner", "📊 Cursor count: ${cursor.count}")

                // Verificar columnas
                val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val pathIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                android.util.Log.d("MusicScanner", "📊 Columnas: ID=$idIndex, TITLE=$titleIndex, ARTIST=$artistIndex, DURATION=$durationIndex, PATH=$pathIndex")

                if (titleIndex < 0 || artistIndex < 0 || durationIndex < 0 || pathIndex < 0) {
                    android.util.Log.e("MusicScanner", "❌ Columnas no encontradas en el cursor")
                    return@use
                }

                while (cursor.moveToNext()) {
                    try {
                        val title = cursor.getString(titleIndex) ?: "Desconocido"
                        val artist = cursor.getString(artistIndex) ?: "Artista desconocido"
                        val duration = cursor.getLong(durationIndex)
                        val filePath = cursor.getString(pathIndex) ?: ""

                        // ✅ Verificar que el archivo existe y es válido
                        if (filePath.isNotEmpty() && duration > 0) {
                            val song = Song(
                                title = title,
                                artist = artist,
                                duration = (duration / 1000).toInt(),
                                filePath = filePath
                            )
                            list.add(song)
                            android.util.Log.d("MusicScanner", "📁 Encontrada: $title - $artist (${song.duration}s)")
                        } else {
                            android.util.Log.d("MusicScanner", "⚠️ Archivo inválido: $filePath, duración: $duration")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MusicScanner", "❌ Error en fila: ${e.message}")
                    }
                }
            } ?: run {
                android.util.Log.e("MusicScanner", "❌ Cursor es null")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("MusicScanner", "❌ Error de seguridad: ${e.message}")
            android.util.Log.e("MusicScanner", "❌ Probablemente faltan permisos")
        } catch (e: Exception) {
            android.util.Log.e("MusicScanner", "❌ Error escaneando: ${e.message}")
            e.printStackTrace()
        }

        android.util.Log.d("MusicScanner", "✅ Total canciones encontradas: ${list.size}")
        return list
    }
}