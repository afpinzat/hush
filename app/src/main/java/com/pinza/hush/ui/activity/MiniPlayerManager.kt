package com.pinza.hush.ui.activity

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.pinza.hush.R
import com.pinza.hush.ui.viewmodel.LibraryViewModel

class MiniPlayerManager(
    private val rootView: View,
    private val libraryViewModel: LibraryViewModel
) {

    private val tvSongTitle: TextView? = rootView.findViewById(R.id.tv_song_title)
    private val tvArtist: TextView? = rootView.findViewById(R.id.tv_artist)
    private val btnPlayPause: ImageButton? = rootView.findViewById(R.id.btn_play_pause)
    private val btnNext: ImageButton? = rootView.findViewById(R.id.btn_next)

    fun updateState(songTitle: String, artist: String, isPlaying: Boolean) {
        android.util.Log.d("MiniPlayer", "🎵 updateState: $songTitle - $artist - $isPlaying")

        tvSongTitle?.text = songTitle
        tvArtist?.text = artist

        val icon = if (isPlaying) {
            R.drawable.ic_pause_24
        } else {
            R.drawable.ic_play_arrow_24
        }
        btnPlayPause?.setImageResource(icon)
    }

    fun setOnClickListener(listener: View.OnClickListener) {
        rootView.setOnClickListener(listener)

        btnPlayPause?.setOnClickListener {
            libraryViewModel.togglePlayPause()
            val icon = if (libraryViewModel.isPlaying.value) {
                R.drawable.ic_pause_24
            } else {
                R.drawable.ic_play_arrow_24
            }
            btnPlayPause?.setImageResource(icon)
        }

        btnNext?.setOnClickListener {
            libraryViewModel.nextSong()
        }
    }

    fun updatePlayPauseIcon(isPlaying: Boolean) {
        val icon = if (isPlaying) {
            R.drawable.ic_pause_24
        } else {
            R.drawable.ic_play_arrow_24
        }
        btnPlayPause?.setImageResource(icon)
    }
}