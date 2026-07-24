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
import android.graphics.Bitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.ViewSizeResolver
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.palette.graphics.Palette
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.data.local.model.SongLyrics
import com.pinza.hush.databinding.FragmentNowPlayingBinding
import com.pinza.hush.ui.library.LibraryViewModel
import com.pinza.hush.ui.playlist.AddToPlaylistDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import coil.request.SuccessResult

@AndroidEntryPoint
class NowPlayingFragment : Fragment(R.layout.fragment_now_playing) {

    private val viewModel: NowPlayingViewModel by activityViewModels()
    private val libraryViewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    private lateinit var lyricsAdapter: LyricsAdapter

    private var isUserSeeking = false
    private var isUserScrollingLyrics = false
    private var lastLyricIndex = -1
    private var currentArtUri: String? = null
    private var currentImageRequest: coil.request.Disposable? = null
    private var currentBackgroundColor: Int = Color.BLACK
    private var backgroundAnimator: ValueAnimator? = null
    private var smokeAnimator: ValueAnimator? = null

    // --- Optimización: debounce de carga de artwork ante skips rápidos ---
    private var artLoadJob: kotlinx.coroutines.Job? = null
    private var hasLoadedFirstArt = false
    private var isFirstUiObservation = true

    // --- Optimización: solo una extracción de Palette en curso a la vez ---
    private var paletteTask: android.os.AsyncTask<Bitmap, Void, Palette>? = null

    // --- Optimización: reutilizar el drawable en vez de crear uno por frame ---
    private var backgroundGradient: GradientDrawable? = null
    private var staticColorsApplied = false

