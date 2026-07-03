package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.databinding.ItemAlbumBinding
import android.view.View

data class AlbumItem(
    val name: String,
    val artist: String,
    val songCount: Int,
    val year: Int? = null,
    val imageUrl: String? = null
)

class AlbumAdapter(
    private val onAlbumClick: (AlbumItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<AlbumItem, AlbumAdapter.AlbumViewHolder>(AlbumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = getItem(position)
        holder.bind(album)
    }

    inner class AlbumViewHolder(
        private val binding: ItemAlbumBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: AlbumItem) {
            binding.apply {
                tvAlbumName.text = album.name
                tvArtistName.text = album.artist
                tvSongCount.text = "${album.songCount} canciones"
                album.year?.let {
                    tvYear.text = it.toString()
                    tvYear.visibility = View.VISIBLE
                } ?: run {
                    tvYear.visibility = View.GONE
                }

                root.setOnClickListener {
                    onAlbumClick(album)
                }
            }
        }
    }

    class AlbumDiffCallback : DiffUtil.ItemCallback<AlbumItem>() {
        override fun areItemsTheSame(oldItem: AlbumItem, newItem: AlbumItem): Boolean {
            return oldItem.name == newItem.name && oldItem.artist == newItem.artist
        }

        override fun areContentsTheSame(oldItem: AlbumItem, newItem: AlbumItem): Boolean {
            return oldItem == newItem
        }
    }
}