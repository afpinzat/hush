package com.pinza.hush.ui.main

import android.content.ComponentName
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.NavHostFragment
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.pinza.hush.R
import com.pinza.hush.databinding.ActivityMainBinding
import com.pinza.hush.service.PlaybackService
import com.pinza.hush.ui.nowplaying.NowPlayingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val nowPlayingViewModel: NowPlayingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Manejar insets para que el miniplayer no se solape con la barra de navegación (gestos)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }

        setupNavigation()
        observePlayback()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateMiniPlayerVisibility()
        }
    }

    private fun observePlayback() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nowPlayingViewModel.uiState.collect { state ->
                    updateMiniPlayerVisibility()
                }
            }
        }
    }

    private fun updateMiniPlayerVisibility() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentDestination = navHostFragment.navController.currentDestination?.id
        
        val hasSong = nowPlayingViewModel.uiState.value.currentSong != null
        val isNowPlayingFragment = currentDestination == R.id.nowPlayingFragment

        if (hasSong && !isNowPlayingFragment) {
            binding.miniplayerContainer.visibility = View.VISIBLE
        } else {
            binding.miniplayerContainer.visibility = View.GONE
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            // Aquí obtienes el controller para enviar comandos al servicio
            val controller = controllerFuture?.get()
            if (controller != null) {
                nowPlayingViewModel.setMediaController(controller)
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        MediaController.releaseFuture(controllerFuture!!)
    }
}
