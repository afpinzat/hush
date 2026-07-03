package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.R
import com.pinza.hush.data.local.model.QueueItem
import com.pinza.hush.databinding.ItemQueueBinding

class QueueAdapter(
    private val onSongClick: (QueueItem) -> Unit,
    private val onRemoveClick: (QueueItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<QueueItem, QueueAdapter.QueueViewHolder>(QueueDiffCallback()) {

    private var currentPlayingQueueId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = ItemQueueBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val item = getItem(position)
        val isPlaying = item.queueId == currentPlayingQueueId
        holder.bind(item, isPlaying)
    }

    fun setCurrentPlayingQueueId(queueId: Int?) {
        currentPlayingQueueId = queueId
        notifyDataSetChanged()
    }

    // ✅ Método público para obtener item por posición
    fun getItemAt(position: Int): QueueItem? {
        return if (position >= 0 && position < itemCount) {
            getItem(position)
        } else {
            null
        }
    }

    inner class QueueViewHolder(
        private val binding: ItemQueueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QueueItem, isPlaying: Boolean) {
            binding.apply {
                tvSongTitle.text = item.song.title
                tvArtist.text = item.song.artist
                tvDuration.text = formatDuration(item.song.duration)

                ivPlayingIndicator.visibility = if (isPlaying) View.VISIBLE else View.GONE

                ivDragHandle.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        onStartDrag(this@QueueViewHolder)
                    }
                    false
                }

                root.setOnClickListener {
                    onSongClick(item)
                }

                btnRemove.setOnClickListener {
                    onRemoveClick(item)
                }
            }
        }

        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }

    class QueueDiffCallback : DiffUtil.ItemCallback<QueueItem>() {
        override fun areItemsTheSame(oldItem: QueueItem, newItem: QueueItem): Boolean {
            return oldItem.queueId == newItem.queueId
        }

        override fun areContentsTheSame(oldItem: QueueItem, newItem: QueueItem): Boolean {
            return oldItem == newItem
        }
    }
}