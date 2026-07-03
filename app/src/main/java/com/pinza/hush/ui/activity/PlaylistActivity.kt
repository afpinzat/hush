package com.pinza.hush.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pinza.hush.databinding.ActivityPlaylistBinding
import com.pinza.hush.ui.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.pinza.hush.ui.adapter.PlaylistListAdapter

@AndroidEntryPoint
class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistBinding
    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var adapter: PlaylistListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button con OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Playlists"
    }

    private fun setupRecyclerView() {
        adapter = PlaylistListAdapter(
            onPlaylistClick = { playlist ->
                val intent = PlaylistDetailActivity.newIntent(this, playlist.id)
                startActivity(intent)
            }
        )
        binding.rvPlaylists.apply {
            layoutManager = LinearLayoutManager(this@PlaylistActivity)
            adapter = this@PlaylistActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.btnCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.playlists.collect { playlists ->
                adapter.submitList(playlists)
                binding.tvEmptyPlaylists.visibility = if (playlists.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Mostrar/ocultar loading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    // Mostrar error
                    viewModel.clearError()
                }
            }
        }
    }

    private fun showCreatePlaylistDialog() {
        val input = TextInputEditText(this).apply {
            hint = "Nombre de la playlist"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Crear Playlist")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createPlaylist(name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}