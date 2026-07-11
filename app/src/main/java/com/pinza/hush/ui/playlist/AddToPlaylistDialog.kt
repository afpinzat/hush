package com.pinza.hush.ui.playlist

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddToPlaylistDialog : DialogFragment() {

    private val viewModel: PlaylistDialogViewModel by viewModels()

    companion object {
        private const val ARG_SONG_ID = "song_id"
        fun newInstance(songId: Long) = AddToPlaylistDialog().apply {
            arguments = Bundle().apply { putLong(ARG_SONG_ID, songId) }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val songId = arguments?.getLong(ARG_SONG_ID) ?: return super.onCreateDialog(savedInstanceState)
        
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Agregar a Playlist")
            .setAdapter(adapter) { _, position ->
                // Este listener se maneja abajo dinámicamente
            }
        
        val dialog = builder.create()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collectLatest { playlists ->
                    val items = mutableListOf<String>()
                    items.add("+ Crear nueva playlist")
                    items.addAll(playlists.map { it.playlist.name })

                    adapter.clear()
                    adapter.addAll(items)
                    adapter.notifyDataSetChanged()

                    dialog.listView.setOnItemClickListener { _, _, position, _ ->
                        if (position == 0) {
                            showCreatePlaylistDialog(songId)
                        } else {
                            val playlistId = playlists[position - 1].playlist.id
                            viewModel.addSongToPlaylist(playlistId, songId)
                            dismiss()
                        }
                    }
                }
            }
        }
        
        return dialog
    }

    private fun showCreatePlaylistDialog(songId: Long) {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Nueva Playlist")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createPlaylistAndAddSong(name, songId)
                    dismiss()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
