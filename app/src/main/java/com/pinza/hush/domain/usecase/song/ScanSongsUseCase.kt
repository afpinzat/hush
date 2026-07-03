package com.pinza.hush.domain.usecase.song

import androidx.compose.remote.creation.dsl.first
import com.pinza.hush.data.scanner.MusicScanner
import com.pinza.hush.domain.repository.ISongRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanSongsUseCase @Inject constructor(
    private val scanner: MusicScanner,
    private val repository: ISongRepository
) {

    suspend operator fun invoke() {
        android.util.Log.d("ScanSongsUseCase", "🔍 Iniciando escaneo...")

        try {
            // 1. Escanear canciones del dispositivo
            val scannedSongs = scanner.scan()
            android.util.Log.d("ScanSongsUseCase", "📋 Escaneadas: ${scannedSongs.size} canciones")

            // 2. Obtener canciones existentes en Room
            val existingSongs = repository.getSongs().first()
            android.util.Log.d("ScanSongsUseCase", "📋 Existentes en Room: ${existingSongs.size} canciones")

            // 3. Mapa de canciones existentes por filePath
            val existingByPath = existingSongs.associateBy { it.filePath }
            val scannedPaths = scannedSongs.map { it.filePath }.toSet()

            // 4. Eliminar canciones que ya no existen en el dispositivo
            val toDelete = existingSongs.filter { it.filePath !in scannedPaths }
            if (toDelete.isNotEmpty()) {
                android.util.Log.d("ScanSongsUseCase", "🗑️ Eliminando: ${toDelete.size} canciones que ya no existen")
                toDelete.forEach { repository.delete(it) }
            }

            // 5. Insertar SOLO canciones nuevas (preserva ediciones)
            val toInsert = scannedSongs.filter { it.filePath !in existingByPath.keys }
            if (toInsert.isNotEmpty()) {
                android.util.Log.d("ScanSongsUseCase", "➕ Insertando: ${toInsert.size} canciones nuevas")
                repository.insertAll(toInsert)
            }

            // 6. Actualizar canciones existentes (opcional: actualizar duración, etc.)
            // Si quieres actualizar metadata sin perder ediciones:
            val toUpdate = scannedSongs.filter { it.filePath in existingByPath.keys }
            if (toUpdate.isNotEmpty()) {
                android.util.Log.d("ScanSongsUseCase", "🔄 Actualizando metadata de: ${toUpdate.size} canciones")
                toUpdate.forEach { scanned ->
                    val existing = existingByPath[scanned.filePath]
                    if (existing != null) {
                        // Solo actualizar campos que no hayan sido editados manualmente
                        // Por ahora, no actualizamos nada para preservar ediciones
                        // Pero podrías actualizar duración si cambia
                    }
                }
            }

            android.util.Log.d("ScanSongsUseCase", "✅ Escaneo completado")

        } catch (e: Exception) {
            android.util.Log.e("ScanSongsUseCase", "❌ Error en escaneo: ${e.message}")
        }
    }
}