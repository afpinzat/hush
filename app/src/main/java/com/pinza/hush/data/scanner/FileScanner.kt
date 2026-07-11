package com.pinza.hush.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.pinza.hush.data.local.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class FileScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scanAudioFiles(): List<Song> {
        val list = mutableListOf<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        context.contentResolver.query(uri, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null)?.use { cursor ->
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                list.add(Song(
                    title = cursor.getString(titleIdx) ?: "Desconocido",
                    artist = cursor.getString(artistIdx) ?: "Artista desconocido",
                    filePath = cursor.getString(dataIdx) ?: "",
                    duration = (cursor.getLong(durIdx) / 1000).toInt()
                ))
            }
        }
        return list
    }

    fun getLrcContent(audioPath: String): String? {
        // 1. Convertimos la ruta del audio a un objeto File
        val audioFile = File(audioPath)

        // 2. Construimos la ruta del posible archivo .lrc
        // Ejemplo: /musica/cancion.mp3 -> /musica/cancion.lrc
        val lrcFile = File(audioFile.parent ?: "", "${audioFile.nameWithoutExtension}.lrc")

        // 3. Si existe, lo leemos
        return if (lrcFile.exists()) {
            try {
                lrcFile.readText()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Usamos el número 17 porque es el valor constante de METADATA_KEY_LYRICS
    fun getLyricsFromFile(path: String): String? {
        return MediaMetadataRetriever().apply {
            try { setDataSource(path) } catch (e: Exception) { return null }
        }.extractMetadata(17) // 17 = METADATA_KEY_LYRICS
    }
}