package com.pinza.hush.ui.adapter

import android.graphics.Color
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
    private var activeTextColor: Int = Color.WHITE
    private var showTranslation: Boolean = false

    fun setActiveLine(index: Int) {
        if (activeLineIndex != index) {
            val oldIndex = activeLineIndex
            activeLineIndex = index
            notifyItemChanged(oldIndex)
            notifyItemChanged(activeLineIndex)
        }
    }

    fun setTranslationEnabled(enabled: Boolean) {
        if (showTranslation != enabled) {
            showTranslation = enabled
            notifyDataSetChanged()
        }
    }

    fun setActiveTextColor(color: Int) {
        if (activeTextColor != color) {
            activeTextColor = color
            notifyDataSetChanged() // Full refresh to update colors
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == activeLineIndex, activeTextColor, showTranslation)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvLine: TextView = view.findViewById(R.id.tv_lyric_line)
        private val tvTranslation: TextView = view.findViewById(R.id.tv_lyric_translation)

        fun bind(line: LrcLine, isActive: Boolean, activeColor: Int, showTranslation: Boolean) {
            tvLine.text = line.text
            
            if (showTranslation && !line.translation.isNullOrBlank()) {
                tvTranslation.text = line.translation
                tvTranslation.visibility = View.VISIBLE
            } else {
                tvTranslation.visibility = View.GONE
            }

            if (isActive) {
                tvLine.setTextColor(activeColor)
                tvLine.alpha = 1.0f
                tvLine.setTypeface(null, Typeface.BOLD)
                tvTranslation.alpha = 0.9f
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).start()
            } else {
                tvLine.setTextColor(if (activeColor == Color.WHITE) Color.WHITE else Color.BLACK)
                tvLine.alpha = 0.5f
                tvLine.setTypeface(null, Typeface.NORMAL)
                tvTranslation.alpha = 0.4f
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
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
