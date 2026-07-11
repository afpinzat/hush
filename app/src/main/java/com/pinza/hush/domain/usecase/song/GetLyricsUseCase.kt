package com.pinza.hush.domain.usecase.song

import com.pinza.hush.data.scanner.FileScanner
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val repository: ISongRepository,
    private val fileScanner: FileScanner
) {
    suspend operator fun invoke(songId: Long, filePath: String): String? {
        // 1. Primero intentamos leer el LRC si el usuario lo creó manualmente
        val lrcContent = fileScanner.getLrcContent(filePath)
        if (lrcContent != null) return lrcContent

        // 2. Si no hay LRC, intentamos leer la letra estática de la base de datos (Room)
        val cachedLyrics = repository.getLyrics(songId)
        if (cachedLyrics != null) return cachedLyrics

        // 3. Si no hay nada, intentamos extraer de los metadatos del archivo
        val fileLyrics = fileScanner.getLyricsFromFile(filePath)

        // 4. Guardamos en Room para la próxima vez
        if (fileLyrics != null) {
            repository.saveLyrics(songId, fileLyrics)
        }

        return fileLyrics
    }
}