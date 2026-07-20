package com.pinza.hush.ui.nowplaying

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
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
                        size(150, 150) // Miniatura muy pequeña para el mini reproductor
                        bitmapConfig(android.graphics.Bitmap.Config.RGB_565) // Ahorro de RAM
                        allowHardware(true)
                    }

                    val iconRes = if (state.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24
                    binding.btnPlayMini.setImageResource(iconRes)

                    // Actualizar barra de progreso con colores que contrasten
                    binding.pbMiniProgress.max = state.duration.toInt()
                    binding.pbMiniProgress.progress = state.progress.toInt()
                    
                    // Asegurar que la barra sea blanca sobre el fondo morado
                    binding.pbMiniProgress.progressTintList = ColorStateList.valueOf(Color.WHITE)
                    binding.pbMiniProgress.progressBackgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(Color.WHITE, 60))

                    // Miniplayer en un morado más suave (Pastel/M3 Lavender)
                    val softPurple = Color.parseColor("#EADDFF") // Color Lavender claro de Material 3
                    binding.miniPlayerContainer.setBackgroundColor(softPurple)
                    
                    // Ajustar colores de texto e iconos para contraste sobre morado CLARO (Negro)
                    val blackState = ColorStateList.valueOf(Color.BLACK)
                    binding.tvMiniTitle.setTextColor(Color.BLACK)
                    binding.tvMiniArtist.setTextColor(ColorUtils.setAlphaComponent(Color.BLACK, 160))
                    
                    binding.btnPlayMini.imageTintList = blackState
                    binding.btnNextMini.imageTintList = blackState
                    binding.btnPreviousMini.imageTintList = blackState
                    binding.btnQueueMini.imageTintList = blackState
                    
                    // La barra de progreso sobre morado claro queda mejor en un morado oscuro o negro
                    binding.pbMiniProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#6750A4"))
                    binding.pbMiniProgress.progressBackgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(Color.BLACK, 20))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
