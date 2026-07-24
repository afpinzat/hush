package com.pinza.hush.ui.playlist

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.activityViewModels
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.databinding.FragmentPlaylistsBinding
import com.pinza.hush.ui.adapter.PlaylistAdapter
import com.pinza.hush.ui.library.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {

    private val viewModel: PlaylistViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlaylistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaylistsBinding.bind(view)

        setupRecyclerView()
        observePlaylists()

        binding.btnAddPlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val bundle = Bundle().apply {
                    putString("name", playlist.name)
                    putString("type", "playlist")
                    putLong("playlist_id", playlist.id)
                }
                findNavController().navigate(R.id.libraryDetailFragment, bundle)
            },
            onPlaylistLongClick = { playlist ->
                showDeletePlaylistDialog(playlist)
            }
        )
        binding.rvPlaylists.apply {
            adapter = this@PlaylistsFragment.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(10)
        }
    }

    private fun showDeletePlaylistDialog(playlist: Playlist) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Playlist")
            .setMessage("¿Estás seguro de que deseas eliminar la playlist '${playlist.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deletePlaylist(playlist)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCreatePlaylistDialog() {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Nueva Playlist")
            .setView(editText)
            .setPositiveButton("Crear") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observePlaylists() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlistsState.collect { state ->
                    val query = libraryViewModel.searchQuery.value
                    val filtered = if (query.isBlank()) {
                        state.playlists
                    } else {
                        state.playlists.filter { it.playlist.name.contains(query, ignoreCase = true) }
                    }
                    adapter.submitList(filtered)
                }
            }
        }

        // También observar cambios en la búsqueda para filtrar inmediatamente
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                libraryViewModel.searchQuery.collect { query ->
                    val currentState = viewModel.playlistsState.value
                    val filtered = if (query.isBlank()) {
                        currentState.playlists
                    } else {
                        currentState.playlists.filter { it.playlist.name.contains(query, ignoreCase = true) }
                    }
                    adapter.submitList(filtered)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}