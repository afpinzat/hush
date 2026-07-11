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
import com.pinza.hush.databinding.FragmentArtistsBinding
import com.pinza.hush.ui.adapter.ArtistAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArtistsFragment : Fragment(R.layout.fragment_artists) {
    
    private val viewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var artistAdapter: ArtistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistsBinding.bind(view)
        
        setupRecyclerView()
        observeArtists()
    }

    private fun setupRecyclerView() {
        artistAdapter = ArtistAdapter { artist ->
            val bundle = Bundle().apply {
                putString("name", artist.artist)
                putString("type", "artist")
            }
            findNavController().navigate(R.id.libraryDetailFragment, bundle)
        }
        binding.rvArtists.apply {
            adapter = artistAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeArtists() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    artistAdapter.submitList(state.artists)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
