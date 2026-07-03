package com.pinza.hush.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ActivityLibraryBinding
import com.pinza.hush.ui.adapter.SongAdapter
import com.pinza.hush.ui.viewmodel.LibraryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var songAdapter: SongAdapter
    private lateinit var miniPlayerManager: MiniPlayerManager

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val NOTIFICATION_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupRecyclerView()
        setupMiniPlayer()
        setupSearch()
        observeViewModel()
        checkPermissionsAndLoadSongs()
        checkNotificationPermission()  // ✅ Solicitar permiso de notificaciones
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun checkPermissionsAndLoadSongs() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasAllPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            libraryViewModel.loadSongsOrScanIfEmpty()
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                libraryViewModel.loadSongsOrScanIfEmpty()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Se necesitan permisos para leer la música",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                libraryViewModel.loadSongs()
            }
        }

        // ✅ Manejar resultado del permiso de notificaciones
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                android.util.Log.d("LibraryActivity", "✅ Permiso de notificaciones concedido")
            } else {
                android.util.Log.d("LibraryActivity", "❌ Permiso de notificaciones denegado")
            }
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(
            onSongClick = { song ->
                libraryViewModel.playSong(song)
                val intent = NowPlayingActivity.newIntent(this, song.id)
                startActivity(intent)
            },
            onMenuClick = { song ->
                showSongOptionsMenu(song)
            }
        )
        binding.rvSongs.apply {
            layoutManager = LinearLayoutManager(this@LibraryActivity)
            adapter = songAdapter
        }
    }

    private fun setupMiniPlayer() {
        miniPlayerManager = MiniPlayerManager(
            binding.miniPlayer.root,
            libraryViewModel
        )

        miniPlayerManager.setOnClickListener {
            libraryViewModel.currentSongId.value?.let { songId ->
                val intent = NowPlayingActivity.newIntent(this, songId)
                startActivity(intent)
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                libraryViewModel.searchSongs(query)
                true
            } else false
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString()
            libraryViewModel.searchSongs(query)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            libraryViewModel.filteredSongs.collect { songs ->
                songAdapter.submitList(songs)
            }
        }

        lifecycleScope.launch {
            libraryViewModel.isLoading.collect { isLoading ->
                // Mostrar/ocultar loading
            }
        }

        lifecycleScope.launch {
            libraryViewModel.error.collect { error ->
                error?.let {
                    android.widget.Toast.makeText(this@LibraryActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                    libraryViewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            libraryViewModel.currentSongTitle.collect { title ->
                if (title.isNotEmpty()) {
                    miniPlayerManager.updateState(
                        title,
                        libraryViewModel.currentArtist.value,
                        libraryViewModel.isPlaying.value
                    )
                }
            }
        }

        lifecycleScope.launch {
            libraryViewModel.isPlaying.collect { isPlaying ->
                val title = libraryViewModel.currentSongTitle.value
                if (title.isNotEmpty()) {
                    miniPlayerManager.updateState(
                        title,
                        libraryViewModel.currentArtist.value,
                        isPlaying
                    )
                }
            }
        }
    }

    private fun showSongOptionsMenu(song: Song) {
        // TODO: Implementar menú contextual
    }

    override fun onResume() {
        super.onResume()
        // ✅ Solo actualizar UI, NO restaurar desde Room
        libraryViewModel.refreshCurrentSong()
    }
}