package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onMenuClick: (Song) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    private var currentPlayingSongId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        val isPlaying = song.id == currentPlayingSongId
        holder.bind(song, isPlaying)
    }

    fun setCurrentPlayingSongId(songId: Int?) {
        currentPlayingSongId = songId
        notifyDataSetChanged()
    }

    inner class SongViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, isPlaying: Boolean) {
            binding.apply {
                tvSongTitle.text = song.title
                tvArtist.text = song.artist
                tvDuration.text = formatDuration(song.duration)

                // ✅ CORREGIDO: Usar ContextCompat.getColor() para ambos casos
                val color = if (isPlaying) {
                    ContextCompat.getColor(root.context, R.color.item_active_bg)
                } else {
                    ContextCompat.getColor(root.context, android.R.color.transparent)
                }
                root.setBackgroundColor(color)

                root.setOnClickListener {
                    onSongClick(song)
                }

                btnMenu.setOnClickListener {
                    onMenuClick(song)
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }

    class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}