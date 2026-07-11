package com.pinza.hush.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ItemSongBinding
import java.util.Locale

class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onMoreOptionsClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    private var currentPlayingId: Long = -1

    fun setCurrentPlayingId(id: Long) {
        currentPlayingId = id
        notifyDataSetChanged() 
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

    class SongViewHolder(
        private val binding: ItemSongBinding,
        private val onSongClick: (Song) -> Unit,
        private val onMoreOptionsClick: (Song) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val colors = listOf(
            "#F4A8C0", "#80CBC4", "#CE93D8", "#FFB74D", "#B39DDB", "#A5D6A7", "#80DEEA"
        )

        fun bind(song: Song, isPlaying: Boolean) {
            binding.apply {
                textTitle.text = song.title
                textArtist.text = song.artist
                textDuration.text = formatTime(song.duration.toLong())

                // Fondo resaltado si está sonando (como en la imagen)
                root.setBackgroundColor(
                    if (isPlaying) Color.parseColor("#1A6750A4") 
                    else Color.TRANSPARENT
                )

                // Color dinámico para el icono (como en la imagen)
                val colorIndex = (song.id % colors.size).toInt()
                val color = Color.parseColor(colors[colorIndex])
                cardSong.setCardBackgroundColor(color.adjustAlpha(0.2f))
                imageSong.imageTintList = ColorStateList.valueOf(color)

                root.setOnClickListener { onSongClick(song) }
                buttonMore.setOnClickListener { onMoreOptionsClick(song) }
            }
        }

        private fun Int.adjustAlpha(factor: Float): Int {
            val alpha = Math.round(Color.alpha(this) * factor)
            val red = Color.red(this)
            val green = Color.green(this)
            val blue = Color.blue(this)
            return Color.argb(alpha, red, green, blue)
        }

        private fun formatTime(millis: Long): String {
            val totalSeconds = millis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem == newItem
    }
}
