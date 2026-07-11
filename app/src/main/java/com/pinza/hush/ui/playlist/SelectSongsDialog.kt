package com.pinza.hush.ui.playlist

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.pinza.hush.ui.library.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectSongsDialog : DialogFragment() {

    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private val playlistViewModel: PlaylistDialogViewModel by viewModels()

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"
        fun newInstance(playlistId: Long) = SelectSongsDialog().apply {
            arguments = Bundle().apply { putLong(ARG_PLAYLIST_ID, playlistId) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlistId = arguments?.getLong(ARG_PLAYLIST_ID) ?: -1L
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar Canciones")

        val allSongs = libraryViewModel.uiState.value.songs
        val songTitles = allSongs.map { "${it.title} - ${it.artist}" }.toTypedArray()
        val checkedItems = BooleanArray(allSongs.size) { false }

        builder.setMultiChoiceItems(songTitles, checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }

        builder.setPositiveButton("Agregar") { _, _ ->
            allSongs.forEachIndexed { index, song ->
                if (checkedItems[index]) {
                    playlistViewModel.addSongToPlaylist(playlistId, song.id)
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)

        return builder.create()
    }
}
