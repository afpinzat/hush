package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.databinding.ItemArtistBinding

data class ArtistItem(
    val name: String,
    val songCount: Int,
    val imageUrl: String? = null
)

class ArtistAdapter(
    private val onArtistClick: (ArtistItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<ArtistItem, ArtistAdapter.ArtistViewHolder>(ArtistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemArtistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val artist = getItem(position)
        holder.bind(artist)
    }

    inner class ArtistViewHolder(
        private val binding: ItemArtistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: ArtistItem) {
            binding.apply {
                tvArtistName.text = artist.name
                tvSongCount.text = "${artist.songCount} canciones"

                root.setOnClickListener {
                    onArtistClick(artist)
                }
            }
        }
    }

    class ArtistDiffCallback : DiffUtil.ItemCallback<ArtistItem>() {
        override fun areItemsTheSame(oldItem: ArtistItem, newItem: ArtistItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ArtistItem, newItem: ArtistItem): Boolean {
            return oldItem == newItem
        }
    }
}