package com.pinza.hush.ui.library

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.R
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
                val index = songs.indexOf(song)
                viewModel.playWithQueue(songs, if (index != -1) index else 0)
            },
            onMoreOptionsClick = { /* Opcional: implementar opciones similares a SongsFragment */ }
        )
        binding.rvSongs.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteSongs.collect { favorites ->
                    songAdapter.submitList(favorites)
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
