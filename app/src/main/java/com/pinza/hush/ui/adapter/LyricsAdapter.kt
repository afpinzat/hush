package com.pinza.hush.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.R
import com.pinza.hush.utils.LrcLine

class LyricsAdapter : ListAdapter<LrcLine, LyricsAdapter.ViewHolder>(DiffCallback()) {

    private var activeLineIndex: Int = -1

    fun setActiveLine(index: Int) {
        if (activeLineIndex != index) {
            val oldIndex = activeLineIndex
            activeLineIndex = index
            notifyItemChanged(oldIndex)
            notifyItemChanged(activeLineIndex)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == activeLineIndex)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvLine: TextView = view.findViewById(R.id.tv_lyric_line)

        fun bind(line: LrcLine, isActive: Boolean) {
            val context = itemView.context
            tvLine.text = line.text
            if (isActive) {
                tvLine.setTextColor(ContextCompat.getColor(context, R.color.primary))
                tvLine.alpha = 1.0f
                tvLine.setTypeface(null, Typeface.BOLD)
                tvLine.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()
            } else {
                tvLine.setTextColor(ContextCompat.getColor(context, R.color.on_background_variant))
                tvLine.alpha = 0.4f
                tvLine.setTypeface(null, Typeface.NORMAL)
                tvLine.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LrcLine>() {
        override fun areItemsTheSame(oldItem: LrcLine, newItem: LrcLine): Boolean =
            oldItem.time == newItem.time

        override fun areContentsTheSame(oldItem: LrcLine, newItem: LrcLine): Boolean =
            oldItem == newItem
    }
}
