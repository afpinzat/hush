package com.pinza.hush.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ActivityNowPlayingBinding
import com.pinza.hush.ui.viewmodel.NowPlayingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NowPlayingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNowPlayingBinding
    private val nowPlayingViewModel: NowPlayingViewModel by viewModels()
    private var isLyricsFullScreen = false
    private var scrollJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNowPlayingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isLyricsFullScreen) {
                    closeLyricsFullScreen()
                } else {
                    finish()
                }
            }
        })

        setupListeners()
        observeViewModel()

        val songId = intent.getIntExtra("song_id", -1)
        if (songId != -1) {
            lifecycleScope.launch {
                nowPlayingViewModel.loadSong(songId)
            }
        }

        // ✅ Iniciar actualización de scroll cada 200ms
        startScrollUpdater()
    }


    private fun setupListeners() {
        binding.btnPlayPause.setOnClickListener {
            nowPlayingViewModel.togglePlayPause()
        }

        binding.btnPrevious.setOnClickListener {
            nowPlayingViewModel.previous()
        }

        binding.btnNext.setOnClickListener {
            nowPlayingViewModel.next()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatDuration(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    nowPlayingViewModel.seekTo(it.progress)
                }
            }
        })

        binding.btnFavorite.setOnClickListener {
            nowPlayingViewModel.toggleFavorite()
        }

        binding.btnShuffle.setOnClickListener {
            nowPlayingViewModel.toggleShuffle()
        }

        binding.btnRepeat.setOnClickListener {
            nowPlayingViewModel.toggleRepeat()
        }

        binding.cardLyrics.setOnClickListener {
            val currentLyrics = nowPlayingViewModel.uiState.value.lyrics
            if (!currentLyrics.isNullOrEmpty()) {
                openLyricsFullScreen(currentLyrics)
            }
        }

        binding.btnQueue.setOnClickListener {
            val intent = Intent(this, QueueActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            nowPlayingViewModel.uiState.collect { state ->
                state.currentSong?.let { song ->
                    binding.tvSongTitle.text = song.title
                    binding.tvArtist.text = song.artist
                }

                if (state.duration > 0) {
                    binding.seekBar.max = state.duration
                    binding.tvTotalTime.text = formatDuration(state.duration)
                }
                binding.seekBar.progress = state.currentPosition
                binding.tvCurrentTime.text = formatDuration(state.currentPosition)

                val playIcon = if (state.isPlaying) {
                    R.drawable.ic_pause_24
                } else {
                    R.drawable.ic_play_arrow_24
                }
                binding.btnPlayPause.setIconResource(playIcon)

                val favIcon = if (state.isFavorite) {
                    R.drawable.ic_favorite_24
                } else {
                    R.drawable.ic_favorite_border_24
                }
                binding.btnFavorite.setImageResource(favIcon)

                val shuffleColor = if (state.isShuffleEnabled) {
                    ContextCompat.getColor(this@NowPlayingActivity, R.color.primary)
                } else {
                    ContextCompat.getColor(this@NowPlayingActivity, R.color.on_background_variant)
                }
                binding.btnShuffle.setColorFilter(shuffleColor)

                val repeatColor = if (state.repeatMode != 0) {
                    ContextCompat.getColor(this@NowPlayingActivity, R.color.primary)
                } else {
                    ContextCompat.getColor(this@NowPlayingActivity, R.color.on_background_variant)
                }
                binding.btnRepeat.setColorFilter(repeatColor)

                // ✅ LETRAS
                state.lyrics?.let { lyricsText ->
                    val spannableLyrics = highlightCurrentLine(
                        lyricsText,
                        state.currentPosition,
                        state.duration
                    )
                    binding.tvLyrics.text = spannableLyrics
                    binding.cardLyrics.visibility = View.VISIBLE
                } ?: run {
                    binding.tvLyrics.text = getString(R.string.lyrics_empty)
                    binding.cardLyrics.visibility = View.VISIBLE
                }

                supportActionBar?.title = state.currentSong?.title ?: "Reproduciendo"
            }
        }
    }

    private fun highlightCurrentLine(lyrics: String, currentPosition: Int, duration: Int): SpannableString {
        val lines = lyrics.split("\n")
        if (lines.isEmpty()) return SpannableString(lyrics)

        val timestampPattern = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2}))?\]\s*(.*)""")
        val timestampedLines = mutableListOf<Pair<Int, String>>()

        for (line in lines) {
            val match = timestampPattern.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val totalSeconds = minutes * 60 + seconds
                val text = match.groupValues[4]
                timestampedLines.add(totalSeconds to text)
            }
        }

        if (timestampedLines.isNotEmpty()) {
            var activeIndex = 0
            for (i in timestampedLines.indices) {
                if (timestampedLines[i].first <= currentPosition) {
                    activeIndex = i
                } else {
                    break
                }
            }

            val fullText = timestampedLines.map { (_, text) -> text }.joinToString("\n")
            val spannable = SpannableString(fullText)

            var lineStart = 0
            for (i in 0 until activeIndex) {
                lineStart = fullText.indexOf("\n", lineStart) + 1
                if (lineStart == 0) break
            }

            var lineEnd = fullText.indexOf("\n", lineStart)
            if (lineEnd == -1) lineEnd = fullText.length

            spannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary)),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                RelativeSizeSpan(1.4f),
                lineStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            return spannable
        }

        return SpannableString(lyrics)
    }

    // Reemplazar estos dos métodos completos

    private fun startScrollUpdater() {
        scrollJob?.cancel()
        scrollJob = lifecycleScope.launch {
            while (true) {
                delay(150)  // ✅ 150ms para suavidad
                val state = nowPlayingViewModel.uiState.value
                state.lyrics?.let { lyrics ->
                    if (state.duration > 0) {
                        // ✅ Actualizar texto resaltado
                        val spannableLyrics = highlightCurrentLine(
                            lyrics,
                            state.currentPosition,
                            state.duration
                        )
                        binding.tvLyrics.text = spannableLyrics

                        // ✅ Hacer scroll suave
                        scrollToActiveLine(lyrics, state.currentPosition, state.duration)
                    }
                }
            }
        }
    }

    private var lastActiveIndex = -1

    private fun scrollToActiveLine(lyrics: String, currentPosition: Int, duration: Int) {
        if (lyrics.isEmpty() || duration == 0) return

        val lines = lyrics.split("\n")
        if (lines.size <= 1) return

        val timestampPattern = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]\s*(.*)""")
        var activeIndex = 0
        var currentLineIndex = 0

        for (line in lines) {
            val match = timestampPattern.find(line)
            if (match != null) {
                val totalSeconds = match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
                if (totalSeconds <= currentPosition) activeIndex = currentLineIndex
                currentLineIndex++
            }
        }

        if (activeIndex == lastActiveIndex) return
        lastActiveIndex = activeIndex

        binding.tvLyrics.post {
            val layout = binding.tvLyrics.layout ?: return@post
            val safeIndex = activeIndex.coerceIn(0, layout.lineCount - 1)
            val lineHeight = binding.tvLyrics.lineHeight

            // La línea activa queda fija 2 líneas por debajo del tope visible:
            // el usuario "lee" 2 líneas antes de que la vista suba.
            val lineTopPx = layout.getLineTop(safeIndex)
            val targetScrollY = (lineTopPx - (2 * lineHeight)).coerceAtLeast(0)

            binding.tvLyrics.scrollTo(0, targetScrollY)
        }
    }

    private fun openLyricsFullScreen(lyrics: String) {
        isLyricsFullScreen = true

        binding.cardArtwork.visibility = View.GONE
        binding.tvSongTitle.visibility = View.GONE
        binding.tvArtist.visibility = View.GONE
        binding.btnFavorite.visibility = View.GONE
        binding.seekBar.visibility = View.GONE
        binding.tvCurrentTime.visibility = View.GONE
        binding.tvTotalTime.visibility = View.GONE
        binding.btnPrevious.visibility = View.GONE
        binding.btnPlayPause.visibility = View.GONE
        binding.btnNext.visibility = View.GONE
        binding.btnShuffle.visibility = View.GONE
        binding.btnRepeat.visibility = View.GONE
        binding.btnQueue.visibility = View.GONE

        binding.cardLyrics.setBackgroundColor(ContextCompat.getColor(this, R.color.background))
        binding.tvLyrics.text = lyrics
        binding.tvLyrics.textSize = 20f
        binding.tvLyrics.setPadding(40, 80, 40, 80)
        binding.tvLyrics.maxHeight = Int.MAX_VALUE

        binding.btnCloseLyrics.visibility = View.VISIBLE
        binding.btnCloseLyrics.setOnClickListener {
            closeLyricsFullScreen()
        }
    }

    private fun closeLyricsFullScreen() {
        isLyricsFullScreen = false

        binding.cardArtwork.visibility = View.VISIBLE
        binding.tvSongTitle.visibility = View.VISIBLE
        binding.tvArtist.visibility = View.VISIBLE
        binding.btnFavorite.visibility = View.VISIBLE
        binding.seekBar.visibility = View.VISIBLE
        binding.tvCurrentTime.visibility = View.VISIBLE
        binding.tvTotalTime.visibility = View.VISIBLE
        binding.btnPrevious.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE
        binding.btnNext.visibility = View.VISIBLE
        binding.btnShuffle.visibility = View.VISIBLE
        binding.btnRepeat.visibility = View.VISIBLE
        binding.btnQueue.visibility = View.VISIBLE

        binding.cardLyrics.setBackgroundColor(ContextCompat.getColor(this, R.color.lyrics_bg))
        binding.tvLyrics.textSize = 14f
        binding.tvLyrics.setPadding(24, 24, 24, 24)
        binding.tvLyrics.maxHeight = 350

        binding.btnCloseLyrics.visibility = View.GONE

        val state = nowPlayingViewModel.uiState.value
        state.lyrics?.let { lyrics ->
            val spannable = highlightCurrentLine(
                lyrics,
                state.currentPosition,
                state.duration
            )
            binding.tvLyrics.text = spannable
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_now_playing, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isLyricsFullScreen) {
                    closeLyricsFullScreen()
                } else {
                    finish()
                }
                true
            }
            R.id.action_edit_song -> {
                showEditSongDialog()
                true
            }
            R.id.action_add_to_playlist -> {
                showAddToPlaylistDialog()
                true
            }
            R.id.action_delete_song -> {
                showDeleteSongDialog()
                true
            }
            R.id.action_edit_lyrics -> {
                showEditLyricsDialog()
                true
            }
            R.id.action_delete_lyrics -> {
                showDeleteLyricsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─── DIÁLOGOS ──────────────────────────────────────────────────────

    private fun showEditSongDialog() {
        val currentSong = nowPlayingViewModel.uiState.value.currentSong ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_song, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_song_title)
        val etArtist = dialogView.findViewById<TextInputEditText>(R.id.et_song_artist)

        etTitle.setText(currentSong.title)
        etArtist.setText(currentSong.artist)

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Canción")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newArtist = etArtist.text.toString().trim()
                if (newTitle.isNotEmpty() && newArtist.isNotEmpty()) {
                    val updatedSong = currentSong.copy(
                        title = newTitle,
                        artist = newArtist
                    )
                    nowPlayingViewModel.updateSong(updatedSong)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddToPlaylistDialog() {
        val currentSong = nowPlayingViewModel.uiState.value.currentSong ?: return
        val playlists = listOf("Favoritos", "Rock", "Pop")

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar a Playlist")
            .setItems(playlists.toTypedArray()) { _, which ->
                val playlistName = playlists[which]
                nowPlayingViewModel.addToPlaylist(currentSong, playlistName)
            }
            .setPositiveButton("Crear nueva") { _, _ ->
                showCreatePlaylistDialog(currentSong)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCreatePlaylistDialog(song: Song) {
        val input = TextInputEditText(this).apply {
            hint = "Nombre de la playlist"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Nueva Playlist")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    nowPlayingViewModel.addToPlaylist(song, name)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteSongDialog() {
        val currentSong = nowPlayingViewModel.uiState.value.currentSong ?: return

        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Canción")
            .setMessage("¿Eliminar \"${currentSong.title}\" de la biblioteca?")
            .setPositiveButton("Eliminar") { _, _ ->
                nowPlayingViewModel.deleteSong(currentSong)
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditLyricsDialog() {
        val currentSong = nowPlayingViewModel.uiState.value.currentSong ?: return
        val currentLyrics = nowPlayingViewModel.uiState.value.lyrics ?: ""

        val input = TextInputEditText(this).apply {
            setText(currentLyrics)
            hint = "Escribe la letra aquí...\nPuedes usar formato:\n[00:00.00] Primera línea\n[00:10.00] Segunda línea"
            minLines = 8
            maxLines = 15
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Editar Letra")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newLyrics = input.text.toString().trim()
                nowPlayingViewModel.saveLyrics(newLyrics)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteLyricsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Letra")
            .setMessage("¿Eliminar la letra de esta canción?")
            .setPositiveButton("Eliminar") { _, _ ->
                nowPlayingViewModel.deleteLyrics()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollJob?.cancel()
    }

    companion object {
        fun newIntent(context: Context, songId: Int): Intent {
            return Intent(context, NowPlayingActivity::class.java).apply {
                putExtra("song_id", songId)
            }
        }
    }
}