package com.pinza.hush.ui.nowplaying

import com.pinza.hush.ui.adapter.LyricsAdapter
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.Player
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import coil.load
import androidx.recyclerview.widget.LinearSmoothScroller
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.FragmentNowPlayingBinding
import com.pinza.hush.ui.library.LibraryViewModel
import com.pinza.hush.ui.playlist.AddToPlaylistDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class NowPlayingFragment : Fragment(R.layout.fragment_now_playing) {

    private val viewModel: NowPlayingViewModel by activityViewModels()
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private lateinit var lyricsAdapter: LyricsAdapter

    private var isUserSeeking = false
    private var lastLyricIndex = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNowPlayingBinding.bind(view)

        setupInsets()
        setupUI()
        setupListeners()
        observeUiState()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        lyricsAdapter = LyricsAdapter()
        binding.rvLyrics.apply {
            adapter = lyricsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()

        binding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }

        binding.btnNext.setOnClickListener {
            viewModel.skipToNext()
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.skipToPrevious()
        }

        binding.btnRepeat.setOnClickListener {
            viewModel.toggleRepeatMode()
        }

        binding.btnQueue.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.queueFragment) {
                findNavController().popBackStack()
            } else {
                findNavController().navigate(R.id.queueFragment, null, navOptions)
            }
        }

        binding.btnAddToPlaylist.setOnClickListener {
            val songId = viewModel.uiState.value.currentSong?.id ?: return@setOnClickListener
            AddToPlaylistDialog.newInstance(songId).show(childFragmentManager, "AddToPlaylistDialog")
        }
        
        binding.btnFavorite.setOnClickListener {
            val song = viewModel.uiState.value.currentSong ?: return@setOnClickListener
            libraryViewModel.updateSong(song.copy(isFavorite = !song.isFavorite))
        }
        
        binding.btnMoreOptions.setOnClickListener {
            viewModel.uiState.value.currentSong?.let { showSongOptions(it) }
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { 
                isUserSeeking = true 
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val newPosition = seekBar?.progress?.toLong() ?: 0L
                viewModel.seekTo(newPosition)
                binding.tvCurrentTime.text = formatTime(newPosition)
                
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    isUserSeeking = false
                }
            }
        })
    }

    private fun showSongOptions(song: Song) {
        val options = arrayOf("Editar canción", "Editar letras", "Borrar canción")
        AlertDialog.Builder(requireContext())
            .setTitle(song.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditSongDialog(song)
                    1 -> showEditLyricsDialog(song)
                    2 -> showDeleteConfirmDialog(song)
                }
            }
            .show()
    }

    private fun showEditSongDialog(song: Song) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_song, null)
        val etTitle = view.findViewById<EditText>(R.id.et_title)
        val etArtist = view.findViewById<EditText>(R.id.et_artist)
        
        etTitle.setText(song.title)
        etArtist.setText(song.artist)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar canción")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val newTitle = etTitle.text.toString()
                val newArtist = etArtist.text.toString()
                if (newTitle.isNotBlank() && newArtist.isNotBlank()) {
                    libraryViewModel.updateSong(song.copy(title = newTitle, artist = newArtist))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditLyricsDialog(song: Song) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_lyrics, null)
        val etLyrics = view.findViewById<EditText>(R.id.et_lyrics)
        
        lifecycleScope.launch {
            val currentLyrics = libraryViewModel.getLyrics(song.id)
            etLyrics.setText(currentLyrics)
            
            AlertDialog.Builder(requireContext())
                .setTitle("Editar letras (LRC)")
                .setView(view)
                .setPositiveButton("Guardar") { _, _ ->
                    libraryViewModel.saveLyrics(song.id, etLyrics.text.toString())
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showDeleteConfirmDialog(song: Song) {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar canción")
            .setMessage("¿Estás seguro de que deseas borrar '${song.title}'?")
            .setPositiveButton("Borrar") { _, _ ->
                libraryViewModel.deleteSong(song)
                findNavController().popBackStack() // Volver tras borrar si es la actual
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvSongTitle.text = state.currentSong?.title
                    binding.tvArtist.text = state.currentSong?.artist

                    binding.ivArtwork.load(state.currentSong?.albumArt) {
                        crossfade(true)
                        placeholder(R.drawable.ic_music_note_24)
                        error(R.drawable.ic_music_note_24)
                    }

                    if (!isUserSeeking) {
                        binding.seekBar.max = state.duration.toInt()
                        binding.seekBar.progress = state.progress.toInt()
                        binding.tvCurrentTime.text = formatTime(state.progress)
                        binding.tvTotalTime.text = formatTime(state.duration)
                    }

                    val iconRes = if (state.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24
                    binding.btnPlayPause.setIconResource(iconRes)

                    val repeatIcon = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_24
                        else -> R.drawable.ic_repeat_24
                    }
                    binding.btnRepeat.setImageResource(repeatIcon)
                    binding.btnRepeat.imageAlpha = if (state.repeatMode == Player.REPEAT_MODE_OFF) 128 else 255
                    
                    val favIcon = if (state.currentSong?.isFavorite == true) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24
                    binding.btnFavorite.setImageResource(favIcon)

                    // Actualizar letras
                    if (state.parsedLyrics.isNotEmpty()) {
                        binding.rvLyrics.isVisible = true
                        binding.tvNoLyrics.isVisible = false
                        lyricsAdapter.submitList(state.parsedLyrics)
                        if (state.currentLineIndex != lastLyricIndex) {
                            lyricsAdapter.setActiveLine(state.currentLineIndex)
                            if (state.currentLineIndex != -1) {
                                smoothScrollToCenter(state.currentLineIndex)
                            }
                            lastLyricIndex = state.currentLineIndex
                        }
                    } else {
                        binding.rvLyrics.isVisible = false
                        binding.tvNoLyrics.isVisible = true
                    }
                }
            }
        }
    }

    private fun smoothScrollToCenter(position: Int) {
        val scroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
            }
        }
        scroller.targetPosition = position
        binding.rvLyrics.layoutManager?.startSmoothScroll(scroller)
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
