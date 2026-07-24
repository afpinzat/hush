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
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.databinding.FragmentSongsBinding
import com.pinza.hush.ui.adapter.SongAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_songs) {
    
    private val viewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSongsBinding.bind(view)
        
        setupRecyclerView()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song -> 
                val songs = songAdapter.currentList
                val index = songs.indexOfFirst { it.id == song.id }
                viewModel.playWithQueue(songs, if (index != -1) index else 0)
            },
            onMoreOptionsClick = { song -> showSongOptions(song) }
        )
        binding.rvSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(20)
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
        val etLyrics2 = view.findViewById<EditText>(R.id.et_lyrics2)
        val switchPrimary = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_primary_second)
        
        lifecycleScope.launch {
            val songLyrics = viewModel.getSongLyrics(song.id)
            etLyrics.setText(songLyrics?.lyrics ?: "")
            etLyrics2.setText(songLyrics?.lyrics2 ?: "")
            switchPrimary.isChecked = songLyrics?.isPrimarySecond ?: false

            AlertDialog.Builder(requireContext())
                .setTitle("Editar letras (LRC)")
                .setView(view)
                .setPositiveButton("Guardar") { _, _ ->
                    val newLyrics = SongLyrics(
                        songId = song.id,
                        lyrics = etLyrics.text.toString(),
                        lyrics2 = etLyrics2.text.toString(),
                        isPrimarySecond = switchPrimary.isChecked
                    )
                    viewModel.saveSongLyrics(newLyrics)
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

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteSongs.collect { favorites ->
                        songAdapter.submitList(favorites)
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