    // --- Optimización: reducir cálculo trigonométrico a la mitad de los frames ---
    private var smokeFrameCounter = 0
    private var lastPushX = 0f
    private var lastPushY = 0f

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
            itemAnimator = null // Prevent flicker during active line change

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        isUserScrollingLyrics = true
                        binding.btnResumeScroll.isVisible = true
                    }
                }
            })
        }

        binding.btnResumeScroll.setOnClickListener {
            isUserScrollingLyrics = false
            binding.btnResumeScroll.isVisible = false
            if (lastLyricIndex != -1) {
                smoothScrollToCenter(lastLyricIndex)
            }
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
        val etLyrics2 = view.findViewById<EditText>(R.id.et_lyrics2)
        val switchPrimary = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_primary_second)
        val btnClear1 = view.findViewById<View>(R.id.btn_clear_lyrics1)
        val btnClear2 = view.findViewById<View>(R.id.btn_clear_lyrics2)

        btnClear1.setOnClickListener { etLyrics.setText("") }
        btnClear2.setOnClickListener { etLyrics2.setText("") }

        lifecycleScope.launch {
            val songLyrics = libraryViewModel.getSongLyrics(song.id)
            etLyrics.setText(songLyrics?.lyrics ?: "")
            etLyrics2.setText(songLyrics?.lyrics2 ?: "")
            switchPrimary.isChecked = songLyrics?.isPrimarySecond ?: false

            AlertDialog.Builder(requireContext())
                .setTitle("Editar letras (LRC)")
                .setView(view)
                .setPositiveButton("Guardar") { _, _ ->
                    val newLyrics = SongLyrics(
                        songId = song.id,
                        lyrics = etLyrics.text.toString(),
                        lyrics2 = etLyrics2.text.toString(),
                        isPrimarySecond = switchPrimary.isChecked
                    )
                    libraryViewModel.saveSongLyrics(newLyrics)
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
                    if (binding.tvSongTitle.text != state.currentSong?.title) {
                        binding.tvSongTitle.text = state.currentSong?.title
                    }
                    if (binding.tvArtist.text != state.currentSong?.artist) {
                        binding.tvArtist.text = state.currentSong?.artist
                    }

                    if (currentArtUri != state.currentSong?.albumArt || isFirstUiObservation) {
                        isFirstUiObservation = false
                        currentArtUri = state.currentSong?.albumArt
                        val albumArtToLoad = state.currentSong?.albumArt

                        // Optimización: debounce solo si ya se cargó la primera vez
                        artLoadJob?.cancel()
                        artLoadJob = viewLifecycleOwner.lifecycleScope.launch {
                            if (hasLoadedFirstArt) {
                                delay(150)
                            }
                            loadArtwork(albumArtToLoad)
                        }
                    }

                    if (!isUserSeeking) {
                        if (binding.seekBar.max != state.duration.toInt()) {
                            binding.seekBar.max = state.duration.toInt()
                        }
                        binding.seekBar.progress = state.progress.toInt()
                        binding.tvCurrentTime.text = formatTime(state.progress)
                        if (binding.tvTotalTime.text != formatTime(state.duration)) {
                            binding.tvTotalTime.text = formatTime(state.duration)
                        }
                    }

                    val iconRes = if (state.isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24
                    // Check if icon actually needs to change to prevent internal button flicker
                    if (binding.btnPlayPause.tag != iconRes) {
                        binding.btnPlayPause.setIconResource(iconRes)
                        binding.btnPlayPause.tag = iconRes
                    }

                    val repeatIcon = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_24
                        else -> R.drawable.ic_repeat_24
                    }
                    if (binding.btnRepeat.tag != repeatIcon) {
                        binding.btnRepeat.setImageResource(repeatIcon)
                        binding.btnRepeat.tag = repeatIcon
                    }

                    val alpha = if (state.repeatMode == Player.REPEAT_MODE_OFF) 128 else 255
                    if (binding.btnRepeat.imageAlpha != alpha) {
                        binding.btnRepeat.imageAlpha = alpha
                    }

                    val isFav = state.currentSong?.isFavorite == true
                    val favIcon = if (isFav) R.drawable.ic_favorite_24 else R.drawable.ic_favorite_border_24
                    if (binding.btnFavorite.tag != favIcon) {
                        binding.btnFavorite.setImageResource(favIcon)
                        binding.btnFavorite.tag = favIcon
                    }

                    // Actualizar letras
                    if (state.parsedLyrics.isNotEmpty()) {
                        if (!binding.rvLyrics.isVisible) {
                            binding.rvLyrics.isVisible = true
                            binding.tvNoLyrics.isVisible = false
                        }
                        lyricsAdapter.setTranslationEnabled(state.showTranslation)
                        lyricsAdapter.submitList(state.parsedLyrics)
                        if (state.currentLineIndex != lastLyricIndex) {
                            lyricsAdapter.setActiveLine(state.currentLineIndex)
                            if (state.currentLineIndex != -1 && !isUserScrollingLyrics) {
                                smoothScrollToCenter(state.currentLineIndex)
                            }
                            lastLyricIndex = state.currentLineIndex
                        }
                    } else {
                        if (binding.rvLyrics.isVisible) {
                            binding.rvLyrics.isVisible = false
                            binding.tvNoLyrics.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun loadArtwork(albumArt: String?) {
        if (_binding == null) return

        val isFirstLoad = !hasLoadedFirstArt
        hasLoadedFirstArt = true

        // Cancel previous request
        currentImageRequest?.dispose()

        val request = ImageRequest.Builder(requireContext())
            .data(albumArt)
            .target(binding.ivArtwork)
            .placeholder(R.drawable.ic_music_note_24)
            .error(R.drawable.ic_music_note_24)
            .size(ViewSizeResolver(binding.ivArtwork))
            .precision(Precision.EXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowHardware(false) // Hardware bitmaps don't work with Palette
            .crossfade(!isFirstLoad) // Crossfade only on song change
            .listener(onSuccess = { _, result ->
                extractPalette(result)
            })
            .build()

        currentImageRequest = requireContext().imageLoader.enqueue(request)
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

    private fun extractPalette(result: SuccessResult) {
        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return
        if (bitmap.isRecycled) return

        val paletteBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }

        // Optimización: cancelar cualquier extracción de paleta anterior que siga en
        // curso. Sin esto, un skip rápido podía dejar varios cálculos de Palette
        // corriendo en paralelo (cada uno reteniendo su propio bitmap en RAM).
        paletteTask?.cancel(true)
        paletteTask = Palette.from(paletteBitmap).generate { palette ->
            if (_binding == null) return@generate
            val vibrantColor = palette?.getVibrantColor(Color.DKGRAY) ?: Color.DKGRAY
            val darkVibrantColor = palette?.getDarkVibrantColor(Color.BLACK) ?: Color.BLACK
            animateBackgroundColor(vibrantColor, darkVibrantColor)
        }
    }

    private fun animateBackgroundColor(targetColor: Int, darkVibrantColor: Int) {
        val colorFrom = currentBackgroundColor
        val colorTo = targetColor

        // Cancel previous color animator if running
        backgroundAnimator?.cancel()

        // Aplicamos el tema oscuro estático (ahora negro)
        applyStaticThemeColorsOnce()

        val gradient = backgroundGradient ?: GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = 1200f
            setGradientCenter(0.5f, 0.5f)
        }.also {
            backgroundGradient = it
            binding.viewBackground.background = it
        }

        backgroundAnimator = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo).apply {
            duration = 1000
            addUpdateListener { animator ->
                if (_binding == null) return@addUpdateListener
                val color = animator.animatedValue as Int

                // Gradiente que desvanece a NEGRO
                gradient.colors = intArrayOf(
                    color,
                    darkVibrantColor,
                    Color.BLACK
                )
            }
            currentBackgroundColor = colorTo
            start()
        }

        // Start infinite "smoke" movement if not already moving
        startSmokeAnimation()
    }

    /** Aplica una sola vez los colores/tints que son constantes en toda la sesión. */
    private fun applyStaticThemeColorsOnce() {
        if (staticColorsApplied) return
        staticColorsApplied = true

        binding.root.setBackgroundColor(Color.BLACK)

        // Glass Effect (Low Alpha White) para la card de letras sobre fondo negro
        val glassColor = Color.argb(40, 255, 255, 255)
        binding.cardLyrics.setCardBackgroundColor(glassColor)
        lyricsAdapter.setActiveTextColor(Color.WHITE)

        val whiteTint = ColorStateList.valueOf(Color.WHITE)

        binding.btnPlayPause.backgroundTintList = whiteTint
        binding.btnPlayPause.iconTint = ColorStateList.valueOf(Color.BLACK)

        binding.seekBar.progressTintList = whiteTint
        binding.seekBar.thumbTintList = whiteTint
        binding.seekBar.progressBackgroundTintList =
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(Color.WHITE, 100))

        binding.btnNext.imageTintList = whiteTint
        binding.btnPrevious.imageTintList = whiteTint
        binding.btnRepeat.imageTintList = whiteTint
        binding.btnQueue.imageTintList = whiteTint
        binding.btnFavorite.imageTintList = whiteTint
        binding.btnAddToPlaylist.imageTintList = whiteTint
        binding.btnBack.imageTintList = whiteTint
        binding.btnMoreOptions.imageTintList = whiteTint

        binding.tvSongTitle.setTextColor(Color.WHITE)
        binding.tvArtist.setTextColor(ColorUtils.setAlphaComponent(Color.WHITE, 200))
        binding.tvCurrentTime.setTextColor(Color.WHITE)
        binding.tvTotalTime.setTextColor(Color.WHITE)
    }

    private fun startSmokeAnimation() {
        if (smokeAnimator != null) return

        smokeAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 12000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { animator ->
                if (_binding == null) return@addUpdateListener
                val t = animator.animatedValue as Float

                // La rotación es una asignación directa (sin trig), se actualiza siempre
                binding.viewBackground.rotation = t

                // --- Optimización: el cálculo trigonométrico (lo más costoso del frame)
                // solo se recalcula cada 2 frames; el movimiento es lento así que no
                // se nota, y esto reduce ~50% el trabajo de CPU de esta animación ---
                smokeFrameCounter++
                if (smokeFrameCounter % 2 == 0) {
                    val rad = t * Math.PI / 180

                    val wave1X = kotlin.math.sin(rad).toFloat()
                    val wave2X = kotlin.math.sin(rad * 2.3f + 1.1f).toFloat() * 0.4f
                    val wave3X = kotlin.math.sin(rad * 0.6f + 2.4f).toFloat() * 0.25f
                    lastPushX = wave1X + wave2X + wave3X

                    val wave1Y = kotlin.math.cos(rad).toFloat()
                    val wave2Y = kotlin.math.cos(rad * 1.7f + 0.6f).toFloat() * 0.35f
                    val wave3Y = kotlin.math.cos(rad * 3.1f + 1.8f).toFloat() * 0.2f
                    lastPushY = wave1Y + wave2Y + wave3Y
                }

                val pushX = lastPushX
                val pushY = lastPushY

                val pushMagnitude = kotlin.math.abs(pushX)
                val stretch = 1f + (pushMagnitude * 0.15f)
                val squeeze = 1f - (pushMagnitude * 0.08f)

                binding.viewBackground.scaleX = (4.5f * stretch) + (pushX * 0.3f)
                binding.viewBackground.scaleY = (3.5f * squeeze) + (pushY * 0.2f)
                binding.viewBackground.alpha = 0.5f + (pushMagnitude * 0.1f).coerceAtMost(0.2f) // Más transparente en negro

                val screenHeight = binding.root.height.toFloat()
                val targetY = screenHeight * 0.55f

                binding.viewBackground.translationX = pushX * 150f
                binding.viewBackground.translationY = targetY + (pushY * 100f)
            }
            start()
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        // Optimización: pausar animaciones infinitas cuando el fragment no está visible
        // (evita seguir gastando CPU/batería en background)
        backgroundAnimator?.pause()
        smokeAnimator?.pause()
    }

    override fun onResume() {
        super.onResume()
        backgroundAnimator?.resume()
        smokeAnimator?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        artLoadJob?.cancel()
        paletteTask?.cancel(true)
        currentImageRequest?.dispose()
        backgroundAnimator?.cancel()
        smokeAnimator?.cancel()
        backgroundAnimator = null
        smokeAnimator = null
        backgroundGradient = null
        staticColorsApplied = false
        hasLoadedFirstArt = false
        isFirstUiObservation = true
        _binding = null
    }
}