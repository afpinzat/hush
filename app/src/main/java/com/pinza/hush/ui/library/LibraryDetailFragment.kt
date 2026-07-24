package com.pinza.hush.ui.library

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.databinding.FragmentLibraryDetailBinding
import androidx.fragment.app.viewModels
import com.pinza.hush.ui.adapter.SongAdapter
import com.pinza.hush.ui.playlist.SelectSongsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.graphics.Color
import android.content.res.ColorStateList

@AndroidEntryPoint
class LibraryDetailFragment : Fragment(R.layout.fragment_library_detail) {

    private val viewModel: LibraryViewModel by activityViewModels()
    private val detailViewModel: LibraryDetailViewModel by viewModels()
    
    private var _binding: FragmentLibraryDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLibraryDetailBinding.bind(view)

        val name = arguments?.getString("name") ?: ""
        val type = arguments?.getString("type") ?: ""
        val playlistId = arguments?.getLong("playlist_id") ?: -1L

        setupUI(name, type)
        setupRecyclerView()
        observeUiState()

        // Cargar los datos específicos
        if (type == "playlist") {
            detailViewModel.loadPlaylistSongs(playlistId)
        } else {
            detailViewModel.loadDetail(name, type)
        }
    }

    private fun setupUI(name: String, type: String) {
        binding.tvTitle.text = name
        binding.toolbar.setNavigationIcon(R.drawable.ic_expand_more_24)
        binding.toolbar.navigationIcon?.setTint(Color.parseColor("#4F378B"))
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        
        // Mostrar botón de agregar solo si es una playlist
        binding.btnAddSongsToPlaylist.isVisible = type == "playlist"
        binding.btnAddSongsToPlaylist.setOnClickListener {
            val playlistId = arguments?.getLong("playlist_id") ?: -1L
            if (playlistId != -1L) {
                SelectSongsDialog.newInstance(playlistId).show(childFragmentManager, "SelectSongsDialog")
            }
        }
    }

    private fun setupRecyclerView() {
        val type = arguments?.getString("type") ?: ""
        val playlistId = arguments?.getLong("playlist_id") ?: -1L

        songAdapter = SongAdapter(
            onSongClick = { song ->
                val songs = songAdapter.currentList
                val index = songs.indexOfFirst { it.id == song.id }
                viewModel.playWithQueue(songs, if (index != -1) index else 0)
            },
            onMoreOptionsClick = { song ->
                showSongOptions(song, type, playlistId)
            }
        )
        binding.rvSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
    }

    private fun showSongOptions(song: Song, type: String, playlistId: Long) {
        val options = mutableListOf("Agregar a cola", "Editar canción", "Editar letras", "Borrar canción")
        if (type == "playlist") {
            options.add("Eliminar de playlist")
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(song.title)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Agregar a cola" -> viewModel.addToQueueNext(song)
                    "Editar canción" -> showEditSongDialog(song)
                    "Editar letras" -> showEditLyricsDialog(song)
                    "Borrar canción" -> showDeleteConfirmDialog(song)
                    "Eliminar de playlist" -> {
                        if (playlistId != -1L) {
                            detailViewModel.removeSongFromPlaylist(playlistId, song.id)
                        }
                    }
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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailViewModel.uiState.collect { state ->
                    songAdapter.submitList(state.songs)
                    binding.progressBar.isVisible = state.isLoading
                }
            }
        }

        // Resaltar canción actual
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSongState.collect { song ->
                    songAdapter.setCurrentPlayingId(song?.id ?: -1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
