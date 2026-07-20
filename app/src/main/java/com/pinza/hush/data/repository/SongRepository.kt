package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.dao.SongLyricsDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.data.source.local.MusicScanner
import com.pinza.hush.domain.repository.ISongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SongRepository @Inject constructor(
    private val dao: SongDao,
    private val lyricsDao: SongLyricsDao,
    private val musicScanner: MusicScanner
) : ISongRepository {

    override fun getSongs() = dao.getAllSongs()

    override fun getAlbums() = dao.getAllAlbums()

    override fun getArtists() = dao.getAllArtists()

    override fun getSongsByAlbum(albumName: String) = dao.getSongsByAlbum(albumName)

    override fun getSongsByArtist(artistName: String) = dao.getSongsByArtist(artistName)

    override suspend fun insertAll(songs: List<Song>) = dao.insertAll(songs)

    override suspend fun scanAndSaveMusic() {
        val scannnedSongs = musicScanner.scanAudioFiles()
        // Opcional: Solo insertar las que no están en la DB comparando por filePath
        dao.insertAll(scannnedSongs)
    }

    override suspend fun getAllFilePaths() = dao.getAllFilePaths() // Ahora es una suspend fun en el DAO

    override suspend fun delete(song: Song) {
        // En lugar de borrar físicamente, la ocultamos para que el escáner
        // no la vuelva a meter cada vez que se inicia la app.
        dao.hideSong(song.id)
    }

    override suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) {
        dao.setFavorite(songId, isFavorite)
    }

    override fun searchAllSongs(query: String): Flow<List<Song>> {
        return dao.searchAll(query)
    }
    override fun getFavoriteSongs() = dao.getFavoriteSongs()

    override suspend fun isFavorite(songId: Long) = dao.isFavorite(songId)

    override fun searchFavorites(query: String) = dao.searchFavorites(query)

    override fun getSongById(songId: Long): Flow<Song?> = dao.getSongFlowById(songId)

    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun updateSong(song: Song) = dao.update(song)

    override suspend fun getLyrics(songId: Long): String? {
        // Al ser PrimaryKey, accedemos directamente
        return lyricsDao.getLyricsSingle(songId)?.lyrics
    }

    override suspend fun getSongLyrics(songId: Long): SongLyrics? {
        return lyricsDao.getLyricsSingle(songId)
    }

    override fun getLyricsFlow(songId: Long): Flow<String?> {
        return lyricsDao.getLyricsFlow(songId).map { it?.lyrics }
    }

    override fun getSongLyricsFlow(songId: Long): Flow<SongLyrics?> {
        return lyricsDao.getLyricsFlow(songId)
    }

    override suspend fun checkAndExtractLyrics(song: Song) {
        val existing = lyricsDao.getLyricsSingle(song.id)
        if (existing == null || existing.lyrics.isBlank()) {
            val extracted = musicScanner.extractLyrics(song.filePath)
            if (!extracted.isNullOrBlank()) {
                saveLyrics(song.id, extracted)
            }
        }
    }

    override suspend fun getSongsByPlaylist(playlistId: Long): List<Song> {
        // Aquí es donde el error suele ocurrir si no has hecho el cambio en el DAO
        return dao.getSongsByPlaylist(playlistId)
    }

    override suspend fun saveLyrics(songId: Long, lyrics: String) {
        // Como simplificamos el modelo, la creación es mínima
        val entry = SongLyrics(
            songId = songId,
            lyrics = lyrics,
            source = "Local",
            language = null
        )
        lyricsDao.insert(entry)
    }

    override suspend fun saveSongLyrics(lyrics: SongLyrics) {
        lyricsDao.insert(lyrics)
    }

}