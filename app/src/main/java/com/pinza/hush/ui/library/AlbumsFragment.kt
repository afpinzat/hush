package com.pinza.hush.ui.library

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.navigation.fragment.findNavController
import com.pinza.hush.R
import com.pinza.hush.databinding.FragmentAlbumsBinding
import com.pinza.hush.ui.adapter.AlbumAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlbumsFragment : Fragment(R.layout.fragment_albums) {
    
    private val viewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private lateinit var albumAdapter: AlbumAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlbumsBinding.bind(view)
        
        setupRecyclerView()
        observeAlbums()
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumAdapter { album ->
            val bundle = Bundle().apply {
                putString("name", album.album)
                putString("type", "album")
            }
            findNavController().navigate(R.id.libraryDetailFragment, bundle)
        }
        binding.rvAlbums.apply {
            adapter = albumAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeAlbums() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    albumAdapter.submitList(state.albums)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
