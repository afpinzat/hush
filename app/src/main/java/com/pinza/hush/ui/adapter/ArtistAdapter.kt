package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.pinza.hush.R
import com.pinza.hush.data.local.dao.SongDao
import com.pinza.hush.databinding.ItemPlaylistBinding

class ArtistAdapter(private val onArtistClick: (SongDao.ArtistSummary) -> Unit) :
    ListAdapter<SongDao.ArtistSummary, ArtistAdapter.ArtistViewHolder>(ArtistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArtistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(getItem(position), onArtistClick)
    }

    class ArtistViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(artist: SongDao.ArtistSummary, onClick: (SongDao.ArtistSummary) -> Unit) {
            binding.tvPlaylistName.text = artist.artist
            binding.tvSongCount.text = "Artista"
            binding.ivPlaylistArt.load(artist.albumArt) {
                crossfade(true)
                placeholder(R.drawable.ic_album_24)
                error(R.drawable.ic_album_24)
            }
            binding.root.setOnClickListener { onClick(artist) }
        }
    }
}

class ArtistDiffCallback : DiffUtil.ItemCallback<SongDao.ArtistSummary>() {
    override fun areItemsTheSame(old: SongDao.ArtistSummary, new: SongDao.ArtistSummary) = old.artist == new.artist
    override fun areContentsTheSame(old: SongDao.ArtistSummary, new: SongDao.ArtistSummary) = old == new
}
