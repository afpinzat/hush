package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.data.local.model.Song
import com.pinza.hush.databinding.ItemQueueBinding
import java.util.Collections

class QueueAdapter(
    private val onSongClick: (Song) -> Unit,
    private val onRemoveClick: (Song) -> Unit
) : ListAdapter<Song, QueueAdapter.QueueViewHolder>(SongAdapter.SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        holder.bind(getItem(position), onSongClick, onRemoveClick)
    }

    // Método para mover elementos (usado por el ItemTouchHelper)
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList)
    }

    class QueueViewHolder(private val binding: ItemQueueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, onClick: (Song) -> Unit, onRemove: (Song) -> Unit) {
            binding.tvSongTitle.text = song.title
            binding.tvArtist.text = song.artist
            binding.root.setOnClickListener { onClick(song) }
            binding.btnRemove.setOnClickListener { onRemove(song) }
        }
    }
}