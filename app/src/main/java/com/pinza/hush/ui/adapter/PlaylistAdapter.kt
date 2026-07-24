package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Precision
import com.pinza.hush.R
import com.pinza.hush.data.local.model.Playlist
import com.pinza.hush.data.local.model.PlaylistWithSongs
import com.pinza.hush.databinding.ItemPlaylistBinding

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onPlaylistLongClick: (Playlist) -> Unit
) : ListAdapter<PlaylistWithSongs, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position), onPlaylistClick, onPlaylistLongClick)
    }

    class PlaylistViewHolder(private val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlistWithSongs: PlaylistWithSongs, onClick: (Playlist) -> Unit, onLongClick: (Playlist) -> Unit) {
            val playlist = playlistWithSongs.playlist
            binding.tvPlaylistName.text = playlist.name
            binding.tvSongCount.text = "${playlistWithSongs.songs.size} canciones"
            
            // Asignar la imagen de la primera canción de la playlist
            val firstSongArt = playlistWithSongs.songs.firstOrNull()?.albumArt
            binding.ivPlaylistArt.load(firstSongArt) {
                placeholder(R.drawable.ic_playlist)
                error(R.drawable.ic_playlist)
                size(400, 400)
                precision(Precision.EXACT)
                crossfade(false)
                allowHardware(true)
            }

            binding.root.setOnClickListener { onClick(playlist) }
            binding.root.setOnLongClickListener {
                onLongClick(playlist)
                true
            }
        }
    }
}

class PlaylistDiffCallback : DiffUtil.ItemCallback<PlaylistWithSongs>() {
    override fun areItemsTheSame(old: PlaylistWithSongs, new: PlaylistWithSongs) = old.playlist.id == new.playlist.id
    override fun areContentsTheSame(old: PlaylistWithSongs, new: PlaylistWithSongs) = old == new
}