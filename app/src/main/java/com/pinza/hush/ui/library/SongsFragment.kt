package com.pinza.hush.ui.library

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.FragmentSongsBinding
import com.pinza.hush.ui.adapter.SongAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SongsFragment : Fragment(R.layout.fragment_songs) {

    private val viewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSongsBinding.bind(view)

        setupRecyclerView()
        observeSongs()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song ->
                val songs = songAdapter.currentList
                // Optimización: comparar por id (Long) en vez de equals() completo del data class
                val index = songs.indexOfFirst { it.id == song.id }
                viewModel.playWithQueue(songs, if (index != -1) index else 0)
            },
            onMoreOptionsClick = { song -> showSongOptions(song) }
        )
        binding.rvSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showSongOptions(song: Song) {
        val options = arrayOf("Agregar a cola", "Editar canción", "Editar letras", "Borrar canción")
        AlertDialog.Builder(requireContext())
            .setTitle(song.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.addToQueueNext(song)
                    1 -> showEditSongDialog(song)
                    2 -> showEditLyricsDialog(song)
                    3 -> showDeleteConfirmDialog(song)
                }
            }
            .show()
    }

    private fun showEditSongDialog(song: Song) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_song, null)
        val etTitle = view.findViewById<EditText>(R.id.et_title)
        val etArtist = view.findViewById<EditText>(R.id.et_artist)

        etTitle.setText(song.title)
        etArtist.setText(song.artist)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar canción")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val newTitle = etTitle.text.toString()
                val newArtist = etArtist.text.toString()
                if (newTitle.isNotBlank() && newArtist.isNotBlank()) {
                    viewModel.updateSong(song.copy(title = newTitle, artist = newArtist))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditLyricsDialog(song: Song) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_lyrics, null)
        val etLyrics = view.findViewById<EditText>(R.id.et_lyrics)

        lifecycleScope.launch {
            val currentLyrics = viewModel.getLyrics(song.id)
            etLyrics.setText(currentLyrics)

            AlertDialog.Builder(requireContext())
                .setTitle("Editar letras (LRC)")
                .setView(view)
                .setPositiveButton("Guardar") { _, _ ->
                    viewModel.saveLyrics(song.id, etLyrics.text.toString())
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showDeleteConfirmDialog(song: Song) {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar canción")
            .setMessage("¿Estás seguro de que deseas borrar '${song.title}'?")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.deleteSong(song)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeSongs() {
        // Optimización: un solo repeatOnLifecycle registrando un único observer de
        // ciclo de vida, con los dos collectors corriendo dentro como hijos
        // (antes eran dos repeatOnLifecycle independientes = doble overhead de lifecycle)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.songs.collect { songs ->
                        songAdapter.submitList(songs)
                    }
                }

                // Resaltar canción actual
                launch {
                    viewModel.currentSongState.collect { song ->
                        songAdapter.setCurrentPlayingId(song?.id ?: -1)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}