package com.pinza.hush.ui.state

import com.pinza.hush.data.local.model.Playlist

data class PlaylistUiState(

    val playlists: List<Playlist> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null

)