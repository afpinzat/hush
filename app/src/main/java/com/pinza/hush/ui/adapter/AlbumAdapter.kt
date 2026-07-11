package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pinza.hush.R
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.databinding.ItemPlaylistBinding // Reusing ItemPlaylistBinding as it has image and text

class AlbumAdapter(private val onAlbumClick: (SongDao.AlbumSummary) -> Unit) :
    ListAdapter<SongDao.AlbumSummary, AlbumAdapter.AlbumViewHolder>(AlbumDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position), onAlbumClick)
    }

    class AlbumViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: SongDao.AlbumSummary, onClick: (SongDao.AlbumSummary) -> Unit) {
            binding.tvPlaylistName.text = album.album
            binding.tvSongCount.text = "Álbum"
            binding.ivPlaylistArt.load(album.albumArt) {
                crossfade(true)
                placeholder(R.drawable.ic_album_24)
                error(R.drawable.ic_album_24)
            }
            binding.root.setOnClickListener { onClick(album) }
        }
    }
}

class AlbumDiffCallback : DiffUtil.ItemCallback<SongDao.AlbumSummary>() {
    override fun areItemsTheSame(old: SongDao.AlbumSummary, new: SongDao.AlbumSummary) = old.album == new.album
    override fun areContentsTheSame(old: SongDao.AlbumSummary, new: SongDao.AlbumSummary) = old == new
}
