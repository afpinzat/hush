package com.pinza.hush.utils

import androidx.media3.common.MediaItem
import com.pinza.hush.data.local.model.Song

fun MediaItem.toSong(): Song {
    // Aquí extraemos los datos que guardaste al crear el MediaItem
    val extras = this.mediaMetadata

    return Song(
        id = this.mediaId.toLongOrNull() ?: 0L,
        title = extras.title?.toString() ?: "Desconocido",
        artist = extras.artist?.toString() ?: "Desconocido",
        duration = 0, // El MediaItem no siempre trae la duración, puedes ignorarlo o manejarlo
        filePath = this.requestMetadata.mediaUri.toString(),
        albumArt = extras.artworkUri?.toString(),
        album = extras.albumTitle?.toString()
    )
}