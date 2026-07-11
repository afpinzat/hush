package com.pinza.hush.ui.library

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.R
import com.pinza.hush.databinding.FragmentLibraryDetailBinding
import com.pinza.hush.ui.adapter.SongAdapter
import com.pinza.hush.ui.playlist.SelectSongsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryDetailFragment : Fragment(R.layout.fragment_library_detail) {

    private val viewModel: LibraryViewModel by activityViewModels()
    
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
            viewModel.loadPlaylistSongs(playlistId)
        } else {
            viewModel.loadDetail(name, type)
        }
    }

    private fun setupUI(name: String, type: String) {
        binding.tvTitle.text = name
        binding.toolbar.setNavigationIcon(R.drawable.ic_expand_more_24)
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
        songAdapter = SongAdapter(
            onSongClick = { song ->
                val songs = songAdapter.currentList
                val index = songs.indexOf(song)
                viewModel.playWithQueue(songs, if (index != -1) index else 0)
            },
            onMoreOptionsClick = { song -> viewModel.addToQueueNext(song) }
        )
        binding.rvSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    songAdapter.submitList(state.detailSongs)
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
