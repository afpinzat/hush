package com.pinza.hush.ui.state

import com.pinza.hush.data.local.model.Song

data class SongUiState(

    val songs: List<Song> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null

)