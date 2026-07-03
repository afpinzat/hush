package com.pinza.hush.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.domain.player.IPlayerManager
import com.pinza.hush.domain.repository.ISongLyricsRepository
import com.pinza.hush.domain.usecase.song.DeleteSongUseCase
import com.pinza.hush.domain.usecase.song.GetSongUseCase
import com.pinza.hush.domain.usecase.song.UpdateSongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongViewModel @Inject constructor(
    private val getSongUseCase: GetSongUseCase,
    private val updateSongUseCase: UpdateSongUseCase,
    private val deleteSongUseCase: DeleteSongUseCase,
    private val lyricsRepository: ISongLyricsRepository,
    private val playerManager: IPlayerManager  // ✅ Usamos IPlayerManager en lugar de PlayerViewModel
) : ViewModel() {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentSongLyrics = MutableStateFlow<SongLyrics?>(null)
    val currentSongLyrics: StateFlow<SongLyrics?> = _currentSongLyrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editingSong = MutableStateFlow<Song?>(null)
    val editingSong: StateFlow<Song?> = _editingSong.asStateFlow()

    fun loadSong(songId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val song = getSongUseCase(songId)
                _currentSong.value = song

                song?.let {
                    val lyrics = lyricsRepository.getLyrics(it.id).first()
                    _currentSongLyrics.value = lyrics
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updateSong(song: Song) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                updateSongUseCase(song)
                _currentSong.value = song
                _isEditing.value = false
                _editingSong.value = null
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                deleteSongUseCase(song)
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = null
                    _currentSongLyrics.value = null
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun saveLyrics(songId: Int, lyrics: String, source: String = "manual", language: String = "es") {
        viewModelScope.launch {
            try {
                val songLyrics = SongLyrics(
                    songId = songId,
                    lyrics = lyrics,
                    source = source,
                    language = language
                )
                lyricsRepository.insert(songLyrics)

                if (_currentSong.value?.id == songId) {
                    _currentSongLyrics.value = songLyrics
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteLyrics() {
        viewModelScope.launch {
            try {
                val lyrics = _currentSongLyrics.value
                lyrics?.let {
                    lyricsRepository.delete(it)
                    _currentSongLyrics.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun startEditing() {
        _isEditing.value = true
        _editingSong.value = _currentSong.value
    }

    fun cancelEditing() {
        _isEditing.value = false
        _editingSong.value = null
    }

    fun playSong() {
        _currentSong.value?.let { song ->
            playerManager.play(song)
        }
    }

    fun clearError() {
        _error.value = null
    }
}