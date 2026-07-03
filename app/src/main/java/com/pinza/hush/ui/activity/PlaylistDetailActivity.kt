package com.pinza.hush.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ActivityPlaylistDetailBinding
import com.pinza.hush.ui.viewmodel.PlayerViewModel
import com.pinza.hush.ui.viewmodel.PlaylistDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.pinza.hush.ui.adapter.PlaylistSongsAdapter

@AndroidEntryPoint
class PlaylistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private val viewModel: PlaylistDetailViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private lateinit var adapter: PlaylistSongsAdapter
    private var playlistId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playlistId = intent.getIntExtra("playlist_id", -1)
        if (playlistId == -1) {
            finish()
            return
        }

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

        viewModel.loadPlaylistDetails(playlistId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Playlist"
    }

    private fun setupRecyclerView() {
        adapter = PlaylistSongsAdapter(
            onSongClick = { song ->
                viewModel.playSongFromPlaylist(song)
                val intent = NowPlayingActivity.newIntent(this, song.id)
                startActivity(intent)
            },
            onRemoveClick = { song ->
                showRemoveSongDialog(song)
            }
        )
        binding.rvPlaylistSongs.apply {
            layoutManager = LinearLayoutManager(this@PlaylistDetailActivity)
            adapter = this@PlaylistDetailActivity.adapter
        }
    }

    private fun setupListeners() {
        binding.btnAddSong.setOnClickListener {
            showAddSongDialog()
        }

        binding.btnPlayAll.setOnClickListener {
            viewModel.playPlaylist()
            viewModel.songs.value.firstOrNull()?.let { song ->
                val intent = NowPlayingActivity.newIntent(this, song.id)
                startActivity(intent)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.songs.collect { songs ->
                adapter.submitList(songs)
                binding.tvEmptySongs.visibility = if (songs.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                binding.tvSongCount.text = "${songs.size} canciones"
            }
        }

        lifecycleScope.launch {
            viewModel.playlist.collect { playlist ->
                playlist?.let {
                    supportActionBar?.title = it.name
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

    private fun showAddSongDialog() {
        val songsNotInPlaylist = viewModel.getSongsNotInPlaylist()
        if (songsNotInPlaylist.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Agregar Canción")
                .setMessage("Todas las canciones ya están en esta playlist")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val songNames = songsNotInPlaylist.map { "${it.title} - ${it.artist}" }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Canción")
            .setItems(songNames) { _, which ->
                val song = songsNotInPlaylist[which]
                viewModel.addSongToPlaylist(song)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRemoveSongDialog(song: Song) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Canción")
            .setMessage("¿Eliminar \"${song.title}\" de esta playlist?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.removeSongFromPlaylist(song)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        fun newIntent(context: Context, playlistId: Int): Intent {
            return Intent(context, PlaylistDetailActivity::class.java).apply {
                putExtra("playlist_id", playlistId)
            }
        }
    }
}