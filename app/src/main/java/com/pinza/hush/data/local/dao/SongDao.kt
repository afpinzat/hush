package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Cambiado a IGNORE para no pisar ediciones
    suspend fun insertAll(songs: List<Song>)

    @Query("""
        SELECT s.* FROM song s
        INNER JOIN playlist_song_join ref ON s.id = ref.songId
        WHERE ref.playlistId = :playlistId AND s.is_hidden = 0
    """)
    suspend fun getSongsByPlaylist(playlistId: Long): List<Song>

    @Update
    suspend fun update(song: Song)

    // En lugar de borrar físicamente, ocultamos para que el escáner no la traiga de vuelta
    @Query("UPDATE song SET is_hidden = 1 WHERE id = :songId")
    suspend fun hideSong(songId: Long)

    @Delete
    suspend fun delete(song: Song)

    @Query("DELETE FROM song")
    suspend fun deleteAll()

    @Query("SELECT * FROM song WHERE is_hidden = 0 ORDER BY title ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT file_path FROM song")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT * FROM song WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM song WHERE id = :id")
    fun getSongFlowById(id: Long): Flow<Song?>

    @Query("SELECT DISTINCT album, album_art FROM song WHERE album IS NOT NULL AND is_hidden = 0 ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<AlbumSummary>>

    @Query("SELECT artist, MIN(album_art) as albumArt FROM song WHERE artist IS NOT NULL AND is_hidden = 0 GROUP BY artist ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<ArtistSummary>>

    @Query("SELECT * FROM song WHERE album = :albumName AND is_hidden = 0 ORDER BY title ASC")
    fun getSongsByAlbum(albumName: String): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE artist = :artistName AND is_hidden = 0 ORDER BY title ASC")
    fun getSongsByArtist(artistName: String): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') AND is_hidden = 0")
    fun searchAll(query: String): Flow<List<Song>>


    // ✅ Marcar como favorito
    @Query("UPDATE song SET is_favorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFavorite: Boolean)

    // ✅ Obtener solo los favoritos
    @Query("SELECT * FROM song WHERE is_favorite = 1 AND is_hidden = 0 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<Song>>

    // ✅ Verificar si es favorito
    @Query("SELECT is_favorite FROM song WHERE id = :songId")
    suspend fun isFavorite(songId: Long): Boolean

    // ✅ Búsqueda dentro de favoritos
    @Query("SELECT * FROM song WHERE is_favorite = 1 AND is_hidden = 0 AND (title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%')")
    fun searchFavorites(query: String): Flow<List<Song>>

    data class AlbumSummary(
        val album: String,
        @ColumnInfo(name = "album_art") val albumArt: String?
    )

    data class ArtistSummary(
        val artist: String,
        @ColumnInfo(name = "albumArt") val albumArt: String?
    )
}
