package com.pinza.hush.ui.nowplaying

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import coil.load
import com.pinza.hush.R
import com.pinza.hush.databinding.FragmentMiniPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MiniplayerFragment : Fragment(R.layout.fragment_mini_player) {

    private val viewModel: NowPlayingViewModel by activityViewModels()
    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()

        // Al hacer clic en el mini reproductor, abrimos el reproductor completo
        binding.llInfo.setOnClickListener {
            findNavController().navigate(R.id.nowPlayingFragment, null, navOptions)
        }
        
        binding.cvMiniArt.setOnClickListener {
            findNavController().navigate(R.id.nowPlayingFragment, null, navOptions)
        }

        binding.btnPlayMini.setOnClickListener {
            viewModel.togglePlayPause()
        }
        
        binding.btnNextMini.setOnClickListener {
            viewModel.skipToNext()
        }
        
        binding.btnPreviousMini.setOnClickListener {
            viewModel.skipToPrevious()
        }
        
        binding.btnQueueMini.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.queueFragment) {
                findNavController().popBackStack()
            } else {
                findNavController().navigate(R.id.queueFragment, null, navOptions)
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvMiniTitle.text = state.currentSong?.title ?: "Sin canción"
                    binding.tvMiniArtist.text = state.currentSong?.artist ?: "Desconocido"
                    
                    binding.ivMiniArt.load(state.currentSong?.albumArt) {
                        crossfade(true)
                        placeholder(R.drawable.ic_music_note_24)
                        error(R.drawable.ic_music_note_24)
                    }

                    val iconRes = if (state.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24
                    binding.btnPlayMini.setImageResource(iconRes)

                    // Actualizar barra de progreso
                    binding.pbMiniProgress.max = state.duration.toInt()
                    binding.pbMiniProgress.progress = state.progress.toInt()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
