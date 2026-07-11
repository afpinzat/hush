package com.pinza.hush.domain.usecase.system

import com.pinza.hush.data.scanner.FileScanner
import com.pinza.hush.domain.repository.ISongRepository
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val repository: ISongRepository,
    private val mediaScanner: FileScanner // Tu clase que lee archivos del cel
) {
    suspend operator fun invoke() {
        val songs = mediaScanner.scanAudioFiles() // Obtiene lista de archivos
        repository.insertAll(songs)     // Inserta en BD
    }
}