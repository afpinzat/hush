package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ItemSongBinding

class PlaylistSongsAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onRemoveClick: (Song) -> Unit
) : ListAdapter<Song, PlaylistSongsAdapter.PlaylistSongsViewHolder>(
    PlaylistSongsDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistSongsViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistSongsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistSongsViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
    }

    inner class PlaylistSongsViewHolder(
        private val binding: ItemSongBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.apply {
                tvSongTitle.text = song.title
                tvArtist.text = song.artist
                tvDuration.text = formatDuration(song.duration)

                // Cambiar icono del menú a eliminar (X)
                btnMenu.setImageResource(R.drawable.ic_close_24)
                btnMenu.setOnClickListener {
                    onRemoveClick(song)
                }

                root.setOnClickListener {
                    onSongClick(song)
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }

    class PlaylistSongsDiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem == newItem
        }
    }
}