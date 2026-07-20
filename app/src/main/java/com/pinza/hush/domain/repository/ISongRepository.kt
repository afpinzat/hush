package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import kotlinx.coroutines.flow.Flow

interface ISongRepository {
    // Escaneo y Datos
    fun getSongs(): Flow<List<Song>>
    fun getAlbums(): Flow<List<SongDao.AlbumSummary>>
    fun getArtists(): Flow<List<SongDao.ArtistSummary>>
    fun getSongsByAlbum(albumName: String): Flow<List<Song>>
    fun getSongsByArtist(artistName: String): Flow<List<Song>>
    suspend fun insertAll(songs: List<Song>)
    suspend fun scanAndSaveMusic()
    suspend fun getAllFilePaths(): List<String>

    suspend fun getSongsByPlaylist(playlistId: Long): List<Song>    // CRUD básico
    suspend fun delete(song: Song)
    suspend fun deleteAll()
    suspend fun updateSong(song: Song) // Agrega esto
     fun getFavoriteSongs(): Flow<List<Song>>

    fun searchAllSongs(query: String): Flow<List<Song>>
    suspend fun isFavorite(songId: Long): Boolean

    fun searchFavorites(query: String): Flow<List<Song>>

    fun getSongById(songId: Long): Flow<Song?>

    // Favoritos
    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) // Agrega esto

    suspend fun getLyrics(songId: Long): String?
    suspend fun getSongLyrics(songId: Long): SongLyrics?
    fun getSongLyricsFlow(songId: Long): Flow<SongLyrics?>

    suspend fun checkAndExtractLyrics(song: Song)

    fun getLyricsFlow(songId: Long): Flow<String?>

    suspend fun saveLyrics(songId: Long, lyrics: String)
    suspend fun saveSongLyrics(lyrics: SongLyrics)
}