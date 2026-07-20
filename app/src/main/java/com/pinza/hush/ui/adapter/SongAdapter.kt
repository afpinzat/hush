package com.pinza.hush.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import coil.transform.RoundedCornersTransformation
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ItemSongBinding
import java.util.Locale

class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onMoreOptionsClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    private var currentPlayingId: Long = -1

    fun setCurrentPlayingId(id: Long) {
        if (currentPlayingId == id) return
        val oldId = currentPlayingId
        currentPlayingId = id

        val songs = currentList
        val oldIndex = songs.indexOfFirst { it.id == oldId }
        val newIndex = songs.indexOfFirst { it.id == currentPlayingId }

        // Payload -> evita re-bind completo (evita recargar imagen con Coil)
        if (oldIndex != -1) notifyItemChanged(oldIndex, PAYLOAD_PLAYING_STATE)
        if (newIndex != -1) notifyItemChanged(newIndex, PAYLOAD_PLAYING_STATE)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SongViewHolder(binding, onSongClick, onMoreOptionsClick)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song, song.id == currentPlayingId)
    }

    override fun onBindViewHolder(
        holder: SongViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_PLAYING_STATE)) {
            // Solo actualiza el fondo de "reproduciendo", sin tocar imagen/texto
            val song = getItem(position)
            holder.bindPlayingStateOnly(song.id == currentPlayingId)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class SongViewHolder(
        private val binding: ItemSongBinding,
        private val onSongClick: (Song) -> Unit,
        private val onMoreOptionsClick: (Song) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, isPlaying: Boolean) {
            binding.apply {
                textTitle.text = song.title
                textArtist.text = song.artist
                textDuration.text = formatTime(song.duration.toLong())

                imageSong.load(song.albumArt) {
                    placeholder(R.drawable.ic_music_note_24)
                    error(R.drawable.ic_music_note_24)
                    size(150, 150)
                    precision(Precision.EXACT)
                    crossfade(false)
                    allowHardware(true)
                }

                val colorIndex = (song.id % PALETTE_SIZE).toInt()

                imageSong.imageTintList = if (song.albumArt.isNullOrBlank()) {
                    tintColorStates[colorIndex]
                } else {
                    null
                }

                cardSong.setCardBackgroundColor(cardBackgroundColors[colorIndex])

                root.setBackgroundColor(
                    if (isPlaying) PLAYING_BG_COLOR else Color.TRANSPARENT
                )

                root.setOnClickListener { onSongClick(song) }
                buttonMore.setOnClickListener { onMoreOptionsClick(song) }
            }
        }

        /** Update liviano: solo cambia el fondo de estado "reproduciendo". */
        fun bindPlayingStateOnly(isPlaying: Boolean) {
            binding.root.setBackgroundColor(
                if (isPlaying) PLAYING_BG_COLOR else Color.TRANSPARENT
            )
        }

        private fun formatTime(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

        companion object {
            private const val PALETTE_SIZE = 7

            private val hexColors = arrayOf(
                "#F4A8C0", "#80CBC4", "#CE93D8", "#FFB74D", "#B39DDB", "#A5D6A7", "#80DEEA"
            )

            // Parseados UNA sola vez para todos los ViewHolders (antes: por cada bind)
            private val parsedColors: IntArray = IntArray(hexColors.size) { i ->
                Color.parseColor(hexColors[i])
            }

            private val tintColorStates: Array<ColorStateList> = Array(parsedColors.size) { i ->
                ColorStateList.valueOf(parsedColors[i])
            }

            // adjustAlpha(0.2f) precalculado una sola vez por color
            private val cardBackgroundColors: IntArray = IntArray(parsedColors.size) { i ->
                adjustAlpha(parsedColors[i], 0.2f)
            }

            private val PLAYING_BG_COLOR = Color.parseColor("#1A6750A4") // Volvemos a un morado muy sutil (10% alpha)

            private fun adjustAlpha(color: Int, factor: Float): Int {
                val alpha = Math.round(Color.alpha(color) * factor)
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                return Color.argb(alpha, red, green, blue)
            }
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_PLAYING_STATE = "PAYLOAD_PLAYING_STATE"
    }
}