package com.pinza.hush.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AlbumArtExtractor {

    /**
     * Extrae la carátula de un archivo MP3 y la guarda en caché
     * @param filePath Ruta del archivo MP3
     * @param context Contexto de la aplicación
     * @return Ruta del archivo de carátula guardado, o null si no hay
     */
    fun extractAndSaveAlbumArt(filePath: String, context: Context): String? {
        return try {
            val retriever = MediaMetadataRetriever()

            // ✅ Usar setDataSource con Uri para evitar problemas de permisos
            try {
                // Intentar con Uri
                val uri = Uri.fromFile(File(filePath))
                retriever.setDataSource(context, uri)
            } catch (e: SecurityException) {
                // Si falla, intentar con la ruta directamente
                android.util.Log.w("AlbumArtExtractor", "⚠️ Falló con Uri, intentando con ruta directa")
                retriever.setDataSource(filePath)
            }

            // Extraer carátula como array de bytes
            val artBytes = retriever.embeddedPicture
            retriever.release()

            if (artBytes == null || artBytes.isEmpty()) {
                android.util.Log.d("AlbumArtExtractor", "No hay carátula para: $filePath")
                return null
            }

            // Convertir bytes a Bitmap
            val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            if (bitmap == null) {
                android.util.Log.d("AlbumArtExtractor", "Error decodificando imagen")
                return null
            }

            // Guardar en caché
            val cacheDir = context.cacheDir
            val fileName = "album_art_${filePath.hashCode()}.jpg"
            val outputFile = File(cacheDir, fileName)

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            android.util.Log.d("AlbumArtExtractor", "Carátula guardada: ${outputFile.absolutePath}")
            outputFile.absolutePath

        } catch (e: SecurityException) {
            android.util.Log.e("AlbumArtExtractor", "❌ Error de seguridad: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("AlbumArtExtractor", "❌ Error extrayendo carátula: ${e.message}")
            null
        }
    }

    /**
     * Extrae metadatos adicionales del MP3
     */
    fun extractMetadata(filePath: String, context: Context): MetadataResult {
        return try {
            val retriever = MediaMetadataRetriever()

            try {
                val uri = Uri.fromFile(File(filePath))
                retriever.setDataSource(context, uri)
            } catch (e: SecurityException) {
                retriever.setDataSource(filePath)
            }

            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            retriever.release()

            MetadataResult(
                album = album,
                year = year?.toIntOrNull()
            )
        } catch (e: Exception) {
            android.util.Log.e("AlbumArtExtractor", "Error extrayendo metadatos: ${e.message}")
            MetadataResult(null, null)
        }
    }

    data class MetadataResult(
        val album: String?,
        val year: Int?
    )
}